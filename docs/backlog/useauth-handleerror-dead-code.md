# useAuth: `_handleError` fonction morte ou branchement oublié

**Observation** : `composables/useAuth.ts` définit une fonction `handleError(error: Error)` (renommée `_handleError` pour satisfaire le lint après le bump `@antfu/eslint-config` v9) qui inspecte une `AxiosError`, distingue un code domaine spécifique et les statuts 401/403, et déclenche `clearAuthState()` + `navigateTo('/login')`.

**Localisation** : [client/composables/useAuth.ts:142](../../client/composables/useAuth.ts) (fonction `_handleError`).

**Constat** : cette fonction n'est ni appelée ailleurs dans le fichier, ni retournée dans l'objet exposé par le composable (`return { user, isAuthenticated, login, logout, register, isAdmin, tryRefresh, initializeSession }` — pas de `handleError`). Elle est donc actuellement du code mort.

Sa logique (redirection sur 401/403, code domaine spécifique) fait doublon partiel avec ce qui est déjà géré inline dans `tryRefresh()` (mêmes cas 401/403 traités directement dans son `catch`), ce qui suggère soit :
- un refactoring inachevé où `handleError` devait remplacer/factoriser cette logique dupliquée mais n'a jamais été branché,
- soit une fonction prévue pour `register()` (qui a son propre `catch (e) { onError(e) }` sans cette logique de redirection) mais oubliée lors du branchement.

**Comportement attendu** : soit la fonction est effectivement utilisée quelque part (à brancher — probablement dans `register()` ou exposée pour usage externe), soit elle est un reliquat à supprimer.

**Impact** : aucun bug actif (code mort n'exécute rien), mais source de confusion et dette technique. Risque d'un vrai gap fonctionnel si elle était censée gérer un cas d'erreur (ex: code domaine 1) qui n'est aujourd'hui géré nulle part ailleurs.
