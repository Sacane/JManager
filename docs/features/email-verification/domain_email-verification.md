# Domain Module — Email Verification

**Context**
Inaugural use of the feature-flag system: gate email verification behind the global flag `email-verification`.
The verification flow differs by the user's `SubscriptionPlan` (the per-plan decision lives here in the domain,
since flags are global ON/OFF):

- **BETA_TESTER** — receives an *access* email containing a link. Clicking the link is treated as proof the
  user owns a real mailbox, so following it **auto-confirms** their email.
- **Simple user (FREE)** — gated by a **sub-flag** `email-verification-simple-user-registration` because there
  is no settled registration strategy yet. When the sub-flag is on, a public registration is created with the
  **FREE** plan instead of `BETA_TESTER`, a *verification* email is sent, the user can still log in, but their
  email is flagged as **not verified** until they confirm it. When the sub-flag is off, registration keeps
  assigning `BETA_TESTER` (today's behaviour).

When `email-verification` is **off**, registration behaves exactly as today (welcome email only, no verified
state surfaced).

**Scope (domain)**
- `User` gains `emailVerified: Boolean` (default `false`).
- `EmailVerificationToken` model: token value, owning `UserId`, expiry — created via injected `Clock`/`IdGenerator`
  (no `LocalDateTime.now()` / `UUID.randomUUID()` in domain).
- Output ports: `EmailVerificationTokenRepository` (save, findByToken, deleteByUserId); extend the notification
  port to send an *access-confirmation* email (beta) and a *verification* email (simple user), each carrying the link.
- Use cases:
  - `VerifyEmailUseCase` — consumes a token, sets `emailVerified = true`, rejects expired/unknown tokens.
  - `ResendVerificationEmailUseCase` — re-issues a token + email for an unverified user.
- `RegisterUserService` branches on `IsFeatureEnabledUseCase("email-verification")` and the sub-flag to decide
  the assigned `SubscriptionPlan` (FREE when the sub-flag is on, else BETA_TESTER), the initial `emailVerified`
  state, and which email to send.

**Acceptance Criteria**
Feature: Email verification
  In order to confirm users own a real mailbox
  As the system
  I want flag-gated email verification that adapts to the subscription plan

Scenario: Flag off keeps current registration behaviour
  Given the flag "email-verification" is disabled
  When a user registers
  Then no verification or access-confirmation email logic runs
  And the welcome email is sent as before

Scenario: Beta tester link auto-confirms email
  Given the flag "email-verification" is enabled
  And a BETA_TESTER user has received an access email with a valid token
  When the user follows the access link and VerifyEmailUseCase consumes the token
  Then the user's emailVerified becomes true

Scenario: Sub-flag on assigns FREE and sends a verification email
  Given the flag "email-verification" is enabled
  And the sub-flag "email-verification-simple-user-registration" is enabled
  When a visitor registers
  Then the user is created with subscriptionPlan = FREE
  And emailVerified = false
  And a verification email with a token is sent
  And the user can still authenticate

Scenario: Sub-flag off keeps BETA_TESTER assignment
  Given the flag "email-verification" is enabled
  And the sub-flag "email-verification-simple-user-registration" is disabled
  When a visitor registers
  Then the user is created with subscriptionPlan = BETA_TESTER
  And the beta access-confirmation email is sent
  And no simple-user verification email is sent

Scenario: Expired token is rejected
  Given an email verification token whose expiry is in the past
  When VerifyEmailUseCase consumes it
  Then the system returns a failure
  And emailVerified remains false

Scenario: Unknown token is rejected
  Given a token value that matches no stored token
  When VerifyEmailUseCase consumes it
  Then the system returns a not-found failure

Scenario: Resend issues a fresh token for an unverified user
  Given an authenticated user with emailVerified = false
  When the user requests a verification email resend
  Then a new token is persisted
  And a verification email is sent

Scenario: Resend is rejected for an already-verified user
  Given an authenticated user with emailVerified = true
  When the user requests a verification email resend
  Then the system returns a failure
  And no new token is created
