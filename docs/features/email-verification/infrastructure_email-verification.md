# Infrastructure Module — Email Verification

**Context**
Persist the `emailVerified` state on the user table, store and retrieve verification tokens, and extend
the Spring mail adapter with a new verification email template. Mirrors the existing
`SpringMailNotificationAdapter` / `EmailTemplates` pattern.

**Scope (infrastructure)**
- Flyway migration: add `email_verified BOOLEAN NOT NULL DEFAULT FALSE` to the users table.
  Existing rows are **not** backfilled to `true` — every current account starts unverified.

  > **Operational note (future)**: when the system is deployed with this migration, all existing
  > beta-tester accounts will show `emailVerified = false`. A one-off admin action or data-patch
  > script will be needed to backfill those rows to `true` before the UI surfaces the unverified state.
  > Out of scope for this issue.

- `EmailVerificationTokenEntity` mapped to a new `email_verification_tokens` table
  (columns: `token`, `user_id`, `expires_at`).
- `JpaEmailVerificationTokenRepository` implementing the domain port
  (`save`, `findByToken`, `deleteByUserId`).
- Token TTL configurable via `app.email-verification.token-ttl` (default 24 h).
- Secure token generation: delegate to the existing infrastructure `IdGenerator` or a
  `SecureRandom`-based adapter — no plain UUIDs.
- `SpringMailNotificationAdapter` extended with `sendVerificationEmail(to, token, verificationLink)`,
  using a new `EmailTemplates.VERIFICATION` entry.

**Acceptance Criteria**
Feature: Email verification — persistence and delivery
  In order to verify mailboxes
  As the system
  I want verified state, tokens, and emails persisted and delivered correctly

Scenario: Token is saved and can be retrieved by value
  Given the JPA token repository
  When a token is saved for a user
  Then it can be retrieved by its token string with the correct userId and expiresAt

Scenario: All tokens for a user are deleted
  Given two tokens saved for the same userId
  When deleteByUserId is called
  Then no tokens remain for that user

Scenario: emailVerified survives a round-trip
  Given a user persisted with emailVerified = false
  When the user is updated to emailVerified = true and reloaded
  Then the persisted value is true

Scenario: Verification email contains the correct link
  Given the mail adapter is invoked with a token "abc123" and verificationLink "https://app/verify-email?token=abc123"
  When the email is sent
  Then the email body contains "https://app/verify-email?token=abc123"
