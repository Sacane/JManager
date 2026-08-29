# Bug Report — Le tri par date est ignoré quand un autre tri de colonne est actif

**Date**: 2026-08-29

## Symptom

Sur la page détail d'un livret (`client/pages/booklet/[id].vue`), vue **tableau** :

1. L'utilisateur clique sur un en-tête triable — `Libellé`, `Dépenses` ou `Recettes`. Le tableau se trie sur cette colonne.
2. L'utilisateur clique ensuite sur l'en-tête **Date**.
3. **Observé** : la flèche de l'en-tête Date bascule (↓ ↔ ↑) mais l'ordre des lignes ne change pas — il reste celui de la colonne précédente.
4. **Attendu** : les transactions se réordonnent par date (across pages), et le tri précédent est abandonné.

Initialement rapporté comme « ne marche que sur tablette » : fausse piste. La vue tablette (> 768 px) affiche le même tableau que le bureau ; le lien avec la tablette venait du fait que l'utilisateur y avait cliqué une colonne triable au préalable. Le bug se reproduit à l'identique sur bureau.

L'inverse est vrai aussi : après un tri Date, l'indicateur de la flèche Date reste « actif » alors qu'un tri de colonne PrimeVue a repris la main.

## Root Cause

Deux mécanismes de tri **indépendants et non coordonnés** agissent sur le même tableau :

1. **Tri par date — server-side.** L'en-tête Date est un `<button>` custom (`#header-dateSort`, [\[id\].vue:951](../../../client/pages/booklet/[id].vue#L951)). Il bascule le ref `sortDirection` et rappelle `loadBookletData()`, qui refait la requête avec `?sortDirection=` ; le backend réordonne toute la période avant pagination et `actualTransactions` est remplacé. La colonne Date n'est **pas** `sortable` côté PrimeVue (retiré dans `bfeddbc6`).

2. **Tri par colonne — client-side PrimeVue.** Les colonnes `Libellé`, `Dépenses`, `Recettes` sont déclarées `sortable: true` ([\[id\].vue:769-777](../../../client/pages/booklet/[id].vue#L769)). `AppTable` ne transmet **aucun** `sortField` / `sortOrder` / `@sort` à `DataTable` ([AppTable.vue:50-63](../../../client/components/AppTable.vue#L50)) : PrimeVue gère donc ce tri en mode **non contrôlé**. Il conserve son état interne `d_sortField` / `d_sortOrder` et **réapplique le tri à chaque nouvelle valeur de `:value`** qu'il reçoit.

Conséquence : quand un tri de colonne PrimeVue est actif et que l'utilisateur bascule l'en-tête Date, `loadBookletData()` récupère bien des lignes triées par date et remplace `actualTransactions`, mais PrimeVue re-trie immédiatement ce tableau frais par sa colonne collante. Les deux tris « fusionnent », PrimeVue gagne — au lieu de « le dernier tri demandé gagne ».

Le test `preserves the order returned by the backend instead of re-sorting the loaded page` ([booklet-id.spec.ts:291](../../../client/tests/pages/booklet-id.spec.ts#L291)) ne détecte rien : il vérifie `vm.filteredTransactions` (les données passées au tableau) et le stub `DataTable` est un simple passe-plat (`booklet-id.spec.ts:150`), donc le tri interne de PrimeVue n'existe pas dans les tests.

> Le bug est causé par l'absence de coordination entre le tri server-side (bouton Date custom) et le tri client-side non contrôlé de PrimeVue (`sortable` sur Libellé/Dépenses/Recettes) dans `client/pages/booklet/[id].vue` + `client/components/AppTable.vue`, ce qui fait que PrimeVue réordonne systématiquement les lignes rechargées par sa dernière colonne triée et masque le tri par date.

## Comportement cible (demandé par le développeur)

Un seul tri actif à la fois. Le dernier tri demandé par l'utilisateur gagne. Pas de fusion.
Décision : **tout le tri passe server-side** (Date, Libellé, Dépenses, Recettes), across pages.

## Fix appliqué

### `domain`
- Nouvel enum `TransactionSortField { DATE, LABEL, EXPENSE, INCOME }`.
- `LoadTransactionsForBookletForAMonthQuery.sortField` (nullable) à côté de `sortDirection`.
  `sortDirection != null` sans `sortField` ⇒ `DATE` (rétro-compat, aucun test date existant modifié).
- `sortForDisplay` généralisé : `LABEL` = comparaison insensible à la casse puis date ;
  `EXPENSE` / `INCOME` = tri par montant dans le sens concerné, l'autre sens toujours rejeté en fin
  de liste en ordre date, quelle que soit la direction ; confirmées/prévisionnelles interclassées.
- `directed()` + `orderByKindThenAmount()` extraits. 9 tests (`LoadTransactionsWithFieldSortTest`).

### `application`
- `GET {bookletID}/transactions?sortField=` — bindé sur l'enum du domaine, valeur inconnue ⇒ 400
  via `ProblemDetailHandler` existant. 5 tests (`TransactionsSortFieldTest`).

### `client`
- `AppTable.vue` : prop `lazy` (défaut `false`), props contrôlées `sortField` / `sortOrder`,
  évènement `sort` + `update:sortField` / `update:sortOrder`. En `lazy`, PrimeVue ne trie plus rien.
- `booklet/[id].vue` : `AppTable` passé en `lazy` ; suppression du bouton custom `#header-dateSort`,
  de `toggleDateSort` et du style `.date-sort-btn` ; la colonne Date redevient `sortable`, les
  colonnes montant deviennent `field: 'expense'` / `'income'`. Un seul état `activeSort`
  (`{ field, direction }`, défaut `DATE`/`DESCENDING`) ; `onSort` traduit l'évènement PrimeVue,
  remet la page à 0 et recharge le backend — le dernier clic gagne, sans fusion. Le mode
  « tout le mois » trie le jeu complet en mémoire via `compareBySort` (miroir de la règle domaine).
- `useBooklet.findTransactionsByIdMonthAndYear(..., sortDirection?, sortField?)`.

### Hors périmètre
`docs/backlog/regular-transactions-primevue-client-sort.md` — `regular-transaction/index.vue` a le
même défaut latent (colonnes `sortable` PrimeVue en pagination server-side). `AppTable` reste
`lazy: false` par défaut : cette page n'est pas modifiée.

## Alternative écartée

### Approche recommandée — `AppTable` à tri contrôlé + état de tri centralisé sur la page

1. **`AppTable.vue`** : exposer le tri de `DataTable`.
   - Ajouter `v-model:sortField` + `v-model:sortOrder` (ou un modèle `sort` unique) et relayer l'évènement `@sort` de `DataTable`.
   - Passer ces props à `DataTable` → PrimeVue passe en mode contrôlé.

2. **`pages/booklet/[id].vue`** : un unique état `activeSort`.
   - Clic sur le bouton **Date** → `activeSort = { kind: 'date', direction }` ; **réinitialiser** le tri PrimeVue (`sortField = null`, `sortOrder = null`) ; `loadBookletData()` avec `sortDirection`.
   - `@sort` d'une colonne PrimeVue → `activeSort = { kind: 'column', field, order }` ; mettre `sortDirection = null` (le backend renvoie son ordre neutre) ; laisser PrimeVue trier la page.
   - L'indicateur (flèche) de l'en-tête Date ne s'affiche « actif » que si `activeSort.kind === 'date'`.

3. Bénéfice transverse : `pages/regular-transaction/index.vue` utilise aussi `AppTable` avec des colonnes `sortable` — le tri contrôlé y sera disponible si besoin.

**Limite connue non traitée par ce correctif** : `Libellé` / `Dépenses` / `Recettes` restent des tris **locaux à la page chargée** (PrimeVue ne trie que les lignes courantes, pas across pages). C'est la limitation pré-existante que `bfeddbc6` n'a corrigée que pour la Date. La lever demanderait un tri server-side multi-champ (backend + domaine) — feature séparée, hors de ce bug.

### Alternative — tout le tri en server-side

Étendre le tri backend pour accepter `field` + `direction` sur label/expense/income, retirer `sortable` de toutes les colonnes PrimeVue, chaque en-tête déclenche `loadBookletData()`. Tri correct across pages sur toutes les colonnes, mais changement backend + domaine + tests : à traiter comme une évolution, pas comme ce correctif.

## Non-Regression Tests

| Couche | Fichier | Cas |
|---|---|---|
| domain | `BookletFeatureTest$LoadTransactionsWithFieldSortTest` | label asc/desc, expense (incomes en fin, quelle que soit la direction), income (expenses en fin), interclassement confirmées/prévisionnelles, `field` sans `direction`, `direction` sans `field` |
| application | `BookletControllerTest$TransactionsSortFieldTest` | `sortField=LABEL\|EXPENSE\|INCOME` ordonne le corps de réponse ; `sortField` inconnu ⇒ 400 |
| client | `tests/components/AppTable.spec.ts` | `lazy` transmis à `DataTable` ; `sortField`/`sortOrder` contrôlés transmis ; `@sort` re-émis en `sort` + `update:sortField`/`update:sortOrder` |
| client | `tests/pages/booklet-id.spec.ts` (`pages/booklet/[id] sorting`) | requête par défaut `DESCENDING`/`DATE` ; `onSort` date/label/expense recharge avec les bons paramètres ; **un 2ᵉ tri de colonne remplace l'`activeSort` au lieu de fusionner** ; `primeSortField`/`primeSortOrder` reflètent l'`activeSort` ; direction conservée au changement de page |
| client | `tests/unit/useBooklet.spec.ts` | `findTransactionsByIdMonthAndYear` transmet `sortField` |

## Follow-up

- `docs/backlog/regular-transactions-primevue-client-sort.md` — même défaut latent sur la page des transactions régulières (non modifiée par ce correctif).
