# Client Module — Add a shared password field with a reveal control

**Context**
Password inputs are declared independently on `pages/login.vue` (2 fields), `pages/settings/index.vue` (3) and `pages/force-password-change.vue` (2), each as a bare `InputText type="password"`. None offers a reveal control. A shared component removes the duplication and gives UX-27 a single place to add the rules and strength indicator later.

**Acceptance Criteria**
Feature: Shared password field
  In order to stop redeclaring password inputs
  As a developer
  I want one component carrying the input and its reveal control

Scenario: The component toggles the input type
  Given the password field is rendered
  When the reveal control is activated
  Then the input type switches between password and text

Scenario: The control is labelled for assistive technology
  Given the password field is rendered
  When I inspect the reveal control
  Then it carries an accessible name reflecting its current action

Scenario: The value is bound both ways
  Given the password field is rendered
  When the user types
  Then the bound model receives the value

Scenario: Every password screen uses it
  Given the login, settings and forced password change screens
  When I inspect their password inputs
  Then all of them are the shared component

**Notes**
- New component under `components/`, with a spec in `tests/components/`.
- Must forward `autocomplete`, `maxlength` and `id` so UX-46 keeps working through it.
- UX-27 will extend the same component with the rules and the strength indicator; keep it open to
  that rather than closing over the current need.
- Priority P2 - Effort S - Frontend only.
