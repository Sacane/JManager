# Client Module — Login by Email

**Context**
The login form in `client/pages/login.vue` currently presents a "username" text field. With authentication now using email, the field must be replaced by an email input. The `UserAuth` type and the `login()` call in `client/composables/useAuth.ts` must be updated accordingly.

**Acceptance Criteria**
Feature: Login form accepts email
  In order to authenticate with my email
  As a registered user
  I want to enter my email address in the login form

Scenario: Successful login from the UI
  Given a registered user with email "alice@example.com"
  When the user enters "alice@example.com" in the email field and the correct password
  And clicks "Se connecter"
  Then the user is authenticated and redirected to the dashboard

Scenario: Login form rejects a malformed email client-side
  Given the user enters "notanemail" in the email field
  When the user submits the form
  Then a validation error is shown inline
  And the form is not submitted

Scenario: Server returns 404 for unknown email
  Given the user enters an email that does not exist in the system
  When the user submits the form
  Then an error message "Identifiants invalides" (or equivalent) is displayed
  And the user remains on the login page

Scenario: Server returns 429 (rate limit)
  Given the user has exceeded the maximum number of login attempts
  When the user submits the form
  Then an error message indicating too many attempts is displayed

**Implementation Plan**
1. **`client/pages/login.vue`** — login section:
   - Replace `<InputText ... v-model="username" />` with an email input (`type="email"`).
   - Update label from "Nom d'utilisateur" (or equivalent) to "Adresse e-mail".
   - Add `maxlength="255"` to match the backend constraint.
   - Update the bound reactive field name from `username` to `email`.

2. **`client/composables/useAuth.ts`**:
   - Update `UserAuth` type: rename field `username` → `email`.
   - Update the `login()` call body to send `{ email, password }` instead of `{ username, password }`.
