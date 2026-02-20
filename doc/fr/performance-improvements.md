# Rapport de montée en performance — JManager

> Rédigé le 21 février 2026  
> Périmètre : backend (Spring Boot / Kotlin) + frontend (Nuxt 3 / Vue 3)

---

## Contexte

Les utilisateurs disposant d'un grand nombre de transactions enregistrées constataient des latences
perceptibles lors de la consultation d'un livret (compte). L'analyse initiale a mis en évidence
plusieurs sources de dégradation, principalement dans la couche infrastructure côté backend, et dans
les appels réseau effectués par le frontend.

---

## 1. Diagnostic initial — problèmes identifiés

### 1.1 Chargement de la totalité des transactions en mémoire

**Fichier concerné (avant) :**
`infra/…/adapters/transaction/TransactionRepositoryJpaAdapter.kt`

```kotlin
// AVANT — toutes les transactions du livret sont chargées en mémoire,
// puis filtrées côté JVM
override fun findTransactionsByBookletYearAndMonth(bookletId: UUID, year: Int, month: Month) =
    bookletJpaRepository.findTransactionsById(bookletId)
        ?.sheets
        ?.filter { it.date.year == year && it.date.month == month }
        ?.map { it.toModel() }
```

Le repository JPA chargeait **l'intégralité** des transactions du livret (`LEFT JOIN FETCH account.sheets`),
puis filtrait les résultats en mémoire côté JVM. Pour un utilisateur ayant 500+ transactions, cela
représentait un volume de données inutilement transféré depuis la base et un coût de mapping O(n).

---

### 1.2 Agrégat complet chargé pour calculer uniquement les soldes

**Fichier concerné (avant) :**
`domain/…/port/api/BookletFeature.kt` — `loadTransactionsForBookletForAMonth`

L'unique point d'entrée du contrôleur (`GET /api/account/report/{id}`) chargeait :
1. L'agrégat `Booklet` **avec toutes ses transactions** via `findAccountByIdWithTransactions`,
2. Les transactions régulières associées,
3. Les trackers de transactions régulières **un par un** (appels N+1),

…même lorsque le frontend n'avait besoin que des soldes (`realSold` / `previewSold`) pour afficher
les cartes de la sidebar.

---

### 1.3 Appels N+1 sur les trackers de transactions régulières

**Fichier concerné (avant) :**
`domain/…/port/api/BookletFeature.kt` — section génération de transactions prévisionnelles

Le code appelait `trackerRepository.findTracker(regularTransactionId, bookletId)` à l'intérieur
d'une boucle sur chaque transaction du mois, générant autant de requêtes SQL que de transactions
régulières actives.

---

### 1.4 Un seul endpoint pour deux besoins différents

Le frontend consommait `GET /api/account/report/{id}?month=X&year=Y` pour deux usages distincts :
- Afficher les **soldes** dans la barre latérale / carte de résumé.
- Afficher la **liste des transactions** dans la vue détaillée du livret.

Un seul appel retournait toutes les données alors que chaque vue n'en avait besoin que d'une partie.

---

## 2. Améliorations apportées

### 2.1 Filtrage des transactions côté base de données

**Nouveaux fichiers :**
- `domain/…/port/spi/repository/TransactionQueryRepository.kt` *(nouveau port SPI)*
- `infra/…/repositories/TransactionQueryJpaRepository.kt` *(nouveau dépôt JPA read-only)*
- `infra/…/adapters/transaction/TransactionQueryRepositoryJpaAdapter.kt` *(nouvel adaptateur)*

**Requête JPQL introduite :**

```kotlin
@Query("""
    SELECT s
    FROM TransactionResource s
    LEFT JOIN FETCH s.personalTag
    LEFT JOIN FETCH s.tag
    WHERE s.account.idAccount = :bookletId
      AND s.date >= :from
      AND s.date <= :to
    ORDER BY s.date, s.lastModified
""")
fun findByBookletIdAndDateBetween(bookletId: UUID, from: LocalDate, to: LocalDate): List<TransactionResource>
```

Le filtrage par plage de dates est désormais délégué au moteur SQL. Seules les transactions du mois
demandé transitent sur le réseau applicatif et sont mappées en objets domaine.

**Impact :** réduction linéaire du volume traité — O(n total) → O(n mois).

---

### 2.2 Projection « balances only » sans chargement de l'agrégat

**Nouveaux fichiers :**
- `domain/…/port/spi/repository/BookletBalanceQueryRepository.kt` *(nouveau port SPI)*
- `infra/…/repositories/BookletBalanceJpaRepository.kt` *(dépôt JPA read-only, projection Spring Data)*
- `infra/…/adapters/BookletBalanceQueryRepositoryJpaAdapter.kt` *(adaptateur)*

**Requête JPQL introduite :**

```kotlin
@Query("""
    SELECT acc.label AS label, acc.amount AS amount, acc.previewAmount AS previewAmount
    FROM BookletResource acc
    WHERE acc.idAccount = :id
""")
fun findPersistedBalances(id: UUID): PersistedBalancesRow?
```

Au lieu de charger l'agrégat `Booklet` entier (label + toutes transactions + regularTransactions),
une projection minimale ne sélectionne que les trois colonnes nécessaires au calcul des soldes.

**Impact :** passage d'une requête avec `LEFT JOIN FETCH` sur des centaines de lignes à une requête
en `O(1)` sur une seule ligne de la table `account`.

---

### 2.3 Élimination des appels N+1 sur les trackers

**Fichier modifié :**
`domain/…/port/api/BookletFeature.kt` — `loadTransactionsForBookletForAMonth`

```kotlin
// AVANT — un appel par transaction régulière
regularTransactions.forEach { rt ->
    val tracker = trackerRepository.findTracker(rt.id, bookletId) // N requêtes
    ...
}

// APRÈS — chargement groupé, puis lookup O(1) par map
val trackersByRegularId = trackerRepository
    .findAllTrackersForBooklet(bookletId)       // 1 seule requête
    .associateBy { it.regularTransactionId }

filteredTransactions.filter { transaction ->
    val tracker = trackersByRegularId[transaction.regularTransactionId] // O(1)
    ...
}
```

**Méthode ajoutée au port :**
`RegularTransactionTrackerRepository.findAllTrackersForBooklet(bookletId: UUID)`  
**Implémentation JPA :**
`JpaRegularTransactionTrackerRepository.findAllByBookletId(bookletId: UUID)`

**Impact :** N requêtes SQL → 1 requête SQL, quel que soit le nombre de transactions régulières.

---

### 2.4 Découplage des endpoints — balances vs transactions

**Fichier modifié :**
`infra/…/api/booklet/Controller.kt`

Deux nouveaux endpoints REST ont été ajoutés :

| Endpoint | Données retournées | Coût |
|---|---|---|
| `GET /api/account/{id}/balances?month=X&year=Y` | `label`, `realSold`, `previewSold` | Léger — projection DB |
| `GET /api/account/{id}/transactions?month=X&year=Y` | Liste des transactions du mois | Borné au mois |

L'ancien `GET /api/account/report/{id}` reste disponible pour la rétro-compatibilité mais n'est
plus utilisé par les vues principales.

**Nouvelles DTOs :**
- `BookletBalancesResponse(label, realSold, previewSold)`
- `BookletTransactionsResponse(transactions)`

**Nouveau use-case domaine :**
`BookletFeature.loadBalancesForBookletForAMonth(...)` → `Result<BookletBalances>`

Ce use-case construit un agrégat **minimal** (`Booklet` sans transactions) en s'appuyant
exclusivement sur la projection balance, puis calcule `previsionalSold` en ne chargeant que les
transactions prévisionnelles de la fenêtre temporelle courante→cible.

---

### 2.5 Adaptation du frontend — requêtes parallèles et ciblées

**Fichier modifié :**
`client/composables/useBooklet.ts`

Deux nouvelles fonctions ont été exposées :

```typescript
async function findBalancesByIdMonthAndYear(
  accountId: string, month: number, year: number
): Promise<BookletBalancesDTO> {
  return get(`account/${accountId}/balances`, { month, year })
}

async function findTransactionsByIdMonthAndYear(
  accountId: string, month: number, year: number
): Promise<BookletTransactionsDTO> {
  return get(`account/${accountId}/transactions`, { month, year })
}
```

**Fichier modifié :**
`client/pages/account/[id].vue` — fonction `loadBookletData`

```typescript
// AVANT — un seul appel retournant tout
const result: BookletReport = await findByIdMonthAndYear(accountId, month, year)

// APRÈS — deux appels parallèles, chacun ciblé
const [balances, transactionsRes] = await Promise.all([
  findBalancesByIdMonthAndYear(accountId, month, year),
  findTransactionsByIdMonthAndYear(accountId, month, year),
])
```

**Impact :**
- Les deux requêtes s'exécutent en parallèle (gain réseau).
- La vue peut désormais afficher les soldes indépendamment de la liste (progressive rendering possible).
- Correction d'un bug JavaScript : `accounts is undefined` causé par l'utilisation de l'ancienne
  réponse dont la structure avait changé.

---

## 3. Résumé des fichiers créés / modifiés

### Nouveaux fichiers

| Fichier | Rôle |
|---|---|
| `domain/…/repository/TransactionQueryRepository.kt` | Port SPI read-optimized pour les transactions |
| `domain/…/repository/BookletBalanceQueryRepository.kt` | Port SPI projection balance |
| `domain/…/models/BookletBalances.kt` | Modèle domaine léger |
| `infra/…/repositories/TransactionQueryJpaRepository.kt` | Repository JPA read-only avec JPQL filtré |
| `infra/…/repositories/BookletBalanceJpaRepository.kt` | Repository JPA read-only projection 3 colonnes |
| `infra/…/adapters/transaction/TransactionQueryRepositoryJpaAdapter.kt` | Adaptateur hexagonal |
| `infra/…/adapters/BookletBalanceQueryRepositoryJpaAdapter.kt` | Adaptateur hexagonal |

### Fichiers modifiés

| Fichier | Modification |
|---|---|
| `domain/…/port/api/BookletFeature.kt` | Ajout de `loadBalancesForBookletForAMonth`, injection des nouveaux ports, chargement groupé des trackers, timing logs |
| `infra/…/api/booklet/Controller.kt` | Ajout des endpoints `/balances` et `/transactions`, DTOs `BookletBalancesResponse` / `BookletTransactionsResponse` |
| `client/composables/useBooklet.ts` | Ajout de `findBalancesByIdMonthAndYear` et `findTransactionsByIdMonthAndYear` |
| `client/pages/account/[id].vue` | Remplacement de l'appel unique par `Promise.all`, adaptation de la consommation des réponses |

---

## 4. Tableau de synthèse des gains

| Problème | Avant | Après | Technique |
|---|---|---|---|
| Filtrage transactions | Toutes les TX chargées en JVM, filtrage applicatif | Filtrage SQL `WHERE date BETWEEN` | JPQL read-only bounded query |
| Chargement pour les soldes | Agrégat complet (TX + regular TX) | Projection 3 colonnes | Spring Data projection interface |
| Trackers N+1 | 1 requête SQL par transaction régulière | 1 seule requête + `Map` lookup | Bulk load + `associateBy` |
| Endpoint unique surchargé | 1 appel retournant toutes les données | 2 endpoints spécialisés | CQRS-like read-side split |
| Appels frontend séquentiels | 1 appel bloquant | 2 appels parallèles (`Promise.all`) | Parallelisation HTTP |

---

## 5. Architecture et respect de l'hexagonale

Toutes les améliorations respectent l'architecture hexagonale du projet :

- Les nouveaux ports (`TransactionQueryRepository`, `BookletBalanceQueryRepository`) sont définis
  dans le module **domain** et annotés `@Port(Side.INFRASTRUCTURE)`.
- Les implémentations concrètes (JPA) résident exclusivement dans le module **infra** et ne
  contaminent pas la logique métier.
- Le domain ne dépend d'aucun détail JPA/SQL.
- La logique de calcul de `previsionalSold` reste intégralement dans le domain service
  `BookletFeatureImpl`.

> **Note :** Les ports de requête (`TransactionQueryRepository`, `BookletBalanceQueryRepository`)
> ont été introduits comme ports de lecture dédiés (pattern *read-side / query port*), permettant
> d'exposer des requêtes optimisées sans altérer les ports d'écriture existants
> (`BookletRepository`, `TransactionRepository`).

