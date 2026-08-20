# Domain Module — Selective Regeneration of Deleted Provisional Transactions

**Context**
Today, `RegenerateDeletedPrevisionalTransactionsUseCase` restores **every** excluded regular transaction for a given booklet/month/year in one blind operation: the user has no visibility into what was deleted and no way to restore only some of it. We want the user to be able to review each deleted provisional/virtual transaction (label, amount, date) for the target month and choose precisely which ones to bring back, with an option to select them all at once.

Deletion already tracks exclusion at `(regularTransactionId, bookletId, YearMonth)` granularity in `RegularTransactionTracker.excludedMonths` (see `DeleteTransactionsByIdsUseCase`) — there is no per-occurrence exclusion. For `Monthly`/`Yearly` regular transactions this maps cleanly to "one candidate per deleted transaction". For `Weekly`/`Daily` regular transactions, which can produce several occurrences in the same month, restoring one candidate restores **the whole excluded month** for that regular transaction — this is a known, accepted limitation for this iteration and must not be silently hidden from the caller.

The domain must expose:
1. A **read-only preview** of what can be regenerated for a booklet/month/year: one candidate per currently-excluded `(regularTransactionId, month)` pair, carrying the regular transaction's label, amount, isIncome, tag, and the projected occurrence date within that month (computed the same way generation already computes it, but without the exclusion gate).
2. A **selective regeneration** command that only un-marks exclusion and (re)generates for the `regularTransactionId`s explicitly passed in, instead of every excluded regular transaction for the month.

The existing golden rule is unchanged: the current month produces persisted previsional transactions, a future month produces virtual (computed, non-persisted) transactions, and a past month never regenerates anything.

**Acceptance Criteria**
```gherkin
Feature: Selective regeneration of deleted provisional transactions
  In order to control exactly which deleted forecast transactions come back
  As an authenticated user
  I want to preview and choose which excluded regular transactions to regenerate for a given month

Scenario: 1. List regenerable candidates for the current month
  Given an authenticated user who previously deleted provisional transactions for the current month
  When the user requests the list of regenerable transactions for the current month and year
  Then the system returns one candidate per excluded regular transaction
  And each candidate exposes the regular transaction's label, amount, isIncome, tag and projected date within the month

Scenario: 2. List regenerable candidates for a future month
  Given an authenticated user who previously deleted virtual transactions for a future month
  When the user requests the list of regenerable transactions for that future month and year
  Then the system returns one candidate per excluded regular transaction
  And each candidate's date is computed on-the-fly without persisting anything

Scenario: 3. No candidates when nothing was excluded
  Given an authenticated user with no excluded regular transactions for the requested month
  When the user requests the list of regenerable transactions for that month and year
  Then the system returns an empty list

Scenario: 4. No candidates for a past month
  Given an authenticated user who requests the list of regenerable transactions for a past month
  When the request is executed
  Then the system returns an empty list

Scenario: 5. Selectively regenerate provisional transactions for the current month
  Given an authenticated user with several excluded regular transactions for the current month
  When the user requests regeneration for the current month with a subset of regular transaction identifiers
  Then only the selected regular transactions are un-marked as excluded in the tracker
  And only the selected transactions are recreated as persisted previsional transactions (isPreview = true)
  And the regular transactions not selected remain excluded

Scenario: 6. Selectively regenerate virtual transactions for a future month
  Given an authenticated user with several excluded regular transactions for a future month
  When the user requests regeneration for that future month with a subset of regular transaction identifiers
  Then only the selected regular transactions are un-marked as excluded in the tracker
  And the system returns the corresponding virtual transactions (computed on-the-fly, not persisted)
  And no previsional transaction is created in the database for the unselected regular transactions

Scenario: 7. Select-all regenerates every excluded regular transaction
  Given an authenticated user with several excluded regular transactions for a given month
  When the user requests regeneration with the identifiers of every currently-excluded regular transaction
  Then all of them are un-marked as excluded
  And all corresponding transactions are regenerated, matching the previous "restore everything" behaviour

Scenario: 8. Regeneration for a past month has no effect regardless of selection
  Given an authenticated user who requests regeneration for a past month with one or more regular transaction identifiers
  When the request is executed
  Then the system returns an empty list
  And no tracker is modified

Scenario: 9. Selecting a regular transaction identifier that is not currently excluded is ignored
  Given an authenticated user who selects a regular transaction identifier that is not excluded for the requested month
  When the user requests regeneration with that identifier
  Then the identifier is ignored
  And no tracker is modified for that regular transaction
```

**Notes**
- Weekly/Daily regular transactions restore at month granularity, not per-occurrence, because `RegularTransactionTracker.excludedMonths` does not track individual dates. Surface this constraint in the candidate DTO/documentation rather than implying per-occurrence precision.
- Reuse the existing occurrence-computation logic (`generateMissingPrevisionalTransactions` for the current month, `calculateVirtualTransactions` for future months) rather than duplicating date/frequency math — it likely needs a variant that computes candidates while bypassing the exclusion check, restricted to the already-excluded regular transactions.
- `RegenerateDeletedPrevisionalTransactionsCommand` needs a new field (e.g. `regularTransactionIds: List<RegularTransactionId>`) to carry the selection; decide whether to extend the existing command/use case or introduce a new one, keeping `docs/features/restore-deleted-provisional-transactions/application_*.md` and `client_*.md` in sync with the chosen contract.
