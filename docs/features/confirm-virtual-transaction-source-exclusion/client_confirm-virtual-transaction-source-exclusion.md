# Confirm Virtual Transaction: Fix Source Month Exclusion — Client Module

**Context**
When a user confirms a virtual transaction from month M (e.g. May) and changes its date to month N (e.g. April), the virtual transaction for May keeps reappearing because the source month is never marked as excluded. The root cause is that the frontend calls `saveTransaction` directly for virtual transactions (which have no ID), without ever triggering the exclusion of the source month in the tracker.

The fix is to call the new `POST /transactions/virtual/confirm` endpoint instead of `saveTransaction` when confirming a virtual transaction. This endpoint handles both the save and the source-month exclusion atomically on the backend.

**Acceptance Criteria**
Feature: Confirm virtual transaction — client-side source month exclusion
    In order to avoid a confirmed virtual transaction reappearing in its source month
    As a user on the booklet page
    I want the confirm flow to call the dedicated virtual-confirm endpoint that excludes the source month

    Scenario: 1 - Confirm virtual transaction with same-month date
    Given the user is viewing booklet page for May 2026
    And a virtual transaction for regular transaction A is visible
    When the user opens the confirm dialog and submits without changing the date
    Then a POST to /transactions/virtual/confirm is made with sourceMonth=5 and sourceYear=2026
    And the virtual transaction disappears from the May view after reload

    Scenario: 2 - Confirm virtual transaction with date changed to a different month
    Given the user is viewing booklet page for May 2026
    And a virtual transaction for regular transaction A is visible
    When the user opens the confirm dialog and changes the date to April 2026
    Then a POST to /transactions/virtual/confirm is made with sourceMonth=5 sourceYear=2026 and the April date
    And May 2026 no longer shows the virtual transaction for A after reload
    And April 2026 shows the newly saved real transaction

    Scenario: 3 - Confirm persisted preview transaction leaves existing behaviour unchanged
    Given the user is viewing booklet page for April 2026
    And a persisted preview transaction with an ID exists
    When the user confirms it
    Then the existing confirmPreviewTransaction flow is used
    And the source month exclusion endpoint is NOT called

**Notes**
- The distinction between virtual and persisted-preview is `!transaction.id`: virtual transactions have no `id`.
- The source month must be derived from the currently viewed month: `bookletData.month` + `bookletData.year`.
- Add a `confirmVirtualTransaction(bookletId, regularTransactionId, sourceMonth, sourceYear, label, amount, date, isIncome, tagId)` function to `useTransaction` composable and call it in `confirmPreview()` inside `[id].vue`.
- Update the `booklet-id.spec.ts` test file to cover the new call and ensure the existing preview-confirm tests remain green.
