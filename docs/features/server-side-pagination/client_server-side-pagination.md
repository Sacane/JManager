# Server-Side Pagination for Transaction Tables: Client Module Impact

**Context**
The booklet detail page (`booklet/{id}`) and regular transactions page (`regular-transaction`) currently load and display all transactions at once. With the backend now providing paginated responses, the frontend must send pagination parameters (`page`, `size`) in API calls, render pagination controls (page navigation + configurable page size selector), and persist the user's page size preference in `localStorage` so it survives browser sessions. The available page sizes are **10, 20, 30, 50** with a default of **10**.

**Acceptance Criteria**
Feature: Client-side pagination controls for transaction tables
    In order to navigate large transaction lists efficiently
    As an authenticated user
    I want pagination controls on transaction tables with a configurable page size saved in my browser

    Scenario: 1 — Booklet transactions table displays paginated data with controls
    Given an authenticated user viewing a booklet detail page with more than 10 transactions
    When the page loads
    Then the transaction table shows only the first 10 transactions (default page size)
    And a pagination bar is visible below the table with page navigation buttons
    And a page size selector displays the options 10, 20, 30, 50

    Scenario: 2 — User navigates to a different page in the booklet transactions table
    Given an authenticated user viewing a booklet with 25 transactions and page size 10
    When the user clicks on page 2 in the pagination bar
    Then the table refreshes with transactions 11-20 fetched from the server
    And the balances (realSold / previsionalSold) remain unchanged (full-period values)

    Scenario: 3 — User changes the page size on the booklet transactions table
    Given an authenticated user viewing a booklet detail page
    When the user selects page size 30 from the page size selector
    Then the table refreshes showing up to 30 transactions per page
    And the selected page size is saved to localStorage
    And the current page resets to page 1

    Scenario: 4 — Page size preference is restored from localStorage on page load
    Given an authenticated user who previously set page size to 20 on the booklet transactions table
    When the user navigates to a booklet detail page
    Then the page size selector shows 20 as selected
    And the table loads with page size 20

    Scenario: 5 — Regular transactions table displays paginated data with controls
    Given an authenticated user viewing the regular transactions page with more than 10 regular transactions
    When the page loads
    Then the table shows only the first 10 regular transactions (default page size)
    And a pagination bar and page size selector are visible

    Scenario: 6 — User navigates pages in the regular transactions table
    Given an authenticated user with 35 regular transactions and page size 10
    When the user clicks on page 3
    Then the table refreshes with regular transactions 21-30 fetched from the server

    Scenario: 7 — User changes page size on the regular transactions table
    Given an authenticated user on the regular transactions page
    When the user selects page size 50
    Then the table refreshes with up to 50 regular transactions
    And the preference is persisted in localStorage
    And the current page resets to page 1

    Scenario: 8 — Page size localStorage keys are separate per table
    Given an authenticated user who set page size 20 on booklet transactions and 50 on regular transactions
    When the user navigates between the two pages
    Then each table restores its own saved page size independently

    Scenario: 9 — No pagination controls when total items fit in one page
    Given an authenticated user viewing a booklet with 5 transactions and page size 10
    When the page loads
    Then all 5 transactions are displayed
    And pagination navigation buttons are hidden or disabled (only one page)

**Notes**
- Use `localStorage` keys like `jmanager.pagination.bookletTransactions.pageSize` and `jmanager.pagination.regularTransactions.pageSize`.
- The `useBooklet` composable methods (`findTransactionsByIdMonthAndYear`, `findByIdMonthAndYear`) must accept `page` and `size` parameters and pass them to the API.
- The `useRegularTransaction` composable's `getRegularTransaction` method must accept `page` and `size` parameters.
- The existing `PageDTO<T>` type from `useAdmin.ts` should be reused or extracted into a shared type.
- The `AppTable` component does not need built-in pagination; use PrimeVue's `Paginator` component alongside `AppTable`, similar to the admin page pattern.
- The `booklet/[id].vue` page's `filteredTransactions` computed property must be adjusted to work with the paginated data from the server (client-side filters like tag/preview filter should be applied before or in conjunction with server-side pagination — clarify with the team if filters should also be pushed server-side in a future iteration).
