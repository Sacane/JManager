# Client Module — Forgotten password screens

**Context**
`pages/login.vue` offers no forgotten password link, which is a dead end for any user who loses their
password. Two screens are needed: requesting a reset from the login page, and setting a new password
from the link received by email, in the style of the existing `verify-email` page.

**Acceptance Criteria**
Feature: Forgotten password screens
  In order to regain access to my account
  As a user who forgot their password
  I want to request a reset link and set a new password

Scenario: 1. The login page offers a forgotten password link
  Given I am on the login page
  When the login form is rendered
  Then a forgotten password link is displayed

Scenario: 2. Requesting a reset confirms without revealing anything
  Given I am on the reset request screen
  When I submit my email address
  Then a neutral confirmation message is displayed

Scenario: 3. A valid link lets me set a new password
  Given I open the reset link received by email
  When I enter a new password and its confirmation
  Then my password is updated and I am redirected to the login page

Scenario: 4. An expired link offers to request a new one
  Given I open a reset link whose token has expired
  When the page is rendered
  Then an expired state is displayed with an action to request a new link

Scenario: 5. Mismatched passwords are reported inline
  Given I am on the new password screen
  When I enter two different passwords
  Then an inline error is displayed and the form is not submitted

**Notes**
- Mirror the state handling of `pages/verify-email.vue` (loading, success, expired, invalid).
- `maxlength` 255 on the email field and 100 on the password fields.
- Combine with UX-27 for the password strength indicator.
- Priority P1 - Effort XL (full stack) - Part 4 of 4.
