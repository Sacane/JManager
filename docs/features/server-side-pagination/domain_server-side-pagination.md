# Server-Side Pagination for Transaction Tables: Domain Module Impact

**Context**
Users with many transactions experience slow loading on the booklet detail page (`booklet/{id}`) and the regular transactions page (`regular-transaction`). To improve performance and UX, server-side pagination must be introduced for both transaction listings. The domain layer must support pagination parameters (`pageNumber`, `pageSize`) in the relevant feature ports and return paginated results using the existing `Page<T>` model. Balances (realSold / previsionalSold) must remain computed on the **full** set of transactions for a given period, independently of the requested page.

The existing `Paginator` domain service and `Page<T>` model already handle in-memory pagination for the admin user list and can be reused.

**Acceptance Criteria**
Feature: Paginated transaction loading in domain layer
    In order to support server-side pagination for transaction tables
    As a domain consumer (application layer)
    I want to request a specific page of transactions while preserving full-period balance calculations

    Scenario: 1 — Load a specific page of booklet transactions for a month
    Given an authenticated user who owns a booklet with 25 transactions in January 2025
    When the user loads transactions for booklet ID, month January, year 2025 with pageNumber 0 and pageSize 10
    Then the system returns a BookletLoadingResult containing only 10 transactions for the requested page
    And the result includes pagination metadata (pageNumber=0, pageSize=10, totalElements=25, totalPages=3)
    And the realSold and previsionalSold are computed on ALL 25 transactions, not just the page

    Scenario: 2 — Load the last page with fewer items than page size
    Given an authenticated user who owns a booklet with 25 transactions in January 2025
    When the user loads transactions with pageNumber 2 and pageSize 10
    Then the system returns a BookletLoadingResult containing exactly 5 transactions
    And the pagination metadata shows totalElements=25 and totalPages=3

    Scenario: 3 — Load booklet transactions with default pagination when no page parameters provided
    Given an authenticated user who owns a booklet with transactions
    When the user loads transactions without specifying pageNumber or pageSize
    Then the system uses default values pageNumber=0 and pageSize=10
    And returns the first page of transactions

    Scenario: 4 — Load booklet transactions for an out-of-range page
    Given an authenticated user who owns a booklet with 5 transactions in January 2025
    When the user loads transactions with pageNumber 5 and pageSize 10
    Then the system returns an empty transaction list with correct totalElements=5

    Scenario: 5 — Retrieve a paginated list of regular transactions
    Given an authenticated user with 35 regular transactions
    When the user requests regular transactions with pageNumber 0 and pageSize 10
    Then the system returns a Page containing 10 regular transactions
    And the pagination metadata shows totalElements=35 and totalPages=4

    Scenario: 6 — Retrieve regular transactions with default pagination
    Given an authenticated user with regular transactions
    When the user requests regular transactions without specifying pageNumber or pageSize
    Then the system uses default values pageNumber=0 and pageSize=10

**Notes**
- The `BookletFeature.loadTransactionsForBookletForAMonth` signature must accept optional `pageNumber: Int?` and `pageSize: Int?` parameters (defaulting to 0 and 10).
- The `BookletLoadingResult` must be extended with pagination metadata fields (pageNumber, pageSize, totalElements, totalPages) or wrapped in a `Page<T>`-compatible structure.
- The `RegularTransactionFeature.getAllRegularTransactions` signature must accept optional `pageNumber: Int?` and `pageSize: Int?` parameters.
- The existing `Paginator` domain service should be reused for in-memory pagination after the full transaction list is assembled.
- Balance calculations (realSold, previsionalSold) MUST operate on the full transaction set before pagination is applied.
- The `BookletLoadingResult.currentTransactions` and `previsionalTransactions` lists should represent the paginated view of the combined transactions.
