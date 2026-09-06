# Client Module — Carry the email across the auth mode switch

**Context**
`pages/login.vue` holds `userAuth` and `userRegistered` as two independent reactive objects. `switchMode` only resets the error state, so the email typed on one form is lost when the other is displayed.

**Acceptance Criteria**
Feature: Email carried across the auth mode switch
  In order to keep the switch cheap for the user
  As a developer
  I want switchMode to carry the email over

Scenario: Switching to registration copies the email
  Given the sign in email is filled
  When switchMode is called with register
  Then the registration email holds the same value

Scenario: Switching to sign in copies the email
  Given the registration email is filled
  When switchMode is called with login
  Then the sign in email holds the same value

Scenario: An empty email overwrites nothing
  Given the source email is empty
  When switchMode is called
  Then the target email keeps its previous value

Scenario: Passwords are never carried
  Given both forms hold a password
  When switchMode is called
  Then neither password is copied to the other form

**Notes**
- Files: `pages/login.vue`.
- Carry the email only. Copying a password between forms would be surprising and would confuse
  credential managers.
- Priority P2 - Effort XS - Frontend only.
