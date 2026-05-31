# Domain Module — Email Verification

**Context**
Self-registered users must verify their email address before gaining full trust. Users created directly
by an admin (beta testers) are considered implicitly verified — the admin vouches for the address.
The domain models the verification token lifecycle, adapts the registration and admin-creation flows,
and exposes two use cases: token consumption and resend.

No feature flag is involved — the verification flow is always active.

**Scope (domain)**
- `User` gains `emailVerified: Boolean`.
  - Admin-created users (`CreateAdminIfNotExists`, admin user-creation) → `emailVerified = true`.
  - Self-registered users (`RegisterUserService`) → `emailVerified = false`; a token is generated and
    a verification email is dispatched immediately after account creation.
- `EmailVerificationToken` value object: secure token string, owning `UserId`, `expiresAt` — created
  via injected `Clock` and `IdGenerator` (no `LocalDateTime.now()` / `UUID.randomUUID()` in domain).
- Output ports:
  - `EmailVerificationTokenRepository` — `save`, `findByToken`, `deleteByUserId`.
  - Extend `NotificationPort` with `sendVerificationEmail(to, token, verificationLink)`.
- Use cases:
  - `VerifyEmailUseCase` — accepts a raw token string, resolves the token, checks expiry, marks
    `emailVerified = true` on the owner, then deletes all tokens for that user.
  - `ResendVerificationEmailUseCase` — rejects already-verified users; deletes any existing token,
    issues a fresh one, and dispatches a new verification email.

**Acceptance Criteria**
Feature: Email verification
  In order to confirm that registered users own a real mailbox
  As the system
  I want to issue and consume verification tokens on self-registration

Scenario: Self-registered user gets emailVerified = false and receives a verification email
  Given a visitor completes the registration form
  When RegisterUserService creates the account
  Then the user is persisted with emailVerified = false
  And a verification token is saved
  And a verification email is dispatched via NotificationPort

Scenario: Admin-created user is auto-verified
  Given an admin creates a user through the admin user-creation flow
  When the user is persisted
  Then emailVerified = true
  And no verification token is issued
  And no verification email is sent

Scenario: Valid token is consumed and email is marked verified
  Given a self-registered user with emailVerified = false
  And a valid, unexpired token exists for that user
  When VerifyEmailUseCase consumes the token
  Then emailVerified becomes true
  And all tokens for that user are deleted

Scenario: Expired token is rejected
  Given a verification token whose expiresAt is in the past
  When VerifyEmailUseCase consumes it
  Then the system returns an EXPIRED failure
  And emailVerified remains false

Scenario: Unknown token is rejected
  Given a token string that matches no stored token
  When VerifyEmailUseCase consumes it
  Then the system returns a NOT_FOUND failure

Scenario: Resend issues a fresh token for an unverified user
  Given an authenticated user with emailVerified = false
  When ResendVerificationEmailUseCase is invoked
  Then any existing token for that user is deleted
  And a new token is saved
  And a verification email is dispatched

Scenario: Resend is rejected for an already-verified user
  Given an authenticated user with emailVerified = true
  When ResendVerificationEmailUseCase is invoked
  Then the system returns an ALREADY_VERIFIED failure
  And no token is created or email sent
