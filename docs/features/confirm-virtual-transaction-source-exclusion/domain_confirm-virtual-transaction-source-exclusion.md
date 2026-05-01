# Confirm Virtual Transaction: Exclude Source Month in Tracker — Domain Module

**Context**
When a user confirms a virtual transaction from month M (e.g. May) and optionally changes its date to month N (e.g. April), the source month M must be marked as excluded in the `RegularTransactionTracker` regardless of the final transaction date. Currently no domain use case handles this as an atomic operation, causing May to keep re-generating the virtual transaction even though it was already confirmed.

A new `ConfirmVirtualTransactionUseCase` must be introduced to atomically:
1. Persist the transaction as a real (non-preview) transaction using the provided amount and date.
2. Mark the **source month** (the month from which the virtual was confirmed) as excluded in the tracker linked to the regular transaction.

**Acceptance Criteria**
Feature: Confirm virtual transaction and exclude source month
    In order to prevent a confirmed virtual transaction from reappearing in its source month
    As an authenticated user
    I want the source month to be marked as excluded when I confirm a virtual transaction

    Scenario: 1 - Confirm virtual transaction with unchanged date
    Given an authenticated user with a booklet linked to a regular transaction A
    And May 2026 is the source month from which the virtual transaction is being confirmed
    And the confirmed date also falls in May 2026
    When the user confirms the virtual transaction with the same date and amount
    Then a new real (isPreview = false) transaction is persisted in the booklet
    And May 2026 is marked as excluded in the tracker for regular transaction A

    Scenario: 2 - Confirm virtual transaction with date changed to a different month
    Given an authenticated user with a booklet linked to a regular transaction A
    And May 2026 is the source month from which the virtual transaction is being confirmed
    And the user provides April 2026 as the new date
    When the user confirms the virtual transaction
    Then a new real (isPreview = false) transaction is persisted with the April 2026 date
    And May 2026 is marked as excluded in the tracker for regular transaction A
    And April 2026 is NOT marked as excluded in the tracker

    Scenario: 3 - Booklet not found
    Given an authenticated user providing a non-existent booklet ID
    When the user attempts to confirm a virtual transaction
    Then the system returns a BOOKLET_NOT_FOUND failure

    Scenario: 4 - Regular transaction tracker not found for the given booklet
    Given an authenticated user with a valid booklet
    And the regular transaction has no tracker entry for that booklet
    When the user attempts to confirm a virtual transaction
    Then a new tracker is created and the source month is marked as excluded
    And the real transaction is persisted

**Notes**
- The new `ConfirmVirtualTransactionCommand` must carry: `token`, `bookletId`, `regularTransactionId`, `sourceMonth`, `sourceYear`, `label`, `amount`, `date`, `isIncome`, and optional `tagId`.
- This use case is distinct from `ConfirmPreviewTransactionUseCase` which operates on persisted (preview) transactions that already have a UUID.
- Domain tests must use in-memory fakes only; no infrastructure dependencies allowed.
- The save operation and the tracker exclusion must be executed inside the same unit-of-work transaction to guarantee atomicity.
