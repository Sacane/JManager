# Server-Side Pagination for Transaction Tables: Application Module Impact

**Context**
The application layer must expose pagination parameters (`page`, `size`) on the booklet transaction and regular transaction API endpoints, and return paginated responses with metadata. The page size preference is user-configurable from the frontend and persisted in the browser's localStorage, so the API simply receives the requested page/size as query parameters. Balances (realSold / previsionalSold) in the booklet report/transactions endpoints must remain computed on the full transaction set and are unaffected by pagination.

**Acceptance Criteria**
Feature: Paginated API endpoints for transaction tables
    In order to deliver paginated transaction data to the frontend
    As the application layer
    I want to accept page and size query parameters and return paginated responses

    Scenario: 1 — Get booklet transactions with pagination parameters
    Given an authenticated user who owns a booklet with 25 transactions in January 2025
    When calling GET /api/booklet/{bookletID}/transactions?month=1&year=2025&page=0&size=10
    Then the response status is 200
    And the response body contains exactly 10 transactions
    And the response includes pagination metadata (pageNumber=0, pageSize=10, totalElements=25, totalPages=3)
    And realSold and previewSold are computed on all 25 transactions

    Scenario: 2 — Get booklet transactions without pagination parameters uses defaults
    Given an authenticated user who owns a booklet with transactions
    When calling GET /api/booklet/{bookletID}/transactions?month=1&year=2025 without page or size
    Then the response uses default page=0, size=10
    And returns the first page of transactions with pagination metadata

    Scenario: 3 — Get booklet report with pagination parameters
    Given an authenticated user who owns a booklet with 25 transactions in January 2025
    When calling GET /api/booklet/report/{bookletID}?month=1&year=2025&page=1&size=10
    Then the response contains 10 transactions (page 1)
    And the response includes pagination metadata
    And realSold and previewSold reflect the full month totals

    Scenario: 4 — Get regular transactions with pagination
    Given an authenticated user with 35 regular transactions
    When calling GET /api/transaction/regular?page=0&size=10
    Then the response status is 200
    And the response body contains a paginated result with 10 regular transactions
    And the pagination metadata shows totalElements=35, totalPages=4

    Scenario: 5 — Get regular transactions without pagination parameters uses defaults
    Given an authenticated user with regular transactions
    When calling GET /api/transaction/regular without page or size
    Then the response uses default page=0, size=10

    Scenario: 6 — Get booklet transactions with out-of-range page
    Given an authenticated user who owns a booklet with 5 transactions
    When calling GET /api/booklet/{bookletID}/transactions?month=1&year=2025&page=10&size=10
    Then the response status is 200
    And the response body contains an empty transaction list with totalElements=5

**Notes**
- Add optional `@RequestParam("page", required = false, defaultValue = "0")` and `@RequestParam("size", required = false, defaultValue = "10")` to `BookletController.findBookletTransactionsByMonthAndYear`, `BookletController.findBookletReportByIdMonthAndYear`, and `TransactionController.getAllRegularTransactions`.
- `BookletTransactionsResponse` and `BookletReport` must be extended to include pagination fields (`pageNumber`, `pageSize`, `totalElements`, `totalPages`).
- The regular transactions endpoint response must switch from `List<RegularTransactionDTO>` to a paginated wrapper (e.g. `PageDTO<RegularTransactionDTO>`).
- Allowed page size values from the frontend: 10, 20, 30, 50. No server-side enforcement is needed, but the default is 10.
