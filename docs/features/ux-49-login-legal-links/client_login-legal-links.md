# Client Module — Link the legal documents from the login page

**Context**
`pages/login.vue` links `/terms` and `/privacy` from the registration consent checkboxes only, and those are hidden while the sign in form is displayed. The sidebar carries the same links but is only rendered for authenticated users.

**Acceptance Criteria**
Feature: Legal links on the login page
  In order to expose the legal documents to anonymous visitors
  As a developer
  I want them linked from the login page in both modes

Scenario: The links are rendered in sign in mode
  Given the login page is in sign in mode
  When it renders
  Then it holds a link to the terms and one to the privacy policy

Scenario: The links are rendered in registration mode
  Given the login page is in registration mode
  When it renders
  Then the same links are still available

Scenario: The links point at the legal routes
  Given the login page
  When I inspect the legal links
  Then they target the terms and privacy routes

**Notes**
- Files: `pages/login.vue`.
- The `legal` layout sends its back link to `/login`, which is the right destination for a visitor
  arriving from here — see UX-35 for the authenticated case.
- Priority P2 - Effort XS - Frontend only.
