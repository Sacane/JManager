# Domain Module — Password reset request and confirmation

**Context**
A user who forgets their password has no way to recover their account: there is no forgotten password
flow anywhere in the product, and the backend exposes no matching endpoint. The domain must own the
reset token lifecycle: issuing a single-use token bound to a user with an expiry, validating it, and
applying the new password. The clock and the token generation must be injected, following the domain
rules of the project.

**Acceptance Criteria**
Feature: Password reset lifecycle
  In order to recover access to my account
  As a registered user
  I want a single-use time-limited reset token

Scenario: 1. Requesting a reset issues a token
  Given a registered user with a verified email address
  When a password reset is requested for that address
  Then a single-use token bound to that user is issued with an expiry date

Scenario: 2. An unknown address issues no token
  Given no user is registered with a given email address
  When a password reset is requested for that address
  Then no token is issued and no error reveals whether the account exists

Scenario: 3. A valid token applies the new password
  Given a reset token that is neither expired nor already used
  When the reset is confirmed with a new password
  Then the user password is replaced and the token is marked as used

Scenario: 4. An expired token is rejected
  Given a reset token whose expiry date has passed
  When the reset is confirmed with a new password
  Then the reset is rejected and the password is unchanged

Scenario: 5. An already used token is rejected
  Given a reset token that has already been consumed
  When the reset is confirmed with a new password
  Then the reset is rejected and the password is unchanged

**Notes**
- Inject `Clock` and the token generator; no `LocalDate.now()` or `UUID.randomUUID()` in the domain.
- Return `Result<T>` for the business failures (expired, already used, unknown token).
- Do not leak account existence: requesting a reset must behave identically for known and unknown addresses.
- Priority P1 - Effort XL (full stack) - Part 1 of 4.
