# `regular-transaction/index.vue` — tri de colonnes client-side sur une liste paginée server-side

## Observation

`client/pages/regular-transaction/index.vue` déclare les colonnes `label`, `isIncome`, `value`,
`regularity` avec `sortable: true` (`regularTransactionColumns`, ~ligne 270). L'`AppTable` y est
utilisé **sans** `lazy`, donc PrimeVue trie ces colonnes lui-même — uniquement sur la **page
chargée**, alors que la liste est paginée côté serveur (`getRegularTransaction(page, size)`).

C'est le même défaut que celui corrigé pour `booklet/[id].vue` (cf.
`docs/bugs/date-sort-overridden-by-column-sort/`) : le tri d'une colonne ne réordonne que ~10
lignes visibles, jamais l'ensemble ; et si l'utilisateur combine deux tris, PrimeVue garde le
dernier de façon collante en écrasant tout rechargement.

## Emplacement

- `client/pages/regular-transaction/index.vue` — `regularTransactionColumns`, `loadRegularTransactions`, `onRtPageChange`.
- Endpoint : `GET /regular-transaction` (`useRegularTransaction().getRegularTransaction`) — **n'accepte aucun paramètre de tri** aujourd'hui.

## Comportement attendu

Tri server-side, un seul tri actif, à travers toutes les pages — comme `booklet/[id].vue` :
`AppTable` en `lazy`, `@sort` → rechargement backend, `sortField` + `sortDirection` sur l'endpoint.
Nécessite d'ajouter le tri à la query domaine des transactions régulières + au contrôleur.

## Impact

Moyen. Fonctionnellement trompeur (le tri "ment" dès qu'il y a plus d'une page), mais sans perte
de données ni erreur. Pas de régression introduite : `AppTable` reste `lazy: false` par défaut,
cette page n'a pas changé de comportement.
