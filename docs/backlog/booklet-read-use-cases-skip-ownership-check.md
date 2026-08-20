# Booklet read use cases do not verify ownership

**Observation**

`userOwnsBooklet` exists in `BookletDomainHelper.kt` and is applied by the mutating booklet use cases
(`DeleteBookletByIdUseCase`, `EditBookletUseCase`) and, since the selective-regeneration work, by
`FindRegenerableTransactionsUseCase` and `RegenerateDeletedPrevisionalTransactionsUseCase`.

The remaining booklet read use cases resolve the booklet by id alone, with no ownership check. They
mostly return nothing useful for a foreign booklet (downstream repository calls filter by
`userId`), but they still answer differently for "booklet exists but belongs to someone else"
(`200` with empty/partial data) than for "booklet does not exist" (`404`).

**Exact location**

`domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/booklet/`
- `FindBookletByIdUseCase.kt` — `findBookletByIdWithTransactions(query.bookletId)` with no owner check
- `LoadTransactionsForBookletForAMonthUseCase.kt`
- `LoadBalancesForBookletForAMonthUseCase.kt`

**Expected behaviour**

Apply `userOwnsBooklet(bookletRepository, userId, bookletId)` at the top of each of these use cases
and return `ResultState.BOOKLET_NOT_FOUND` when it fails — reporting a foreign booklet as missing
rather than forbidden, consistent with what the four use cases listed above already do.

**Impact**

- Booklet-id enumeration oracle: an authenticated user can distinguish an existing booklet belonging
  to another user from a non-existent id by comparing status codes. Low severity on its own (no data
  is returned), but it is a disclosure primitive and the inconsistency across sibling use cases makes
  the intended contract unclear.
- `FindBookletByIdUseCase` deserves the closest look — verify whether it can actually return another
  user's booklet content, in which case the severity is higher than enumeration alone.

Noted while adding the ownership check to the regeneration use cases; the rest of the cluster was
left untouched to keep that change scoped.
