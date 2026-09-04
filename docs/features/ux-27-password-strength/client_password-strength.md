# Client Module — Show password rules and strength

**Context**
Three screens ask for a new password without stating any rule or showing any strength feedback:
registration on `pages/login.vue`, the settings password change and `force-password-change`. The user
discovers the constraint through a generic server error after submitting. None of the three offers a
show-password toggle either, which is the main cause of failed entry on mobile.

**Acceptance Criteria**
Feature: Password rules and strength feedback
  In order to choose a valid password on the first try
  As a user setting a password
  I want the rules and the strength displayed as I type

Scenario: 1. The rules are displayed before typing
  Given I am on a screen asking for a new password
  When the form is rendered
  Then the password rules are displayed

Scenario: 2. Rules are validated live
  Given I am on a screen asking for a new password
  When I type a password
  Then each satisfied rule is marked as satisfied while I type

Scenario: 3. A strength indicator is displayed
  Given I am on a screen asking for a new password
  When I type a password
  Then a strength indicator reflects its quality

Scenario: 4. The password can be revealed
  Given I am on a screen asking for a new password
  When I activate the show password control
  Then the password becomes readable in clear text

Scenario: 5. A weak password blocks submission
  Given I typed a password that does not satisfy the rules
  When I try to submit the form
  Then the submission is blocked and the unmet rules are highlighted

**Notes**
- Files: `pages/login.vue`, `pages/settings/index.vue`, `pages/force-password-change.vue`, plus a shared password field component.
- The rules must mirror the backend policy; align with the admin console hint, which currently states a weaker minimum of 6 characters.
- Combine with UX-21 for the reset screen.
- Priority P1 - Effort M - Frontend only.
