# Dashboard Monthly Cycle Range — Wrong Period Displayed for Target Month : Client Module

## Context

When a booklet is configured with a monthly cycle that does not start on the 1st (e.g. start day = 27, end day = 26),
the dashboard displays an incorrect date range for a given calendar month view.

**Root cause**: `currentDateRange` in `pages/dashboard/index.vue` passes `periodAnchorDate` directly to
`resolveMonthlyCycleRangeFromAnchor`. When the user navigates backwards from a date that falls *after* the
configured cycle start day (e.g. navigating from April 29 → March 29), the anchor day (29) is ≥ the cycle
start day (27), so the function returns the *next* cycle (Mar 27 → Apr 26) instead of the expected one
(Feb 27 → Mar 26).

**Existing solution available**: `resolveMonthlyCycleRangeForTargetMonth` already exists in
`client/utils/monthlyCycleRange.ts` and uses day 15 as a safe anchor to reliably resolve the cycle for a
given calendar month. The dashboard must use this function for the month view instead of
`resolveMonthlyCycleRangeFromAnchor(periodAnchorDate, ...)`.

**Example scenario**

| Configuration | Navigated to | Currently shown | Expected |
|---|---|---|---|
| start=27, end=26 | March 2026 (anchor = March 29) | 27 Mar – 26 Apr 2026 | 27 Feb – 26 Mar 2026 |

## Acceptance Criteria

```gherkin
Feature: Dashboard displays the correct monthly cycle period for the selected calendar month

  Background:
    Given the user is authenticated
    And a booklet exists

  Scenario: Cycle starting in the second half of the month — navigating backward from a late anchor date
    Given the booklet is configured with a monthly cycle starting on day 27 and ending on day 26
    And the current date is April 29, 2026
    When the user navigates to the March 2026 view on the dashboard
    Then the displayed period is "27 fév. - 26 mars 2026"
    And the period label shows "mars 2026"

  Scenario: Cycle starting in the second half of the month — initial view on current month
    Given the booklet is configured with a monthly cycle starting on day 27 and ending on day 26
    And today is March 10, 2026
    When the user opens the dashboard
    Then the displayed period is "27 fév. - 26 mars 2026"

  Scenario: Standard cycle starting on the 1st is unaffected
    Given the booklet is configured with the default cycle starting on day 1
    When the user navigates to the March 2026 view on the dashboard
    Then the displayed period is "1 mars - 31 mars 2026"

  Scenario: Cycle starting in the first half of the month uses the cycle that begins in the target month
    Given the booklet is configured with a monthly cycle starting on day 5 and ending on day 4
    When the user navigates to the March 2026 view on the dashboard
    Then the displayed period is "5 mars - 4 avr. 2026"

  Scenario: Navigating forward also produces the correct period
    Given the booklet is configured with a monthly cycle starting on day 27 and ending on day 26
    And the user is currently viewing February 2026 (period: 27 jan. - 26 fév.)
    When the user navigates to the next month
    Then the displayed period is "27 fév. - 26 mars 2026"
```

## Implementation Plan

### `client/pages/dashboard/index.vue`

1. Import `resolveMonthlyCycleRangeForTargetMonth` from `~/utils/monthlyCycleRange` (alongside the existing import).
2. In the `currentDateRange` computed property, replace the month-mode branch:
   - **Before**: `resolveCustomMonthlyRange(periodAnchorDate.value, startDay, endDay)`
   - **After**: `resolveMonthlyCycleRangeForTargetMonth(periodAnchorDate.value.getFullYear(), periodAnchorDate.value.getMonth() + 1, startDay, endDay)`
3. The `previousDateRange` computed property is unaffected — it uses `currentDateRange.value.start` as an anchor, which is now correct.

### `client/tests/pages/` (or `client/tests/unit/`)

Add unit tests to cover the five Gherkin scenarios above, targeting the
`resolveMonthlyCycleRangeForTargetMonth` utility function directly (pure function, no DOM needed):

- Cycle start=27, end=26, target=March 2026 → Feb 27 – Mar 26
- Cycle start=27, end=26, target=April 2026 → Mar 27 – Apr 26
- Default cycle start=1, target=March 2026 → Mar 1 – Mar 31
- Cycle start=5, end=4, target=March 2026 → Mar 5 – Apr 4
