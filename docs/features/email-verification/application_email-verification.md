# Application Module — Email Verification

**Context**
Expose the verification endpoints, surface the verified state to the frontend, and wire the flag-gated
registration branching through the existing buses. The link in the emails lands on the client, which calls the
verify endpoint with the token.

**Scope (application)**
- `POST /verify-email` (token in body or query) → consumes the token via `VerifyEmailUseCase`; returns success,
  expired, or not-found outcomes mapped to appropriate status codes.
- `POST /verify-email/resend` (authenticated) → `ResendVerificationEmailUseCase`.
- Surface `emailVerified` on the authenticated user payload (session/refresh response and/or a `/me` settings
  endpoint) so the client can render the unverified banner and settings indicator.
- Wire `RegisterUserService` flag-gated behaviour through the command bus (no behavioural logic in the controller).

**Acceptance Criteria**
Feature: Email verification API
  In order to let users confirm their email
  As a client
  I want endpoints to verify and resend, and verified state in the user payload

Scenario: Verify with a valid token
  Given a valid, unexpired verification token
  When the client calls POST /verify-email with that token
  Then the response is 200
  And the user's emailVerified becomes true

Scenario: Verify with an expired token
  Given an expired verification token
  When the client calls POST /verify-email with that token
  Then the response is a 4xx error indicating the link expired

Scenario: Verify with an unknown token
  Given a token matching no record
  When the client calls POST /verify-email with that token
  Then the response is 404

Scenario: Resend verification email
  Given an authenticated user with emailVerified = false
  When the user calls POST /verify-email/resend
  Then the response is 200
  And a new verification email is dispatched

Scenario: Verified state is exposed to the client
  Given an authenticated user
  When the client fetches the user/session payload
  Then the payload includes emailVerified

Scenario: Resend is rejected for a verified user
  Given an authenticated user with emailVerified = true
  When the user calls POST /verify-email/resend
  Then the response is a 4xx error
