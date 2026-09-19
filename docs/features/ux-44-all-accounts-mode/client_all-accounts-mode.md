# Client Module — Add a real "Tous les comptes" mode to the dashboard

**Context**
`pages/index.vue` resolves `selectedBookletId` to an existing booklet, falling back to the first one,
and the header `select` lists only individual booklets. The subtitle falls back to "Tous les comptes"
when no booklet is selected, which only happens on a failed load — the label described a view that
did not exist.

Most of the aggregation already works. The stats endpoints take an optional `bookletId` and aggregate
every booklet when it is omitted — made reliable by the JPA fetch fix of 29 August 2026 — and
`selectedBookletBalance` already falls back to the total balance when no booklet is selected.

Three decisions, taken with the product owner on 19 September 2026:

- **Period**: monthly cycles are per booklet, so an aggregated view has no cycle of its own. It uses
  the **calendar month**, first to last day. A booklet's cycle applies again as soon as that booklet
  is selected.
- **Balance**: the total across every booklet.
- **Single-booklet features**: the account budget is keyed by booklet and is hidden. The "add a
  transaction" and "import a CSV" quick actions need one target booklet and are hidden; "create a
  regular transaction" stays, since its dialog lets the user pick booklets.

**Acceptance Criteria**
Feature: Aggregated dashboard mode
  In order to see my overall situation
  As an authenticated user
  I want a "Tous les comptes" option that aggregates every booklet

Scenario: 1. The selector offers the aggregated mode first
  Given I have two booklets
  When the account selector is rendered
  Then it offers "Tous les comptes" before the individual booklets

Scenario: 2. The aggregated mode queries every booklet
  Given "Tous les comptes" is selected
  When the dashboard loads its statistics
  Then the requests carry no booklet id

Scenario: 3. The aggregated mode uses the calendar month
  Given a booklet whose cycle starts on the 25th
  And "Tous les comptes" is selected
  When the month view of September 2026 is displayed
  Then the period runs from 01/09/2026 to 30/09/2026

Scenario: 4. A selected booklet keeps its own cycle
  Given a booklet whose cycle starts on the 25th
  When that booklet is selected for the month view of September 2026
  Then the period runs from 25/08/2026 to 24/09/2026

Scenario: 5. The balance is the total of every booklet
  Given two booklets holding 100 and 250
  When "Tous les comptes" is selected
  Then the balance shown is 350

Scenario: 6. The header names the aggregated view
  Given "Tous les comptes" is selected
  When the dashboard header is rendered
  Then it reads "Tous les comptes"

Scenario: 7. The account budget is hidden
  Given "Tous les comptes" is selected
  When the dashboard is rendered
  Then the account budget block is not displayed

Scenario: 8. Single-booklet quick actions are hidden
  Given "Tous les comptes" is selected
  When the quick actions are rendered
  Then "add a transaction" and "import a CSV" are not offered
  And "create a regular transaction" is still offered

Scenario: 9. The choice survives a reload
  Given "Tous les comptes" was selected
  When the dashboard is loaded again
  Then "Tous les comptes" is still selected

**Notes**
- Files: `pages/index.vue`.
- The selection is persisted through the existing `dashboard.selectedBookletId.v1` key, with a
  sentinel value for the aggregated mode.
- Priority P2 - Effort M - Frontend only.
