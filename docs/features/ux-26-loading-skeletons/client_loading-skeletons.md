# Client Module — Replace full-screen spinners with skeletons

**Context**
Four different loading treatments coexist: a full-screen `pi pi-spin pi-spinner` on the dashboard and
the booklet detail, a PrimeVue `ProgressSpinner` in a card on the booklets, tags and admin pages, raw
text on the settings page, and inline spinners in the dialogs. No skeleton exists, so the dashboard
stays nearly empty while loading and then reveals about fifteen blocks at once.

**Acceptance Criteria**
Feature: Consistent loading feedback
  In order to perceive the application as responsive
  As an authenticated user
  I want the page structure shown while the data loads

Scenario: 1. The dashboard shows a skeleton of its layout
  Given the dashboard data is loading
  When the page is rendered
  Then placeholder blocks matching the final layout are displayed

Scenario: 2. Lists show row placeholders
  Given a paginated list is loading
  When the page is rendered
  Then placeholder rows are displayed instead of a full-screen spinner

Scenario: 3. Loading feedback is consistent across pages
  Given any page is loading its data
  When the page is rendered
  Then the same loading component family is used

Scenario: 4. The skeleton disappears when the data arrives
  Given a page displaying its loading skeleton
  When the data arrives
  Then the skeleton is replaced by the real content without layout shift

**Notes**
- Files: `pages/index.vue`, `pages/booklet/[id].vue`, `pages/booklet/index.vue`, `pages/tag/index.vue`, `pages/admin/index.vue`, `pages/settings/index.vue`.
- Extract a shared skeleton component rather than duplicating markup.
- Priority P1 - Effort M - Frontend only.
