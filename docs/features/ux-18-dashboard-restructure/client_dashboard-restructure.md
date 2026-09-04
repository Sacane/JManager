# Client Module — Restructure the dashboard into three readable zones

**Context**
`pages/index.vue` stacks about fifteen blocks that all share the same visual weight
(`rounded-2xl p-6 shadow-lg`), so nothing signals what matters. Several figures are shown twice or
three times: the header pills repeat the upcoming totals, the quick stats banner repeats the tag and
forecast counts, and the "Actions rapides" card only duplicates the sidebar links. The header also
claims "Tous les comptes" while no aggregated mode exists.

**Acceptance Criteria**
Feature: Readable dashboard
  In order to understand my situation at a glance
  As an authenticated user
  I want the dashboard organised into a few meaningful zones

Scenario: 1. The dashboard is organised in three zones
  Given I open the dashboard
  When the page is rendered
  Then the content is grouped into a situation zone, an upcoming zone and a breakdown zone

Scenario: 2. No figure is displayed twice
  Given I open the dashboard
  When the page is rendered
  Then each key figure appears in exactly one place

Scenario: 3. Quick actions offer real shortcuts
  Given I open the dashboard
  When the quick actions are rendered
  Then they offer actions that are not already in the sidebar

Scenario: 4. A misleading account label is not displayed
  Given no aggregated account mode exists
  When the dashboard header is rendered
  Then it never claims to show all accounts

**Notes**
- Depends on UX-11.
- Files: `pages/index.vue`.
- Move the secondary analysis blocks out of the main view; drop the duplicated quick stats banner.
- Priority P1 - Effort L - Frontend only.
