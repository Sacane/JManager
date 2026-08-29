## Observation

L'audit [docs/technical/jpa-transactions/2026-08-29-jpa-fetch-and-transaction-boundary-audit.md](../technical/jpa-transactions/2026-08-29-jpa-fetch-and-transaction-boundary-audit.md)
(finding D) a corrigé `EditBookletService` et `SaveBookletService` — les deux exemples
concrètement vérifiés faisant plusieurs appels de repository non atomiques dans `handle()` sans
passer par `executeInTransaction`. `DeleteAccountUseCase` a été identifié avec le même symptôme
(impact jugé faible, non corrigé) et **44 fichiers `handle()` au total** n'appellent pas
`executeInTransaction` — la majorité n'en a probablement pas besoin (un seul appel de repository),
mais aucun tri systématique n'a été fait pour confirmer lesquels font réellement 2+ appels non
lus-seuls.

## Location

Rechercher les `handle()` sans `executeInTransaction` :
```
grep -rL "executeInTransaction" $(grep -rl "override fun handle" domain/src/main/kotlin --include="*.kt")
```
`DeleteAccountUseCase.kt` en est l'exemple déjà identifié.

## Expected behaviour

Chaque `handle()` faisant plusieurs appels de repository dont au moins une écriture devrait les
envelopper dans `infraTransactionManager.executeInTransaction(...)`, suivant le pattern déjà
appliqué à `DeleteTransactionsByIdsService`, `EditBookletService`, `SaveBookletService`, etc.

## Impact

Fenêtres de course rares (nécessitent une action utilisateur en double, quasi simultanée) sur les
handlers concernés — sévérité faible à moyenne selon le handler. Un inventaire complet permettrait
de confirmer l'ampleur réelle et de prioriser.
