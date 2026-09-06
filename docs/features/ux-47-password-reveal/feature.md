# UX-47 — Seeing the password being typed

**Context**
None of the screens asking for a password offers a way to reveal it. On a phone, typing a long password blind is the main cause of a failed sign-in, and the user has no way to check what they entered before submitting.

**Acceptance Criteria**
Feature: Seeing the password being typed
  In order to check what I typed before submitting
  As a user entering a password
  I want to reveal it

Scenario: The password can be revealed
  Given I typed a password
  When I activate the reveal control
  Then the password is displayed in clear text

Scenario: The password can be hidden again
  Given I revealed my password
  When I activate the control again
  Then the password is masked again

Scenario: Every password field offers it
  Given any screen asking me for a password
  When I look at the field
  Then a reveal control is available

**Notes**
- Layer-agnostic functional acceptance for UX-47. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
