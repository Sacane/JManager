# UX-46 — Password managers work on the auth forms

**Context**
Only the login email field declares an autocomplete hint. The login password, and every registration field, declare none, so password managers cannot reliably fill or offer to save credentials.

**Acceptance Criteria**
Feature: Password managers work on the auth forms
  In order to sign in without retyping my credentials
  As a user with a password manager
  I want the auth forms to be recognised

Scenario: Signing in is offered from the manager
  Given I stored my credentials in my password manager
  When I open the login form
  Then the manager offers to fill the email and the password

Scenario: Registering offers to save a new credential
  Given I fill the registration form
  When I submit it
  Then the manager offers to save the new credential

Scenario: The confirmation field is not treated as a new password
  Given I fill the registration form
  When the manager reads the confirmation field
  Then it is understood as a confirmation, not as a second credential

**Notes**
- Layer-agnostic functional acceptance for UX-46. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
