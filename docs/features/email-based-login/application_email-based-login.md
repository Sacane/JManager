# Application Module — Login by Email

**Context**
The REST login endpoint (`POST /api/user/auth`) currently accepts a `username` field in `UserPasswordDTO`. With the domain now accepting an email-based `LoginCommand`, the DTO and the controller dispatch must be updated to pass `email` instead of `username`.

**Acceptance Criteria**
Feature: REST login endpoint accepts email
  In order to authenticate via email
  As an API client
  I want to POST an email and password to /api/user/auth and receive a valid session

Scenario: Successful login via REST with valid email
  Given a registered user with email "alice@example.com" and a valid password
  When the client POSTs { "email": "alice@example.com", "password": "<correct>" } to /api/user/auth
  Then the response is HTTP 200
  And the response body contains a valid access token and refresh token
  And HTTP-only cookies "token" and "refresh_token" are set

Scenario: Login with unknown email returns 404
  Given no user with email "ghost@example.com"
  When the client POSTs { "email": "ghost@example.com", "password": "any" } to /api/user/auth
  Then the response is HTTP 404

Scenario: Login with wrong password returns 401
  Given a registered user with email "alice@example.com"
  When the client POSTs { "email": "alice@example.com", "password": "<wrong>" } to /api/user/auth
  Then the response is HTTP 401

Scenario: Login with malformed email is rejected before reaching the domain
  When the client POSTs { "email": "not-an-email", "password": "any" } to /api/user/auth
  Then the response is HTTP 400

**Implementation Plan**
1. **`UserPasswordDTO`** — rename field `username: String` → `email: String`; add `@field:Email` and `@field:Size(max = 255)` constraints.

2. **`SessionController.login()`** — update `LoginCommand` construction:
   ```kotlin
   LoginCommand(email = userDTO.email, userPassword = userDTO.password)
   ```

3. **Input validation** — ensure `@Validated` / `@Valid` is already present on the controller parameter (it should be); `@field:Email` on the DTO field is sufficient.
