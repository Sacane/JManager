# Client Module — Declare autocomplete on the auth forms

**Context**
`pages/login.vue` declares `autocomplete="email"` on the login email only. The login password, the registration username, email, password and confirmation carry none, so browsers and password managers fall back to heuristics.

**Acceptance Criteria**
Feature: Autocomplete hints on the auth forms
  In order for credential managers to behave predictably
  As a developer
  I want every auth field to declare its autocomplete role

Scenario: The login fields declare their roles
  Given the login form
  When I inspect its fields
  Then the email declares email and the password declares current-password

Scenario: The registration fields declare their roles
  Given the registration form
  When I inspect its fields
  Then the username declares username, the email declares email, and both password fields declare new-password

Scenario: The form is wrapped for credential detection
  Given the auth forms
  When I inspect their markup
  Then each is a form element with a submit action

**Notes**
- Files: `pages/login.vue`.
- `new-password` on both registration password fields is the documented pattern for a password plus
  its confirmation; `current-password` belongs to the login one.
- Priority P2 - Effort XS - Frontend only.
