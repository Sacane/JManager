# Regenerable-transaction endpoints do not accept a date range

**Observation**
`GET /api/booklet/{bookletID}/transactions/regenerable` and
`POST /api/booklet/{bookletID}/transactions/regenerate` only accept `month` and `year`, while
`/transactions`, `/balances` and `/report` all accept optional `startDate` and `endDate`.

**Exact location**
- `application/src/main/kotlin/fr/sacane/jmanager/application/api/booklet/Controller.kt`,
  `findRegenerableTransactions` (~line 193) and `regenerateDeletedPrevisionalTransactions` (~line 213)
- `client/composables/useBooklet.ts`, `findRegenerableTransactions` and
  `regenerateDeletedPrevisionalTransactions` — no `BookletDateRangeQuery` parameter

**Expected behaviour**
The two endpoints accept the same optional date range as the other booklet queries, so the
regeneration can operate on whatever period the booklet page is displaying.

**Impact**
Medium. UX-14 lets the user look at an arbitrary date range, and every figure on the page follows
it — except regeneration, which would silently work on the calendar month instead. The client
therefore hides the action while a custom range is active. The feature is not broken, but it is
unavailable in a state the user can now reach, with no way to tell them why beyond a tooltip.

**Found while**
Implementing UX-14 (date range filtering on the booklet detail page), whose constraint was that
every displayed figure must describe the selected period.
