# Client Module — Sort Booklet Transactions By Date Across All Pages

**Context**
On the booklet transactions page (`pages/booklet/[id].vue`), the "Date" column is declared `sortable: true` on the `AppTable`/PrimeVue `DataTable`, which sorts the `rows` prop client-side, in memory. Since transactions are paginated server-side (`loadBookletData` fetches one page at a time via `onPageChange`), that in-memory sort only ever reorders the transactions of the page currently loaded — it cannot bring the oldest/most recent transaction of the whole period into view once there is more than one page.

The domain and application layers now expose a `sortDirection` parameter on the transactions query (see the corresponding domain and application issues for this feature). The client must stop relying on the DataTable's built-in client-side sort for the "Date" column and instead drive sorting through a backend reload: clicking the column header should reset to page 1 and reload the booklet data with the matching sort direction.

**Acceptance Criteria**
```gherkin
Feature: Sort booklet transactions by date across all pages
  In order to see the most recent or oldest transaction of the whole period, not just the current page
  As an authenticated user viewing a booklet's transactions
  I want clicking the "Date" column header to reorder the transaction list globally and reload from the first page

Scenario: Sort transactions by most recent date first
  Given the user is viewing a booklet's transactions with multiple pages of data
  When the user clicks the "Date" column header to sort by most recent first
  Then the table reloads starting from page 1
  And the displayed transactions are ordered from most recent to oldest across the whole period, not only the loaded page

Scenario: Sort transactions by oldest date first
  Given the user is viewing a booklet's transactions currently sorted by most recent first
  When the user clicks the "Date" column header again to sort by oldest first
  Then the table reloads starting from page 1
  And the displayed transactions are ordered from oldest to most recent across the whole period, not only the loaded page

Scenario: Sort direction is preserved across navigation actions
  Given the user has sorted transactions by most recent date first
  When the user changes page, month, or year
  Then the transaction list keeps loading with the currently selected sort direction until the user changes it again
```

**Notes**
- Depends on the application issue `application_expose-transaction-sort-direction-param.md` for the `sortDirection` query parameter.
- The "Date" column should no longer use PrimeVue's built-in client-side `sortable` behaviour for this field; replace it with an explicit click handler that updates a `sortDirection` ref, resets `currentPage` to 0, and calls `loadBookletData()`.
- Reuse the existing composable that fetches booklet transactions rather than duplicating the request logic — add `sortDirection` to its parameters.
- Source card: [Trello #95 — triage des trans](https://trello.com/c/6WOsEI16/95-triage-des-trans).
