# Client Module — Filter booklet transactions by date range

**Context**
The booklet detail page locks navigation to a single month through a month select plus a year date
picker, which is two controls for one notion and offers no previous/next month shortcut. Seeing a
custom range, or the last ninety days, is impossible. The backend already accepts optional
`startDate` and `endDate` parameters on `GET /api/booklet/{id}/transactions`, `/balances` and
`/report`, so this is frontend work only.

**Acceptance Criteria**
Feature: Date range filtering of transactions
  In order to analyse an arbitrary period
  As an authenticated user
  I want to filter the transactions of a booklet by a date range

Scenario: 1. A custom range loads the matching transactions
  Given I am on the detail page of a booklet
  When I select a start date and an end date
  Then only the transactions within that range are listed
  And the displayed balances match that range

Scenario: 2. Quick shortcuts are available
  Given I am on the detail page of a booklet
  When I choose a preset range
  Then the corresponding start and end dates are applied

Scenario: 3. Clearing the range returns to the monthly view
  Given a custom date range is applied
  When I clear the range
  Then the page returns to the selected month and year

Scenario: 4. An invalid range is rejected
  Given I am on the detail page of a booklet
  When I select an end date earlier than the start date
  Then an inline error is displayed and no request is sent

**Notes**
- Backend already supports it: `startDate` / `endDate` on the booklet endpoints.
- Files: `pages/booklet/[id].vue`, `components/booklet/BookletPageHeader.vue`, `composables/useBooklet.ts`.
- Priority P1 - Effort M - Frontend only.
