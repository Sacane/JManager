## Observation

Every night at 02:00, `RetentionScheduler` (infrastructure) runs `PurgeExpiredDataService.purge()`
to hard-delete accounts that never gave consent past the retention window. In production this
currently fails with a `DataIntegrityViolationException` for at least one account:

```
2026-08-29 02:00:00.233 ERROR [scheduling-1] o.h.e.jdbc.spi.SqlExceptionHelper [req=] -
ERROR: update or delete on table "booklet" violates foreign key constraint
"monthly_transaction_booklet_id_account_fkey" on table "regular_transaction_booklet"
  Detail: Key (id_booklet)=(81d55bc6-eb45-4cf8-9901-a6004c44fa29) is still referenced
  from table "regular_transaction_booklet".
```

## Location

- `domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/retention/PurgeExpiredDataUseCase.kt:42`
  (`PurgeExpiredDataService.purge`) → calls `userRepository.deleteById(userId)`.
- `infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/UserRepositoryJpaAdapter.kt:186`
  (`deleteById`) → `userPostgresRepository.deleteById(id)`, which relies on JPA cascade to also
  delete the user's booklets. The cascade removes the `booklet` row but does not first remove the
  linking rows in `regular_transaction_booklet` (the table that attaches a `RegularTransaction` to
  a `Booklet`), so Postgres rejects the delete.
- Triggered by `infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/scheduler/RetentionScheduler.kt:34`.

## Expected behaviour

Purging an expired/unconsented account should delete (or the schema should cascade-delete) every
row that references its booklets — including `regular_transaction_booklet` — so the account is
fully removed, exactly like the interactive "delete booklet" flow already handles it
(`BookletJpaRepositoryAdapter.deleteBookletById` explicitly calls
`booklet.clearAllRegularTransactions()` before deleting the booklet — the purge path is missing
the equivalent step).

## Impact

- The nightly retention/GDPR purge **silently fails** for any unconsented account that has at
  least one recurring transaction linked to a booklet — `LoggingErrorHandler` logs the exception
  and the scheduler moves on, `PurgeSummary` under-reports what was actually deleted, and the
  account is never purged, night after night.
- This is a compliance-relevant bug (data that should be deleted per the retention policy is kept
  indefinitely for affected accounts), not just a log-noise issue.
