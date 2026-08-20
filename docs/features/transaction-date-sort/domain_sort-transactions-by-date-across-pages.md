# Domain Module — Sort Booklet Transactions By Date Across All Pages

**Context**
`LoadTransactionsForBookletForAMonthQuery` builds the full in-memory list of transactions for a period (`allDisplayTransactions`) and slices it into pages via `Paginator.paginate`, but nothing in the query lets a caller choose the ordering used before that slicing happens. The list is implicitly ordered the way `TransactionQueryRepository.findByBookletIdAndDateBetween` returns it (ascending by date, then by last-modified timestamp), with no way to request the reverse order.

Today, on the booklet page, the "Date" column is sorted client-side by the frontend, on whichever page is currently loaded. Because pagination is server-side, this only ever reorders the rows already in memory — it cannot surface the true oldest/most recent transaction of the whole period once there is more than one page. Fixing this requires the domain query itself to accept a sort direction and apply it to the complete dataset *before* pagination, so that any requested page reflects the correct global order.

**Acceptance Criteria**
```gherkin
Feature: Sort booklet transactions by date across all pages
  In order to browse all transactions of a period from the most relevant date first, regardless of pagination
  As an authenticated user
  I want to request booklet transactions ordered by ascending or descending date before they are paginated

Scenario: Load transactions ordered by most recent date first
  Given an authenticated user with 15 transactions spread across a booklet for a given month, split into pages of 10
  When the user loads transactions for that booklet, month, and year with sortDirection DESCENDING, pageNumber 1, and pageSize 10
  Then the system returns the 5 remaining transactions ordered from most recent to oldest
  And that order reflects the global date order across the whole period, not just the transactions on that page

Scenario: Load transactions ordered by oldest date first
  Given an authenticated user with 15 transactions spread across a booklet for a given month, split into pages of 10
  When the user loads transactions for that booklet, month, and year with sortDirection ASCENDING, pageNumber 1, and pageSize 10
  Then the system returns the 5 remaining transactions ordered from oldest to most recent
  And that order reflects the global date order across the whole period, not just the transactions on that page

Scenario: Omitting the sort direction preserves existing behaviour
  Given an authenticated user with a confirmed transaction dated later than a previsional one, in a booklet for a given month
  When the user loads transactions without specifying a sort direction
  Then the system returns confirmed transactions first, then previsional ones, exactly as before
```

**Notes**
- Introduce a `TransactionSortDirection` enum (`ASCENDING` / `DESCENDING`) and add it as a nullable parameter of `LoadTransactionsForBookletForAMonthQuery`, defaulting to `null`.
- `null` (no explicit sort) must keep the current display order — confirmed transactions first, then previsional/virtual ones. An `ASCENDING` default would *not* have been backward compatible: today the two groups are concatenated, not interleaved by date, so a global date sort changes the order whenever a confirmed transaction is dated after a previsional one.
- Apply the sort to `allDisplayTransactions` (the merged list of confirmed + previsional/virtual transactions) in `LoadTransactionsForBookletForAMonthService`, before the call to `paginator.paginate`, so both categories stay consistently ordered together.
- Source card: [Trello #95 — triage des trans](https://trello.com/c/6WOsEI16/95-triage-des-trans).
- Related issues: `application_expose-transaction-sort-direction-param.md`, `client_sort-transactions-by-date-across-pages.md`.
