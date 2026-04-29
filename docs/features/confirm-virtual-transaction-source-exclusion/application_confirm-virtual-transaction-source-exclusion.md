# Confirm Virtual Transaction: Expose Dedicated Endpoint — Application Module

**Context**
When a user confirms a virtual transaction from month M (e.g. May) and optionally changes its date to month N (e.g. April), the source month M must be marked as excluded in the tracker. A new domain use case `ConfirmVirtualTransactionUseCase` handles this atomically; the application layer must expose a dedicated REST endpoint to invoke it.

The endpoint replaces the current client-side workaround of calling `saveTransaction` directly for virtual transactions, which only persists the transaction but never excludes the source month.

**Acceptance Criteria**
Feature: POST /transactions/virtual/confirm endpoint
    In order to atomically confirm and exclude a virtual transaction
    As an authenticated user
    I want a dedicated endpoint that saves the transaction and marks the source month as excluded

    Scenario: 1 - Confirm a virtual transaction happy path
    Given an authenticated user with a booklet and a regular transaction A
    When calling POST /transactions/virtual/confirm with bookletId, regularTransactionId, sourceMonth, sourceYear, label, amount, date, and isIncome
    Then the system returns HTTP 200
    And the response body contains the saved transaction data and updated booklet balance

    Scenario: 2 - Confirm a virtual transaction with date in a different month
    Given an authenticated user viewing May 2026 with a virtual transaction for regular transaction A
    When calling POST /transactions/virtual/confirm with sourceMonth=5, sourceYear=2026 and a date in April 2026
    Then the system returns HTTP 200
    And May 2026 is marked as excluded in the tracker verified via subsequent GET for May
    And the transaction appears with the April date

    Scenario: 3 - Booklet not found
    Given an authenticated user providing an unknown booklet ID
    When calling POST /transactions/virtual/confirm
    Then the system returns HTTP 404

    Scenario: 4 - Missing required fields
    Given an authenticated user
    When calling POST /transactions/virtual/confirm with a missing required field
    Then the system returns HTTP 400

**Notes**
- Request DTO fields: `bookletId` (String), `regularTransactionId` (String), `sourceMonth` (Int), `sourceYear` (Int), `label` (String), `amount` (BigDecimal), `date` (LocalDate), `isIncome` (Boolean), `tagId` (String?).
- Response DTO: reuse `TransactionResultDTO`.
- The endpoint must be authenticated (existing security filter chain applies).
- No change to the existing `ConfirmPreviewTransactionUseCase` or DELETE endpoint.
