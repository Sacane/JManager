# Bug: Transaction Regeneration — Distinguish Previsional vs Virtual (Client)

**Context**
The booklet detail page (`/booklet/[id].vue`) displays a "Regenerate" button when `hasRegenerableTransactions == true`. This button calls `regenerateDeletedPrevisionalTransactions` in `useBooklet.ts`, which posts to `POST /api/booklet/{id}/transactions/regenerate`.

Following the domain and API fixes, the endpoint now distinguishes:
- **Current month**: returns previsional transactions (`type: PREVISIONAL`).
- **Future month**: returns virtual transactions (`type: VIRTUAL`).
- **Past month**: `hasRegenerableTransactions` will always be `false`, so the button is never shown.

The frontend must adapt its behavior and UX based on the displayed month type:
- **Current month**: existing behavior — reload the view after previsional regeneration.
- **Future month**: same reload, sufficient since virtual transactions will be included by `loadTransactionsForBookletForAMonth` once the exclusion is lifted.
- The tooltip label and the success toast message must reflect the distinction.

**Acceptance Criteria**

Feature: UI regenerate button adapted to the month type

Scenario: Regenerate button is shown for the current month when an exclusion exists
    Given the user is viewing a booklet for the current month
    And at least one regular transaction is excluded for that month
    When the page is loaded
    Then the Regenerate button is visible
    And its tooltip indicates that deleted previsional transactions can be regenerated

Scenario: Clicking Regenerate for the current month reloads the view with previsional transactions
    Given the user is viewing a booklet for the current month
    And the Regenerate button is displayed
    When the user clicks the Regenerate button
    Then the POST regenerate call is made
    And a success toast indicates that previsional transactions have been regenerated
    And the view is reloaded and shows the new previsional transactions

Scenario: Regenerate button is shown for a future month when an exclusion exists
    Given the user is viewing a booklet for a future month
    And at least one regular transaction is excluded for that future month
    When the page is loaded
    Then the Regenerate button is visible
    And its tooltip indicates that deleted virtual transactions can be restored

Scenario: Clicking Regenerate for a future month reloads the view with virtual transactions
    Given the user is viewing a booklet for a future month
    And the Regenerate button is displayed
    When the user clicks the Regenerate button
    Then the POST regenerate call is made
    And a success toast indicates that virtual transactions have been restored
    And the view is reloaded and shows the virtual transactions

Scenario: Regenerate button is absent for a past month
    Given the user is viewing a booklet for a past month
    When the page is loaded
    Then the Regenerate button is not displayed because hasRegenerableTransactions is false

**Notes**
- Update `useBooklet.ts`: `regenerateDeletedPrevisionalTransactions` must return the new `RegenerateTransactionsResponse` (including the `type` field).
- In `[id].vue`, use the returned `type` or compare the current month to adapt the tooltip and toast message.
- The `regenerate()` function can determine whether the current month is selected by comparing `bookletData.month` and `bookletData.year` against `new Date()`.
- Ensure page tests in `tests/pages/` cover both scenarios (current month and future month).
