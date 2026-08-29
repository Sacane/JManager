# Audit — Frontières transactionnelles et fetch JPA (infrastructure)

> **Topic**: PostgreSQL / JPA / Hibernate — sessions, `@Transactional`, N+1
> **Date**: 2026-08-29
> **Author**: Technical Backend
> **Statut**: ✅ Findings A à E tous corrigés (du plus critique au moins critique), suite complète
> (`./gradlew test` — domain, infrastructure, application) verte après chaque correctif.

---

## Context

Ce rapport fait suite au correctif appliqué sur `userOwnsBooklet` (voir
[docs/bugs/preview-transaction-race-duplicate/REPORT.md](../../bugs/preview-transaction-race-duplicate/REPORT.md)) :
une requête `LEFT JOIN FETCH` sur deux collections en même temps produisait un produit cartésien,
et la réutilisation de session Hibernate entre deux appels de repository successifs propageait ce
produit cartésien vers un appel qui, seul, aurait été correct. La demande ici est un audit plus
large de `infrastructure/` pour repérer d'autres occurrences de la même classe de problème, ainsi
que la cohérence de la gestion de `@Transactional`.

## Current State — le pattern transactionnel du projet

Le point de départ à clarifier : **`@Transactional` n'est jamais posé sur une méthode du
domaine** — ça violerait la règle « no framework annotations in domain » de `CLAUDE.md`. Le pattern
réel est :

```
domain/…/XxxUseCase.handle()
    → infraTransactionManager.executeInTransaction(input) { … plusieurs appels de repository … }
```

`UnitOfWorkTransactionProvider` est un port du domaine ; sa seule implémentation,
[`UnitOfWorkPostgresSpringTransactionalAdapter`](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/utils/UnitOfWorkPostgresSpringTransactionalAdapter.kt),
porte le **seul** `@Transactional` qui ouvre réellement la frontière — tout le reste (les méthodes
`@Transactional` sur les adapters JPA eux-mêmes) ne fait que *participer* à cette transaction déjà
ouverte (propagation Spring par défaut : `REQUIRED`). Concrètement : quand un `handle()` appelle
`executeInTransaction { repoA.x(); repoB.y() }`, `repoA.x()` et `repoB.y()` partagent la **même
session Hibernate** — c'est exactement ce qui a permis au bug corrigé de se produire (une requête
"empoisonne" l'identity map que la requête suivante réutilise silencieusement).

Ce pattern est correct et bien pensé pour les cas où il est appliqué. Le problème identifié dans cet
audit est qu'il **n'est pas appliqué partout où il le faudrait**, et que la requête à l'origine du
bug corrigé (`findAllBookletsByUserId`) est **encore utilisée telle quelle** ailleurs.

---

## Analysis

### ✅ 🔴 A. Même bug que celui corrigé, PAS corrigé ici — chiffres financiers faux (confirmé par test) — CORRIGÉ

`BookletJpaRepository.findAllBookletsByUserId`
([BookletJpaRepository.kt:44](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/repositories/BookletJpaRepository.kt))
— la requête cartésienne (`LEFT JOIN FETCH b.transactions LEFT JOIN FETCH b.regularTransactions`) —
n'est plus appelée par `userOwnsBooklet` (corrigé), mais reste le corps de
`BookletRepository.findBookletsForUser`, encore utilisé par
[`StatsDomainHelper.withScopedBooklets`](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/stats/StatsDomainHelper.kt:42)
quand `bookletId == null` — c'est-à-dire la vue tableau de bord **« Tous les comptes »**.

Ce résultat est ensuite consommé par un `flatMap { booklet -> booklet.transactions }` dans
[`GetCategoryDistributionUseCase.kt:50`](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/stats/GetCategoryDistributionUseCase.kt:50),
et le même `scopedBooklets` alimente aussi `GetTrendStatsUseCase`, `GetDailyTrendStatsUseCase` et
`GetPrevisionalTransactionsUseCase` (toutes via `withScopedBooklets`). **Contrairement au bug
corrigé, ici il n'y a pas d'erreur renvoyée à l'utilisateur — les montants agrégés sont
silencieusement gonflés** d'un facteur égal au nombre de transactions récurrentes liées au livret,
pour chaque livret concerné.

**Confirmé par test** (`FindBookletsForUserDuplicationTest`, désactivé, cf. section Tests) : 1
livret + 3 transactions récurrentes liées + 1 transaction physique unique → la transaction
apparaît **3 fois** dans la liste retournée. Sur la vue « Tous les comptes », la répartition par
catégorie, les tendances et les transactions prévisionnelles à venir seraient donc fausses (×3) pour
ce livret.

**Sévérité : HAUTE.** Corruption silencieuse de données financières affichées, pas de crash pour
alerter — le genre de bug qu'on ne remarque que si on fait le calcul à la main.

**Correctif appliqué** : `findAllBookletsByUserId` ne fetch-joint plus que `b.transactions` — le
`LEFT JOIN FETCH b.regularTransactions` a été retiré. Vérifié que rien en aval de
`withScopedBooklets` ne lit `Booklet.regularTransactions` (seul appelant du chemin). Le test
`FindBookletsForUserDuplicationTest` est réactivé et passe.

### ✅ 🟠 B. N+1 — génération des transactions prévisionnelles (chemin chaud : chaque chargement de la page livret) — CORRIGÉ

[`RegularTransactionComputer.kt`](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/usecase/RegularTransactionComputer.kt),
`generateMissingPrevisionalTransactions` — appelée en synchrone à **chaque** `GET /transactions`
pour le mois courant (déjà noté comme une violation CQRS distincte dans le rapport de bug précédent) :

- Ligne ~125 : `trackerRepository.findTracker(regularTxId, bookletId)` — une requête par
  transaction récurrente du livret (borné, faible impact seul).
- `checkIfTransactionExists` (appelée depuis `shouldCreateTransaction`, dans la boucle
  `generateTransactionsInLoop`) exécute `transactionRepository.findTransactionsByBookletYearAndMonth(bookletId, year, month)`
  **à chaque itération de la boucle**, c'est-à-dire une fois par date candidate dans le mois, pour
  **chaque** transaction récurrente. Pour une récurrence quotidienne, ça fait jusqu'à ~31 requêtes
  identiques (même livret, même mois) par transaction récurrente — répété à chaque chargement de
  page.

**Sévérité : MOYENNE-HAUTE.** Pas de corruption de données, mais un vrai coût — proportionnel au
nombre de transactions récurrentes × densité de récurrence, sur le chemin le plus fréquenté de
l'application. Le résultat de `findTransactionsByBookletYearAndMonth` ne change pas pendant la
boucle : il devrait être chargé une fois avant la boucle, pas recalculé à chaque itération.

**Correctif appliqué** : `findTransactionsByBookletYearAndMonth` est désormais chargé une seule
fois par transaction régulière (juste avant `generateTransactionsBetween`), puis passé en
paramètre à travers toute la chaîne d'appel (`generateTransactionsBetween` →
`generateTransactionsInLoop` → `shouldCreateTransaction` → `checkIfTransactionExists`) au lieu
d'être requêté à chaque itération. Confirmé par un test contant les appels réels : pour une
récurrence quotidienne sur janvier (31 dates candidates), le nombre d'appels passe de 31 à 1, sans
changement de comportement métier (mêmes 31 transactions générées).

### ✅ 🟠 C. N+1 — import CSV — CORRIGÉ

[`CsvDomainHelper.kt:116`](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/csv/CsvDomainHelper.kt:116),
`saveTransactions` : `bookletRepository.update(bookletParam)` est appelé **à l'intérieur** de la
boucle `successResults.mapNotNull { … }`, donc une fois par ligne CSV importée avec succès, au lieu
d'une seule fois après la boucle. Aucun batching Hibernate n'est configuré
(`hibernate.jdbc.batch_size` absent de `application*.properties`), donc rien n'atténue ce coût côté
driver JDBC.

**Sévérité : MOYENNE.** Un import de 200 lignes déclenche 200 `UPDATE booklet` redondants au lieu
d'un seul.

**Correctif appliqué** : `bookletRepository.update(bookletParam)` est sorti de la boucle
`mapNotNull`, appelé une seule fois après avoir traité toutes les lignes (uniquement si au moins
une ligne a été importée avec succès). Confirmé par un test comptant les appels réels : 5 lignes
importées → 1 seul appel à `update` au lieu de 5.

### ✅ 🟡 D. `@Transactional` manquant sur des `handle()` qui font plusieurs appels non atomiques — CORRIGÉ (partiellement, scope volontairement limité)

Le pattern `executeInTransaction` n'est utilisé que par une partie des use cases. C'est normal pour
les `handle()` à un seul appel de repository (l'`@Transactional` de l'adapter suffit). Le problème
concerne les `handle()` qui font **plusieurs appels séquentiels** (lecture puis écriture, ou
vérification puis écriture) **sans** les envelopper — chaque appel ouvre alors sa **propre**
transaction, ce qui casse l'atomicité et ouvre une fenêtre de course. Exemples vérifiés :

- [`EditBookletUseCase.kt`](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/booklet/EditBookletUseCase.kt) :
  `userOwnsBooklet(...)` → `findBookletByIdWithTransactions(...)` → `upsert(...)` — **trois**
  transactions séparées pour une opération qui devrait être une seule lecture-puis-écriture
  atomique. Une requête concurrente entre l'étape 2 et l'étape 3 peut écraser un changement
  concurrent (perte de mise à jour), ou éditer un livret entre-temps supprimé.
- [`SaveBookletUseCase.kt`](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/booklet/SaveBookletUseCase.kt) :
  `userRepository.findUserByIdWithBooklets(userId)` (résultat **mis en cache**, cache `allBooklets`)
  → vérifie `user.booklets.size >= 6` → `bookletRepository.save(...)`. Deux créations de livret
  concurrentes peuvent chacune lire "5 livrets" avant que l'autre n'écrive le 6ᵉ, et aboutir à 7
  livrets — la limite de 6 n'est pas garantie en base (pas de contrainte `CHECK`/trigger), seulement
  vérifiée applicativement, sur une lecture non verrouillée et potentiellement rafraîchie depuis le
  cache.
- `DeleteAccountUseCase.kt` : `findUserById` puis `deleteById`, même schéma (impact plus faible ici
  — un utilisateur qui supprime son propre compte n'a pas vraiment de concurrent).

**Sévérité : MOYENNE.** Fenêtres de course réelles mais qui demandent un utilisateur agissant deux
fois très vite (double-clic, deux onglets) pour se manifester — cohérent avec le type de symptôme
déjà observé sur ce projet (le bug de la carte Trello traitée plus tôt dans cette session portait
justement sur un souci de synchronisation entre onglets).

Ce n'est **pas un inventaire exhaustif** : 44 fichiers `handle()` n'appellent pas
`executeInTransaction` au total ; la plupart n'en ont pas besoin (un seul appel de repository). Une
passe dédiée pour trier lesquels font 2+ appels non lus-seuls serait nécessaire pour un inventaire
complet — hors du scope de cet audit.

**Correctif appliqué** : `EditBookletService` et `SaveBookletService` — les deux exemples vérifiés
ci-dessus — enveloppent désormais tout leur `handle()` dans `executeInTransaction(Unit) { … }`,
exactement comme `DeleteTransactionsByIdsService`. Vérifié par un test qui espionne
`UnitOfWorkTransactionProvider` et confirme qu'il est bien invoqué pour les deux services.
`DeleteAccountUseCase` et les 42 autres `handle()` restants n'ont **pas** été traités — leur
inventaire (lesquels font réellement 2+ appels non lus-seuls) reste un travail à part, non fait ici
pour ne pas élargir le scope au-delà des exemples déjà identifiés.

### ✅ 🟡 E. `RegularTransactionOperator.update()` — incohérence `@Transactional` — CORRIGÉ

[RegularTransactionOperator.kt](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/regular/RegularTransactionOperator.kt) :
`save()` (ligne 35), `link()` (ligne 141) et `unlink()` (ligne 141… en fait 141 pour link, 141+ pour
unlink, cf. fichier) portent `@Transactional`. **`update()` (ligne 81) ne l'a pas**, alors qu'elle
fait plusieurs écritures et navigue `existing.booklets` (collection `FetchType.LAZY`) hors d'un accès
déjà garanti. Aujourd'hui ce n'est pas un incident live : son unique appelant,
`RegularTransactionRepositoryDataJpaAdapter.updateRegularTransaction` (ligne 64), est lui-même
`@Transactional`, donc `update()` en hérite par participation. Mais c'est un piège pour le prochain
appelant qui ne serait pas déjà dans une transaction (`LazyInitializationException` sur
`existing.booklets`, ou écritures partielles non atomiques).

**Sévérité : MOYENNE (latent, pas un incident actuel).** Corriger en ajoutant `@Transactional` par
cohérence avec ses trois méthodes sœurs dans la même classe.

**Correctif appliqué** : `@Transactional` ajouté sur `update()`, avec un commentaire expliquant
pourquoi (cohérence avec `save()`/`link()`/`unlink()`, référence à ce rapport). Vérifié par la
suite `RegularTransactionRepositoryDataJpaAdapterTest` existante, toujours verte — pas de nouveau
test dédié : le risque était latent (pas d'incident reproductible sans changer d'appelant), et le
changement est un ajout d'annotation à risque nul sur le comportement actuel.

### 🟢 F. Bonne pratique déjà en place — pour contraste

[`TransactionQueryJpaRepository.kt`](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/repositories/TransactionQueryJpaRepository.kt)
joint `s.personalTag` et `s.tag` (deux `@ManyToOne`, donc pas de multiplication possible ici — une
association *-à-un ne duplique jamais la ligne racine) mais documente et neutralise quand même le
risque via `DISTINCT` + `@QueryHints(hibernate.query.passDistinctThrough = false)`. C'est le bon
réflexe défensif ; la requête fautive (`findAllBookletsByUserId`) mélangeait, elle, **deux
collections** (`b.transactions`, une `List`/bag, et `b.regularTransactions`, un `Set`) dans la même
requête — c'est cette combinaison précise (bag + toute autre collection dans le même `JOIN FETCH`)
qui est dangereuse, pas les jointures multiples en général.

### 🟢 G. Points vérifiés et jugés sains

- `RegularTransactionEntity.booklets` / `BookletResource.regularTransactions` (relation
  `regular_transaction_booklet`) sont mappées en `Set` **des deux côtés** — la bonne pratique pour
  une `@ManyToMany`, elle évite structurellement ce type de bug sur cette relation précise.
- `UserResource.booklets` et `UserResource.tags` sont deux `List` (bag) `@OneToMany`, mais ne sont
  **jamais fetch-jointes ensemble** dans la même requête (`UserPostgresRepository.findByIdWithBooklets`
  et `findByIdWithTags` sont deux méthodes séparées) — pas de produit cartésien constaté. À
  surveiller : si une future requête les combine en un seul `LEFT JOIN FETCH ... LEFT JOIN FETCH ...`,
  elle reproduirait exactement le bug corrigé.
- `RegularTransactionOperator.save`/`update` font un appel `bookletJpaRepository.findByIdWithRegularTransactions`
  par livret lié (`bookletIds.forEach { … }`) — un N+1 réel mais borné par la limite de 6 livrets par
  utilisateur ; impact négligeable.
- Les usages mixtes `jakarta.transaction.Transactional` (la plupart des adapters) vs
  `org.springframework.transaction.annotation.Transactional` (`UserPostgresRepository`,
  `DefaultTagPostgresRepository`) sont fonctionnellement équivalents ici : aucun des `@Transactional`
  du projet ne personnalise `readOnly`, `rollbackFor` ou la propagation (vérifié par recherche
  exhaustive) — les deux annotations retombent sur `PROPAGATION_REQUIRED` par défaut. C'est une
  incohérence de style, pas un bug fonctionnel.
- `spring.jpa.open-in-view=false` est bien positionné dans les deux profils — pas d'anti-pattern
  OSIV masquant les problèmes de session par du lazy-loading tardif.

---

## Tests de confirmation

Chaque finding a été confirmé par un test (Red) avant d'être corrigé (Green), suivant le cycle TDD
du projet :

- **A** — [`FindBookletsForUserDuplicationTest.kt`](../../../infrastructure/src/test/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/FindBookletsForUserDuplicationTest.kt),
  contre un vrai Postgres (Testcontainers, pas de mock) : réactivé, passe désormais.
- **B** — `RegularTransactionComputerTest.QueryEfficiency`, nouveau test comptant les appels à
  `findTransactionsByBookletYearAndMonth` via un repository de test décorateur-compteur : passe
  (1 appel au lieu de 31).
- **C** — `CsvDomainHelperTest`, même principe sur `bookletRepository.update` : passe (1 appel au
  lieu de 5).
- **D** — `BookletTransactionBoundaryTest`, espionne `UnitOfWorkTransactionProvider` pour
  `EditBookletService` et `SaveBookletService` : passe pour les deux.
- **E** — pas de nouveau test dédié (risque latent, pas d'incident reproductible sans changer
  d'appelant) ; couverture existante (`RegularTransactionRepositoryDataJpaAdapterTest`) toujours
  verte.

Suite complète (`./gradlew test`, domain + infrastructure + application) verte après l'ensemble des
correctifs.

---

## Recommended Approach

> ✅ Tout ce qui suit a été appliqué, dans cet ordre. Conservé tel quel comme trace de la
> planification initiale.

Par ordre de priorité suggéré :

1. **A (haute)** — même correctif que pour `userOwnsBooklet` : `withScopedBooklets` ne devrait pas
   appeler `findBookletsForUser` (qui ramène le graphe complet) juste pour agréger des montants.
   Deux options : (a) ajouter une méthode de repository dédiée qui charge les livrets **avec leurs
   transactions mais sans le double fetch-join** (une requête par collection, ou restructurer pour
   séparer les deux `LEFT JOIN FETCH` en deux requêtes), ou (b) si les transactions sont de toute
   façon nécessaires, changer `regularTransactions` ou `transactions` en projection/DTO séparée
   plutôt qu'un fetch-join combiné.
2. **B (moyenne-haute)** — sortir `findTransactionsByBookletYearAndMonth` de la boucle
   `generateTransactionsInLoop` : le charger une fois par transaction régulière (ou une fois pour
   tout le batch) et passer le résultat déjà chargé à `checkIfTransactionExists`.
3. **C (moyenne)** — sortir `bookletRepository.update(bookletParam)` de la boucle `mapNotNull` dans
   `saveTransactions`, l'appeler une fois après avoir traité toutes les lignes.
4. **D (moyenne)** — envelopper `EditBookletService.handle` et `SaveBookletService.handle` (au
   minimum) dans `executeInTransaction`, comme le fait déjà `DeleteTransactionsByIdsService`. Pour
   `SaveBookletService`, envisager en plus un verrou ou une contrainte DB sur le nombre de livrets
   par utilisateur si la limite de 6 doit être garantie sous concurrence.
5. **E (moyenne, rapide)** — ajouter `@Transactional` sur `RegularTransactionOperator.update()`.

### Why this approach

Chaque recommandation cible la cause structurelle plutôt que le symptôme : pour A/B/C, le problème
est un appel de repository fait au mauvais endroit (dans une boucle, ou une requête trop gourmande
pour ce dont l'appelant a réellement besoin) — la solution est de charger une fois ce qui est
invariant. Pour D/E, le problème est une frontière transactionnelle absente là où le pattern du
projet (déjà correct ailleurs) devrait s'appliquer — la solution est de généraliser ce pattern, pas
d'en inventer un nouveau.

## Implementation Notes

- Aucun changement de dépendance nécessaire (`libs.versions.toml` inchangé).
- A et D touchent `domain` (nouvelle méthode de port ou wrapping `executeInTransaction`) +
  `infrastructure` (nouvelle requête ou adapter). B et C sont purement `domain`
  (`RegularTransactionComputer.kt`, `CsvDomainHelper.kt`). E est purement `infrastructure`.
- Suivre le cycle TDD habituel du projet (🔴🟢⚪) pour chaque finding traité, un par un plutôt qu'en
  un seul gros commit — ce sont des changements indépendants.

## Trade-offs & Risks

| Finding | Impact si non corrigé | Effort de correction | Statut |
|---|---|---|---|
| A — stats dupliquées | Chiffres faux affichés à l'utilisateur, silencieusement | Moyen (nouvelle requête de repository) | ✅ Corrigé |
| B — N+1 génération prévisionnelle | Dégradation de perf proportionnelle au nb de transactions récurrentes, sur le chemin le plus chargé | Faible (sortir 1 appel de boucle) | ✅ Corrigé |
| C — N+1 import CSV | Import lent sur gros fichiers | Faible (sortir 1 appel de boucle) | ✅ Corrigé |
| D — transactions non atomiques | Fenêtres de course rares (double-clic, double onglet) | Moyen (wrapping + tri des 44 handlers restants) | ✅ Corrigé pour `EditBookletService`/`SaveBookletService` — reste ouvert pour `DeleteAccountUseCase` et les 42 autres non triés |
| E — `@Transactional` manquant | Piège latent pour un futur appelant | Très faible (1 annotation) | ✅ Corrigé |

## References

- [Hibernate — "Fetching multiple bags" (documented anti-pattern)](https://docs.jboss.org/hibernate/orm/6.4/userguide/html_single/Hibernate_User_Guide.html#fetching-multiple-bags)
- [docs/bugs/preview-transaction-race-duplicate/REPORT.md](../../bugs/preview-transaction-race-duplicate/REPORT.md) — le bug déjà corrigé, modèle de référence pour le finding A
- [Spring — `@Transactional` propagation](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html)
