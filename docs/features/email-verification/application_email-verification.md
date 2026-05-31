# Application Module — Email Verification

**Context**
Expose the verification endpoints publicly and surface `emailVerified` on the authenticated user
payload so the client can render the appropriate UI. No feature flag involved — endpoints are always
active. The verification link in the email lands on the client page, which calls the API with the token.

**Scope (application)**
- `GET /verify-email?token={token}` (public, no auth) — delegates to `VerifyEmailUseCase`;
  returns 200 on success, 410 Gone on expiry, 404 on unknown token.
- `POST /verify-email/resend` (authenticated) — delegates to `ResendVerificationEmailUseCase`;
  returns 200, or 409 Conflict if already verified.
- Add `emailVerified: Boolean` to the authenticated user payload (the response returned by `/me`,
  session, and refresh endpoints) so the client always has the current state.
- No business logic in controllers — all branching lives in the use cases.

**Acceptance Criteria**
Feature: Email verification API
  In order to let users confirm their email
  As a client
  I want public verification and authenticated resend endpoints, and verified state in the user payload

Scenario: Valid token is accepted
  Given a valid, unexpired verification token
  When the client calls GET /verify-email?token=<valid>
  Then the response is 200
  And the user's emailVerified becomes true

Scenario: Expired token returns 410
  Given an expired verification token
  When the client calls GET /verify-email?token=<expired>
  Then the response is 410 Gone

Scenario: Unknown token returns 404
  Given a token value matching no record
  When the client calls GET /verify-email?token=<unknown>
  Then the response is 404

Scenario: Resend dispatches a new verification email
  Given an authenticated user with emailVerified = false
  When the client calls POST /verify-email/resend
  Then the response is 200
  And a new verification email is dispatched

Scenario: Resend is rejected for a verified user
  Given an authenticated user with emailVerified = true
  When the client calls POST /verify-email/resend
  Then the response is 409 Conflict

Scenario: emailVerified is exposed in the user payload
  Given an authenticated user
  When the client calls the session or /me endpoint
  Then the response body includes emailVerified as a boolean
