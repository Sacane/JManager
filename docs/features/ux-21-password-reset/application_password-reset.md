# Application Module — Password reset REST endpoints

**Context**
The application module must expose the two REST endpoints of the password reset flow and wire them to
the domain use cases. These endpoints are unauthenticated and therefore need rate limiting and
uniform responses that do not disclose whether an account exists.

**Acceptance Criteria**
Feature: Password reset endpoints
  In order to reset my password from the client
  As an anonymous visitor
  I want endpoints to request a reset and to confirm it

Scenario: 1. Requesting a reset returns a neutral response
  Given an anonymous visitor
  When a reset is requested for any email address
  Then the response is 202 Accepted regardless of whether the account exists

Scenario: 2. Confirming with a valid token succeeds
  Given a valid reset token
  When the reset is confirmed with a new password and its confirmation
  Then the response is 204 No Content and the password is updated

Scenario: 3. Confirming with an invalid token fails
  Given an expired or already used reset token
  When the reset is confirmed
  Then the response is 400 Bad Request with a business error code

Scenario: 4. Mismatched passwords are rejected
  Given a valid reset token
  When the reset is confirmed with two different passwords
  Then the response is 400 Bad Request and the password is unchanged

Scenario: 5. Repeated requests are rate limited
  Given an anonymous visitor
  When reset requests are sent repeatedly for the same address
  Then further requests are rejected with 429 Too Many Requests

**Notes**
- Every `String` field of the request DTOs must carry `@Size`: email 255, password 100.
- Endpoints must be excluded from authentication in the Spring Security configuration.
- Priority P1 - Effort XL (full stack) - Part 3 of 4.
