# Client Module — Disambiguate the two save actions of the settings page

**Context**
`pages/settings/index.vue` exposes two buttons with different scopes and no explanation:
"Changer le mot de passe" inside its own card, and "Enregistrer les parametres" at the bottom of the
page which persists only the projection window and the booklet cycles. A user who edits the
projection window and then clicks the password button loses the unsaved settings silently. There is
also no unsaved-changes indicator and no guard when navigating away.

**Acceptance Criteria**
Feature: Explicit and safe settings saving
  In order not to lose my changes
  As an authenticated user
  I want each settings section to state exactly what it saves

Scenario: 1. Each section states its own scope
  Given I am on the settings page
  When the page is rendered
  Then every section that can be saved shows a button naming what it saves

Scenario: 2. Unsaved changes are visible
  Given I changed the projection window without saving
  When I look at the projection section
  Then an unsaved-changes indicator is displayed

Scenario: 3. Leaving with unsaved changes asks for confirmation
  Given I changed the projection window without saving
  When I navigate to another page
  Then a confirmation is requested before leaving

Scenario: 4. Saving one section does not discard another
  Given I changed the projection window
  When I submit the password change form
  Then my pending projection change is still present after the password is changed

**Notes**
- Files: `pages/settings/index.vue`.
- Suggested approach: per-section save with inline confirmation, plus `onBeforeRouteLeave` guard.
- Priority P0 - Effort M - Frontend only.
