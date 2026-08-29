# Bug Report — "La transaction demandée est introuvable" when deleting a previsional transaction

**Date**: 2026-08-29
**Status**: ✅ Fixed. See "Confirmed Root Cause" and "Fix (applied)" below; the sections further
down are kept as an investigation log (a disproven hypothesis and a corrected one), not the final
answer.

## Confirmed Root Cause

`userOwnsBooklet(bookletRepository, command.userId, command.bookletID)` — added to
`DeleteTransactionsByIdsService` (and identically to `EditTransactionUseCase`,
`ConfirmPreviewTransactionUseCase`, `ConfirmVirtualTransactionUseCase`) in commit `743c22c8`
(`feat(domain): enforce user ownership checks on booklet operations`) — calls
`BookletRepository.findBookletsForUser`, implemented by
[`BookletJpaRepositoryAdapter.findBookletsForUser`](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/BookletJpaRepositoryAdapter.kt)
→ [`BookletJpaRepository.findAllBookletsByUserId`](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/repositories/BookletJpaRepository.kt:44):

```sql
SELECT DISTINCT b FROM BookletResource b
LEFT JOIN FETCH b.transactions
LEFT JOIN FETCH b.regularTransactions
WHERE b.owner.idUser = :userId
```

`BookletResource.transactions` is a plain `MutableList` (a Hibernate **bag** — `@OneToMany` with no
`@OrderColumn`); `regularTransactions` is a `MutableSet`. **Fetching a bag alongside another
collection in the same JPQL query is a classic Hibernate cartesian-product trap**: the SQL produces
one row per `(transaction, regularTransaction)` pair for each booklet, and JPQL `DISTINCT` only
deduplicates the *root* entity (`b`) — not bag elements. With **N** regular transactions linked to
a booklet, every transaction belonging to that booklet — including ones with no
`regularTransactionId` at all, since the duplication happens at the booklet level, not per
transaction — ends up appearing **N times** in the in-memory `transactions` list.

This alone would just be wasteful, except that `userOwnsBooklet`'s query runs **first**, inside the
exact same Spring `@Transactional` boundary (`UnitOfWorkPostgresSpringTransactionalAdapter.executeInTransaction`)
as the subsequent `bookletRepository.findBookletByIdWithTransactions(command.bookletID)` call — both
`BookletJpaRepositoryAdapter` methods are themselves `@Transactional` (propagation `REQUIRED`), so
they share **one Hibernate session**. Hibernate's session-level identity map then returns the
*same* managed `BookletResource` instance for the second call (matched by primary key) instead of
re-fetching — reusing the already-initialized, **duplicated** `transactions` list from the first
query.

`DeleteTransactionsByIdsService` then does:

```kotlin
val transactionsToDelete = booklet.transactions.filter { command.transactionIds.contains(it.id) }
if (transactionsToDelete.size != command.transactionIds.size) {
    return TRANSACTION_NOT_FOUND
}
```

With the target transaction duplicated N times, `transactionsToDelete.size` is N instead of 1, the
size check fails, and the service reports `TRANSACTION_NOT_FOUND` — even though the transaction
genuinely exists, belongs to the right booklet, and both ids in the request payload were correct
(confirmed by the user directly against the database).

### Reproduction (confirmed)

Integration test against real Testcontainers Postgres — booklet with **3** regular transactions
linked, plus **1** manually-created preview transaction (`regularTransactionId = null`, matching
the user's actual scenario, not one generated from a regular transaction):

```
DeleteTransactionsByIdsUseCaseOwnershipSideEffectTest >
  deleting a preview transaction should succeed even when the booklet has several regular
  transactions linked() FAILED
    [delete result: Certaines transactions à supprimer sont introuvables pour le livret
    95db55ca-eba4-4956-a41d-e5dd42064643]
    expected: OK
     but was: TRANSACTION_NOT_FOUND
```

Test file (currently `@Disabled`, kept as the ready-to-enable Red step for the fix):
[infrastructure/src/test/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/transaction/DeleteTransactionsByIdsUseCaseOwnershipSideEffectTest.kt](../../../infrastructure/src/test/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/transaction/DeleteTransactionsByIdsUseCaseOwnershipSideEffectTest.kt)

An earlier attempt with only **1** regular transaction linked passed (no visible duplication —
N=1 doesn't produce a detectable size mismatch), which is why the first pass at this
investigation (below) wrongly ruled the ownership check out. The real trigger is **2+** regular
transactions linked to the same booklet, which is a completely ordinary state for an active
personal-finance account.

### Blast radius

Not specific to delete. The identical `userOwnsBooklet`-then-`findBookletByIdWithTransactions`
pattern, added by the same commit, also affects:

- `EditTransactionUseCase` (editing any transaction on an affected booklet)
- `ConfirmPreviewTransactionUseCase` (confirming a previsional transaction)
- `ConfirmVirtualTransactionUseCase` (confirming a virtual/recurring occurrence)

Any authenticated user with **2 or more regular transactions linked to the same booklet** can hit
`TRANSACTION_NOT_FOUND` on any of these four operations, for any transaction in that booklet,
regardless of whether that specific transaction relates to a regular transaction at all.

### Fix (applied)

`userOwnsBooklet` only needs a yes/no membership answer — it never needed to eager-fetch full
transaction/regular-transaction graphs for every booklet the user owns just to check one id.

- **Domain** — new port method
  [`BookletRepository.existsBookletForUser(userId, bookletId): Boolean`](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/port/output/repository/BookletRepository.kt),
  documented as intentionally fetch-join-free. `userOwnsBooklet`
  ([BookletDomainHelper.kt](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/booklet/BookletDomainHelper.kt))
  now calls it directly instead of `findBookletsForUser(userId).any { it.id == bookletId }`.
- **Infrastructure** — `BookletJpaRepository.existsBookletForUser` backed by
  `SELECT COUNT(b) > 0 FROM BookletResource b WHERE b.idBooklet = :bookletId AND b.owner.idUser =
  :userId` (no fetch joins at all), implemented in `BookletJpaRepositoryAdapter`.
- `InMemoryBookletRepository` (domain test fake) implements the same method for domain-level tests.

This removes both the wasted work and the session-poisoning side effect at the source, without
touching `findBookletByIdWithTransactions` or the `transactions`/`regularTransactions` bag/set
mapping at all. (A unique-constraint-style fix wasn't applicable here — this isn't a race
condition, it's a deterministic cartesian product on every call.)

**Verification**: the previously-failing reproduction test
(`DeleteTransactionsByIdsUseCaseOwnershipSideEffectTest`) now passes, plus two new direct tests for
`existsBookletForUser` in `BookletJpaRepositoryAdapterTest`. Full backend suite
(`./gradlew test` — domain, infrastructure, application) green.

---

## Investigation log (kept for context — superseded by "Confirmed Root Cause" above)

## Correction (after initial report)

The user initially could not find the transaction row and, based on that, this report concluded a
row had likely been deleted after being duplicated by a concurrency race. The user has since
confirmed the row **does** exist in the database — it's a persisted preview transaction. This
invalidates the "duplicate created then one copy deleted" narrative as written below. The real
question is open again: **why does `DeleteTransactionsByIdsService` fail to find a transaction row
that demonstrably exists in `sheet`?**

Since `booklet.transactions` is loaded with no filter (`findByIdWithTransactions` — see below), a
row that exists in `sheet` but doesn't show up in `booklet.transactions` for the booklet ID sent by
the delete request means one of:

1. The transaction's `booklet_id` column does not match the `bookletID` sent in the delete request
   (wrong/stale booklet id on the client, or the row belongs to a different booklet than the one
   being viewed).
2. The `id` sent by the client for that row does not match the row's actual `id_sheet` value.

**Update — tested and refuted**: the user pointed at the `userOwnsBooklet(...)` check added to
`DeleteTransactionsByIdsService` in commit `743c22c8` as a possible cause. The hypothesis was:
`userOwnsBooklet` → `BookletRepository.findBookletsForUser` runs a query that
`LEFT JOIN FETCH`es **both** `b.transactions` and `b.regularTransactions` in one shot (see
`BookletJpaRepository.findAllBookletsByUserId`), inside the *same* Spring/Hibernate transaction as
the subsequent `findBookletByIdWithTransactions` call (confirmed: `UnitOfWorkPostgresSpringTransactionalAdapter.executeInTransaction`
is `@Transactional`, and both adapter methods are too, so they share one Hibernate session/entity
identity map). If that cartesian double-collection fetch left the `BookletResource.transactions`
bag incompletely populated for the entity already cached in the session, the later call would
silently reuse the incomplete collection instead of loading a correct one.

I wrote an infrastructure integration test
(`DeleteTransactionsByIdsUseCaseOwnershipSideEffectTest`, run against real Testcontainers Postgres,
not mocked) reproducing this exact sequence: booklet + one regular transaction linked to it + one
persisted preview transaction tied to that regular transaction, then
`DeleteTransactionsByIdsUseCase.handle(...)` for that transaction's id. **It passed** — the delete
succeeded (`ResultState.OK`), so this specific mechanism does not reproduce the bug, at least not
in this simple single-booklet/single-regular-transaction shape. The test was removed after running
it (it doesn't correspond to an actual fix, per the project's TDD discipline — tests accompany
confirmed fixes, not disproven hypotheses). **The `userOwnsBooklet` check is very likely not the
cause**, though a more complex account (several booklets, several regular transactions) has not
been tried and can't be fully ruled out without one.

**Next diagnostic step (still needed — from the user)**: `LoggingCommandBus`
([LoggingCommandBus.kt](../../../application/src/main/kotlin/fr/sacane/jmanager/application/bus/LoggingCommandBus.kt))
logs one INFO line per command dispatch, e.g.
`COMMAND | DeleteTransactionsByIdsCommand | TRANSACTION_NOT_FOUND | 4 ms`, and
`DeleteTransactionsByIdsCommand.mdcContext()` puts the exact `bookletID` used in that request into
the log line's `[booklet=...]` field. Finding that line in `app.log` around the time of the failed
delete, and comparing its `booklet=` value against the transaction row's actual `booklet_id` column
in `sheet`, will directly confirm or rule out hypothesis 1 above.

---

## Original hypothesis (kept for reference — not confirmed, see Correction above)

## Symptom

In production, deleting a previsional ("prévisionnelle") transaction from the booklet page fails
with `TRANSACTION_NOT_FOUND` (error code `1002`, surfaced client-side as "La transaction demandée
est introuvable" — see `client/utils/errorCodeMap.ts:41`). The user confirmed the transaction id
sent by the delete request genuinely does not exist in the database — this is not a stale display
that a page refresh would explain away.

## Investigation

Ruled out first, per the user's own hypothesis:

- **No server-side cache** sits on the read path. The only `@Cacheable` caches in the codebase are
  `allBooklets` (booklet list + balances), `allTags`, `defaultTag`, and `featureFlags`
  ([BookletJpaRepositoryAdapter.kt](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/BookletJpaRepositoryAdapter.kt),
  [UserRepositoryJpaAdapter.kt:46](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/UserRepositoryJpaAdapter.kt),
  [TagRepositoryJpaAdapter.kt](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/TagRepositoryJpaAdapter.kt),
  [FeatureFlagRepositoryJpaAdapter.kt](../../../infrastructure/src/main/kotlin/fr/sacane/jmanager/infrastructure/spi/adapters/FeatureFlagRepositoryJpaAdapter.kt)).
  None of them store transaction rows, and each request runs in its own transaction/persistence
  context, so a stale row surviving across requests via Hibernate's first-level cache is not
  possible either.

What the read path actually does, instead:

`LoadTransactionsForBookletForAMonthService.handle()`
([LoadTransactionsForBookletForAMonthUseCase.kt:118-127](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/booklet/LoadTransactionsForBookletForAMonthUseCase.kt))
— the handler behind `GET /transactions` — calls
`RegularTransactionGeneratorService.generateMissingPrevisionalTransactions()` **on every request**
whenever the requested period covers the current month. That method
([RegularTransactionComputer.kt:112-200](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/usecase/RegularTransactionComputer.kt))
is a **check-then-insert**: for each active regular transaction it queries whether a matching
preview transaction already exists for that occurrence
(`checkIfTransactionExists` → `shouldCreateTransaction`), and if not, creates and **persists** a
new preview `Transaction` row.

This check-then-insert has no concurrency guard:

- No unique constraint exists on the transaction table for `(regular_transaction_id, date)` —
  `sheet.regular_transaction_id` only has a plain index, added in
  [V6__add_regular_transaction_id_to_sheet.sql](../../../infrastructure/src/main/resources/db/migration/V6__add_regular_transaction_id_to_sheet.sql).
- No row lock or `INSERT ... ON CONFLICT` is used around the check + save.

**This is the same class of bug the codebase already fixed once, for a different table.**
[V11__add_unique_constraint_regular_transaction_tracker.sql](../../../infrastructure/src/main/resources/db/migration/V11__add_unique_constraint_regular_transaction_tracker.sql)
explicitly documents deduplicating `regular_transaction_tracker` rows and adding
`UNIQUE (regular_transaction_id, booklet_id)` *"to make upsert idempotent under concurrency"*. The
equivalent guard was never added for the transaction table itself, even though the same
read-triggered generation logic is exposed to the same kind of race: two `GET /transactions`
requests hitting the backend close together for the same booklet/current-month (two tabs open on
the same booklet, a slow request being retried, a double GET on page load) can each independently
find "no occurrence yet" and each persist their own preview transaction row for the same logical
occurrence — two distinct real ids for what the user perceives as one line.

## Root Cause (stated with the residual uncertainty noted)

The most likely explanation, backed by the evidence above: a duplicate preview transaction was
created by a race between two concurrent `GET /transactions` calls. The user (or another action —
e.g. deleting the duplicate from a second tab, or the "regenerate" flow) already removed one of the
two duplicate rows. The booklet page still held the now-deleted row's id in its in-memory
transaction list — displaying it as a normal previsional line — so clicking delete on it sent a
request for an id that had, correctly, already ceased to exist. The backend behaved correctly here
(`DeleteTransactionsByIdsService`, [DeleteTransactionsByIdsUseCase.kt:76](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/transaction/DeleteTransactionsByIdsUseCase.kt)),
returning `TRANSACTION_NOT_FOUND` as designed when a requested id isn't in
`booklet.transactions` — the bug is upstream, in how that id came to exist twice in the first
place.

**Not yet directly confirmed**: I have not queried the production database to see the actual
duplicate rows for this user's booklet/regular transaction. That single check would turn this from
a well-evidenced hypothesis into a certainty — run against the affected booklet:

```sql
SELECT regular_transaction_id, date, count(*)
FROM sheet
WHERE booklet_id = '<affected booklet id>'
  AND regular_transaction_id IS NOT NULL
GROUP BY regular_transaction_id, date
HAVING count(*) > 1;
```

## Proposed Fix (not applied — multi-layer change, needs your go-ahead)

1. **Infrastructure**: new Flyway migration adding a unique constraint on the transaction table
   scoped to `(regular_transaction_id, date)` (mirroring the `V11` migration for the tracker
   table), after deduplicating any existing duplicate rows.
2. **Domain**: make `generateMissingPrevisionalTransactions` tolerate the constraint violation
   (catch and skip, the same way the tracker upsert became idempotent) instead of assuming the
   check-then-insert is race-free.
3. Also worth reconsidering separately (flagged, not blocking this fix): a **query** handler
   (`GET /transactions`) silently **persisting new rows** as a side effect is a CQRS violation in a
   codebase that otherwise separates commands and queries cleanly — worth a dedicated design
   discussion, not bundled into this bug fix.

I have not applied any of this — this is a multi-file, cross-layer change (new migration +
domain-service behaviour), so per the investigation protocol I'm stopping here for your decision
rather than fixing it unprompted. Let me know if you'd like me to implement it (and whether you'd
like to run the verification query above first).

## Follow-up

- Backlog item also raised during this investigation, unrelated to this bug:
  [docs/backlog/retention-purge-fails-on-linked-regular-transactions.md](../../backlog/retention-purge-fails-on-linked-regular-transactions.md)
  — the nightly GDPR/retention purge job fails on booklets that still have regular transactions
  linked to them.
