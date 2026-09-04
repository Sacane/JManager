# UX-32 — No debug label in the shared header

**Context**
`components/NHeader.vue` renders the name of the active theme — "Clair" or "Sombre" — as 2rem text in the top-left corner. It labels nothing and duplicates the state already carried by the toggle icon next to it. The header is shared by the legal pages and by the consent, email verification and forced password change screens, so it sits on top of a consent form.

**Acceptance Criteria**
Feature: No debug label in the shared header
  In order to read a page without distraction
  As a user
  I want the header to show controls, not the state of a control

Scenario: The theme name is not displayed
  Given I open a page using the shared header
  When the header is rendered
  Then the name of the active theme is not displayed as page content

Scenario: The theme can still be changed
  Given I open a page using the shared header
  When I activate the theme control
  Then the theme changes and the control reflects the new state

**Notes**
- Layer-agnostic functional acceptance for UX-32. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
