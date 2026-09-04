# Client Module — Unify the income and expense colour code across pages

**Context**
The colour meaning changes from page to page. In `pages/booklet/[id].vue` income is blue
(`#009CFE`), while the dashboard, the booklets page and the regular transactions page render income
in green. In the booklet table the empty-income placeholder is even rendered in blue while the empty
expense placeholder is grey, so the two columns treat emptiness differently. Users have to relearn
the colour code when changing page.

**Acceptance Criteria**
Feature: One colour code for income and expenses
  In order to read any screen the same way
  As an authenticated user
  I want income and expenses to always use the same colours

Scenario: 1. Income uses the same colour on every page
  Given the semantic tokens are available
  When I compare an income amount on the dashboard, the booklet detail and the regular transactions
  Then the three use the same income token

Scenario: 2. Expenses use the same colour on every page
  Given the semantic tokens are available
  When I compare an expense amount on the dashboard, the booklet detail and the regular transactions
  Then the three use the same expense token

Scenario: 3. Empty amount cells are neutral
  Given a transaction that is an expense
  When the income column is rendered for that row
  Then the empty placeholder uses a neutral colour, like the expense column does

**Notes**
- Depends on UX-11.
- Files: `pages/booklet/[id].vue`, `pages/index.vue`, `pages/booklet/index.vue`, `pages/regular-transaction/index.vue`, `components/booklet/BookletPageHeader.vue`.
- A product decision is required first: income is green or blue. Apply it everywhere.
- Priority P1 - Effort S - Frontend only.
