# Application Module — Reset endpoints, rate limits and session revocation

**Context**
Expose the reset flow, protect it from abuse, and make a password change actually end older sessions.
Read against the code on 21 September 2026:

- **Sessions are not checked per request.** `JwtCookieAuthenticationFilter` accepts any correctly signed
  JWT whose user still exists; the in-memory sessions of `SessionManager` are never consulted. A stolen
  access token therefore survives a password change until it expires (1 h), and a refresh token mints
  new ones for longer. The filter already loads the user from the database on every request, so
  comparing the token's `iat` with `credentialsChangedAt` costs no extra query.
- **Rate limiting** exists only for sign-in: `LoginRateLimiter`, in memory, keyed on
  `HttpServletRequest.remoteAddr`, counting failures. Production runs **behind a reverse proxy** and no
  `server.forward-headers-strategy` is set, so `remoteAddr` is most likely the proxy's address for every
  visitor — which would make the sign-in limit global. Not confirmed from here: the limiter logs the IP
  it blocks, and one production log line settles it. The proxy is nginx 1.21.6 and **appends** the client
  address (`proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for`), checked on the server on
  21 September 2026.
- **Public routes** are listed in `SecurityConfig`, and page routes must also be listed in
  `SpaController`. Pages outside that second list currently answer 401 when opened from a link — see
  `docs/bugs/spa-pages-401-on-direct-load/`. **That bug must be fixed before this module ships**, or
  the reset link fails exactly as the verification link does today.

**Endpoints**

| Method and path | Body | Answers |
|---|---|---|
| `POST /api/password-reset/request` | `{ email }` | **202** always · 429 when rate limited |
| `POST /api/password-reset/validate` | `{ token }` | 204 valid · 400 `token_invalid` / `token_expired` · 429 |
| `POST /api/password-reset/confirm` | `{ token, newPassword, confirmPassword }` | 204 · 400 with the domain error key · 429 |

The token travels in the body, never in the path or the query string, so it never reaches access logs.

**Acceptance Criteria**
Feature: Password reset endpoints
  In order to reset my password from the client
  As an anonymous visitor
  I want endpoints to request, check and confirm a reset

Scenario: 1. Requesting a reset answers the same for any address
  Given an anonymous visitor
  When a reset is requested for a registered address and then for an unknown one
  Then both answers are 202 with an identical empty body

Scenario: 2. A valid token is reported valid
  Given a valid reset token
  When it is validated
  Then the answer is 204 and the token remains usable

Scenario: 3. A stale token is reported with its reason
  Given an expired token and an unknown token
  When each is validated
  Then the answers are 400 with the error keys "token_expired" and "token_invalid"

Scenario: 4. Confirming with a valid token succeeds
  Given a valid reset token
  When the reset is confirmed with a compliant password and its confirmation
  Then the answer is 204 and the user can sign in with the new password

Scenario: 5. Domain failures keep their error keys
  Given a valid reset token
  When the reset is confirmed with mismatched passwords, then with a non-compliant password
  Then the answers are 400 carrying the mismatch key, then the password policy key

Scenario: 6. The request endpoint is limited per address
  Given three reset requests for the same address within an hour
  When a fourth request for that address arrives
  Then the answer is 429 whether or not the address is registered

Scenario: 7. The request endpoint is limited per client
  Given five reset requests from the same client within 15 minutes
  When a sixth request arrives from that client
  Then the answer is 429

Scenario: 8. The token endpoints are limited per client
  Given ten validate or confirm calls from the same client within 15 minutes
  When another call arrives from that client
  Then the answer is 429

Scenario: 9. A token issued before a password change is refused
  Given a user whose credentialsChangedAt is after the issue time of an access token
  When a request is made with that access token
  Then the answer is 401

Scenario: 10. A token without issue time is refused once the password has changed
  Given an access token issued before this release, with no issue time
  And a user whose credentialsChangedAt is set
  When a request is made with that token
  Then the answer is 401

Scenario: 11. Nothing changes for users who never changed their password
  Given a user whose credentialsChangedAt is empty
  When a request is made with an access token issued before this release
  Then the request is authenticated as before

Scenario: 12. The device that changes its password stays signed in
  Given a signed-in user
  When that user changes their password in the settings, or completes a forced change
  Then the answer sets fresh access and refresh cookies, and the next request is authenticated

Scenario: 13. The reset pages and endpoints are public
  Given an anonymous visitor
  When "/forgot-password" and "/reset-password" are opened directly and the three endpoints are called
  Then none is refused for lack of authentication

**Notes**
- DTOs: `email` `@NotBlank @Size(max = 255)`, `token` `@NotBlank @Size(max = 64)`, passwords
  `@NotBlank @Size(max = 100)`. The **minimum** length is the domain policy's job (UX-27), not `@Size`:
  one rule, one place.
- Scenario 6 counts **requests**, not sends, so a 429 reveals nothing about the address. It exists to
  stop anyone flooding a victim's mailbox; 7 and 8 stop enumeration and brute force from one client.
- Generalise `LoginRateLimiter` into a keyed sliding-window limiter rather than writing a second one;
  sign-in keeps counting failures, the reset counts every request.
- **Client IP behind the proxy**: set `server.forward-headers-strategy=native`. nginx **appends** the
  client address, so anything the client put in `X-Forwarded-For` stays on the left. Tomcat reads the
  header from the right and skips trusted proxies, so the forged part is ignored — **provided nginx's
  address, as Spring sees it, is a trusted proxy**. By default that means a private range (10/8,
  172.16/12, 192.168/16, 127/8); a public address needs `server.tomcat.remoteip.internal-proxies`.
  Never use the `framework` strategy here: it reads the header from the left and would honour the
  forged value. The behaviour lives in the embedded server, so test it against a real one
  (`RANDOM_PORT`), not MockMvc, which bypasses it. This also repairs the sign-in limiter if it is global
  today.
- `iat` has second precision. A token is refused when `iat < credentialsChangedAt` with the change time
  **truncated to the second**, so the cookies re-issued in scenario 12 — in the same second as the
  change — are accepted. The cost is a window of under one second in which an older token issued that
  same second would also pass; refresh tokens are revoked at the change, so none can be minted in it.
- `ProblemDetail` carries `code` and `errorKey`, as every other endpoint does.
- Priority P1 · Effort XL (full stack) · Part 3 of 4.
