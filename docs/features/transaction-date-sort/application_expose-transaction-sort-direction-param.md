# Application Module — Expose Transaction Sort Direction On Booklet Transactions Endpoint

**Context**
`GET /booklet/{bookletID}/transactions` (see `findBookletTransactionsByMonthAndYear` in `Controller.kt`) already accepts `page` and `size` query parameters but has no way to request a sort direction, so it always dispatches `LoadTransactionsForBookletForAMonthQuery` with the domain's default ordering. Once the domain query supports a `sortDirection` parameter (see the domain issue for this feature), the endpoint must expose it so the client can request a globally ordered, paginated transaction list instead of reordering only the currently loaded page.

**Acceptance Criteria**
```gherkin
Feature: Expose sort direction on the booklet transactions endpoint
  In order to let the client request a globally ordered, paginated transaction list
  As an API consumer
  I want to pass a sortDirection query parameter to GET /booklet/{bookletID}/transactions

Scenario: Request transactions sorted by most recent date first
  Given an authenticated user with transactions in a booklet for a given month
  When the client calls GET /booklet/{bookletID}/transactions with sortDirection=DESCENDING, a page, and a size
  Then the endpoint forwards DESCENDING as the sort direction to LoadTransactionsForBookletForAMonthQuery
  And the response contains the requested page ordered accordingly

Scenario: Request transactions without specifying a sort direction
  Given an authenticated user with confirmed and previsional transactions in a booklet for a given month
  When the client calls GET /booklet/{bookletID}/transactions without a sortDirection parameter
  Then the endpoint forwards no sort direction and returns confirmed transactions before previsional ones, preserving current behaviour

Scenario: Reject an invalid sort direction value
  Given an authenticated user
  When the client calls GET /booklet/{bookletID}/transactions with sortDirection=INVALID
  Then the endpoint returns a 400 Bad Request response
```

**Notes**
- Add `@RequestParam(required = false) sortDirection: TransactionSortDirection?` to `findBookletTransactionsByMonthAndYear`. Binding directly to the domain enum makes Spring answer an unknown value with a `MethodArgumentTypeMismatchException`, which `ProblemDetailHandler` already maps to 400 — no extra validation code.
- The response must be built from `BookletLoadingResult.orderedTransactions`, not from `currentTransactions + previsionalTransactions`: that concatenation regroups confirmed transactions before previsional ones and silently discards the ordering computed by the domain.
- Depends on the domain issue `domain_sort-transactions-by-date-across-pages.md` for `TransactionSortDirection` and the query parameter it introduces.
- Source card: [Trello #95 — triage des trans](https://trello.com/c/6WOsEI16/95-triage-des-trans).
- Related issue: `client_sort-transactions-by-date-across-pages.md`.
