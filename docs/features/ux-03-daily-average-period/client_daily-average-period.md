# Client Module — Fix the daily average computed against a hardcoded 30 days

**Context**
The dashboard expense card displays `Moy. journaliere: {{ (monthlyExpenses / 30).toFixed(2) }} EUR`
in `pages/index.vue`. The divisor is hardcoded to 30 regardless of the selected period, so the value
is roughly 3x too high in the quarter view and 12x too high in the year view. The average must be
computed against the real number of days of the active period, which is already resolved by
`resolveMonthlyCycleRangeForTargetMonth` / `resolveMonthlyCycleRangeFromAnchor`.

**Acceptance Criteria**
Feature: Correct daily expense average
  In order to trust the figures shown on my dashboard
  As an authenticated user
  I want the daily average to match the selected period

Scenario: 1. Monthly view uses the real day count of the period
  Given the dashboard is on the month period
  And the resolved period spans 31 days with 620.00 EUR of expenses
  When the expense card is rendered
  Then the daily average shows 20.00 EUR

Scenario: 2. Quarter view divides by the quarter length
  Given the dashboard is on the quarter period
  And the resolved period spans 92 days with 920.00 EUR of expenses
  When the expense card is rendered
  Then the daily average shows 10.00 EUR

Scenario: 3. Year view divides by the year length
  Given the dashboard is on the year period
  And the resolved period spans 365 days with 3650.00 EUR of expenses
  When the expense card is rendered
  Then the daily average shows 10.00 EUR

Scenario: 4. A period with no elapsed day does not divide by zero
  Given the dashboard is on a period resolving to zero day
  When the expense card is rendered
  Then the daily average shows 0.00 EUR instead of an invalid value

**Notes**
- Files: `pages/index.vue`.
- Add a unit test covering the three periods.
- Priority P0 - Effort XS - Frontend only.
