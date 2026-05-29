# Infrastructure Module — Email Verification

**Context**
Persist the new verified state and verification tokens, and extend the Spring mail adapter to send the two new
email types with a link carrying the token. Mirrors the existing `SpringMailNotificationAdapter` /
`EmailTemplates` approach and reuses the configured `app.url`.

**Scope (infrastructure)**
- `emailVerified` column added to the user table, default `false`. Existing rows are **not** backfilled to
  `true` — every current account starts unverified and must confirm through the flow once the flag is enabled.

> **TODO (future)**: when `email-verification` is first turned ON in production, every existing user will see
> the unverified banner and need to re-confirm. A one-off re-notification job (e.g. "send access/verification
> email to all users with `emailVerified = false`") will be needed before flipping the flag in prod. Out of
> scope for this issue — create a dedicated operational task when the flag is ready to be enabled.
- `EmailVerificationTokenEntity` + JPA adapter implementing `EmailVerificationTokenRepository`
  (save, findByToken, deleteByUserId); token TTL configurable.
- Token generation via the infrastructure `IdGenerator`/secure random adapter.
- `NotificationPort` extension implemented: `sendAccessConfirmationEmail(...)` (beta — link confirms on click)
  and `sendEmailVerificationEmail(...)` (simple user), each building a link as `${app.url}/verify-email?token=...`.
- New `EmailTemplates` entries for the access-confirmation and verification emails.

**Acceptance Criteria**
Feature: Email verification persistence and delivery
  In order to verify mailboxes
  As the system
  I want verified state, tokens, and emails persisted and delivered

Scenario: Persist and look up a verification token
  Given the token adapter
  When a token is saved for a user
  Then it can be retrieved by its token value with the correct owner and expiry

Scenario: Verified state survives a round-trip
  Given a user persisted with emailVerified = false
  When the user is updated to emailVerified = true
  Then reloading the user reports emailVerified = true

Scenario: Verification email contains the token link
  Given the flag-gated simple-user flow triggers a verification email
  When the mail adapter sends it
  Then the email body contains a link to ${app.url}/verify-email with the generated token

Scenario: Access-confirmation email is sent for beta testers
  Given a BETA_TESTER registration under the enabled flag
  When the mail adapter sends the access email
  Then the body contains an access link carrying the verification token

Scenario: Tokens are cleared after successful verification
  Given a user verifies their email
  When verification succeeds
  Then their outstanding verification tokens are removed
