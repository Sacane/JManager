# Client Module — Forgotten password screens

**Context**
`pages/login.vue` offers no way out for a user who lost their password. Two public pages are needed, in
the `centercard` layout like `pages/verify-email.vue`, whose state handling (loading, success,
expired, invalid) they follow. Available since earlier lots: `PasswordField` with its reveal control
(UX-47), `FieldError` for inline errors (UX-20), and the email carried between sign-in and sign-up
(UX-48). UX-27 adds the rules display and the strength indicator; this module places them on the reset
form, it does not build them.

**Acceptance Criteria**
Feature: Forgotten password screens
  In order to regain access to my account
  As a user who forgot their password
  I want to request a reset link and set a new password

Scenario: 1. The login page offers a way to reset
  Given I am on the login page in sign-in mode
  When the form is rendered
  Then a "Mot de passe oublié ?" link leads to "/forgot-password"

Scenario: 2. The address I typed follows me
  Given I typed "johan@example.com" on the login page
  When I follow the forgotten password link
  Then the request form is prefilled with it, and the address does not appear in the URL

Scenario: 3. Requesting a reset confirms without revealing anything
  Given I am on "/forgot-password"
  When I submit an address
  Then the page says an email was sent if an account matches, that the link lasts 30 minutes, and to check spam

Scenario: 4. Too many requests are explained
  Given the request endpoint answers 429
  When I submit an address
  Then the page asks me to wait a few minutes, without a generic error

Scenario: 5. The token leaves the address bar
  Given I open "/reset-password#token=abc"
  When the page loads
  Then the fragment is removed from the address bar and from the history entry before anything else

Scenario: 6. A valid link shows the form
  Given the token is reported valid
  When the page is rendered
  Then it shows the new password form with the password rules

Scenario: 7. A stale link offers a new one
  Given the token is reported expired or invalid
  When the page is rendered
  Then it says the link is no longer valid and offers a link to "/forgot-password", and shows no form

Scenario: 8. A link without a token is treated as invalid
  Given I open "/reset-password" with no fragment
  When the page is rendered
  Then it shows the invalid state without calling the API

Scenario: 9. Mismatched passwords are reported inline
  Given I am on the new password form
  When I enter two different passwords and submit
  Then an inline error is shown under the confirmation field and nothing is sent

Scenario: 10. A server refusal lands on its field
  Given the confirm endpoint answers 400 with the password policy key
  When I submit the form
  Then the message is shown under the new password field

Scenario: 11. The link expiring while I type is handled
  Given the form is shown
  When the confirm endpoint answers 400 with "token_expired"
  Then the page switches to the expired state and offers a new link

Scenario: 12. Success leads to sign-in
  Given I submit a compliant new password with a valid token
  When the reset succeeds
  Then I am taken to the login page with a message saying I can sign in with my new password

**Notes**
- Files: `pages/forgot-password.vue`, `pages/reset-password.vue`, `pages/login.vue`, a
  `usePasswordReset` composable — and its `vi.stubGlobal` stub in `tests/setup.ts`.
- `maxlength` 255 on the email field (`autocomplete="email"`), 100 on the password fields
  (`autocomplete="new-password"`).
- Scenario 2: pass the address through router state, not a query parameter — an address is personal
  data and a URL ends up in history and logs.
- Scenario 5 matters because the page may be left open, bookmarked or shared from the address bar.
  Use `history.replaceState`; the token then lives only in the component.
- Both pages are public: no `auth` middleware. Their server side needs no route: the SPA routing fix
  (PR #230, `docs/bugs/spa-pages-401-on-direct-load/`) serves any non-reserved path, which is what makes
  "/reset-password" open from the email at all.
- Every user-visible string in French; identifiers, `data-test` and tests in English.
- Priority P1 · Effort XL (full stack) · Part 4 of 4.
