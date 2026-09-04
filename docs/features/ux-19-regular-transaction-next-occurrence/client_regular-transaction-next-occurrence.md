# Client Module — Show the next occurrence and the monthly commitment

**Context**
The regular transactions page lists the recurring entries but never answers the question that brings
the user there: when is the next one due, and how much am I committed to every month. The frequency
and the start date are already available client side (`frequencyToString`, `startDate`), so the next
occurrence and the monthly totals can be derived without a backend change.

**Acceptance Criteria**
Feature: Next occurrence and monthly commitment
  In order to anticipate my recurring charges
  As an authenticated user
  I want to see the next due date and my monthly totals

Scenario: 1. Each row shows its next occurrence
  Given I am on the regular transactions page
  When the list is rendered
  Then each entry displays the date of its next occurrence

Scenario: 2. A summary shows the monthly commitment
  Given I am on the regular transactions page
  When the page is rendered
  Then a summary displays the monthly recurring expenses total and the monthly recurring income total

Scenario: 3. An ended recurrence is marked as such
  Given a regular transaction whose recurrence has ended
  When the list is rendered
  Then the entry is marked as ended instead of showing a next occurrence

**Notes**
- Files: `pages/regular-transaction/index.vue`, `composables/useRegularTransaction.ts`, `utils/`.
- Reuse the frequency model already used by `FrequencySelector` and `MonthlyRepeatSelector`.
- Priority P1 - Effort M - Frontend only.
