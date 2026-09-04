# UX-48 — Switching between sign in and sign up keeps what I typed

**Context**
The login page switches between sign in and sign up in place. A user who typed their email, realised they have no account and switched to registration has to type it again: the two forms keep independent state.

**Acceptance Criteria**
Feature: Switching between sign in and sign up keeps what I typed
  In order not to retype what I just entered
  As a visitor
  I want my email carried across the switch

Scenario: The email survives the switch to registration
  Given I typed my email on the sign in form
  When I switch to registration
  Then the email field is already filled with it

Scenario: The email survives the switch back
  Given I typed my email on the registration form
  When I switch back to sign in
  Then the email field is already filled with it

Scenario: Passwords are not carried across
  Given I typed a password on either form
  When I switch to the other
  Then the password fields are empty

**Notes**
- Layer-agnostic functional acceptance for UX-48. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
