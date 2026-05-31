# Domain Module — Login by Email

**Context**
Users currently authenticate with a `pseudonym` (username). The goal is to replace the login identifier with the user's email address so that credentials are more intuitive and consistent with modern application conventions. The domain `LoginCommand` and the `UserRepository` port must be updated accordingly, and the `LoginService` must be adjusted to look up users by email.

**Acceptance Criteria**
Feature: Login by email address
  In order to access my personal finance data with a familiar identifier
  As a registered user
  I want to log in using my email address instead of my pseudonym

Scenario: Successful login with email
  Given a registered user with email "alice@example.com" and a valid password
  When the system handles a LoginCommand with email "alice@example.com" and the correct password
  Then the system returns a UserToken containing an access token and a refresh token

Scenario: Login with correct email but wrong password
  Given a registered user with email "alice@example.com"
  When the system handles a LoginCommand with email "alice@example.com" and an incorrect password
  Then the system returns an unauthorized failure

Scenario: Login with unknown email
  Given no user exists with email "ghost@example.com"
  When the system handles a LoginCommand with email "ghost@example.com"
  Then the system returns a not-found failure

**Implementation Plan**
1. **`LoginCommand`** (`domain/port/input/user/LoginUseCase.kt`)
   - Rename field `pseudonym: String` → `email: String`.

2. **`UserRepository` port** (`domain/port/output/UserRepository.kt`)
   - Add: `fun findByEmailWithEncodedPassword(email: String): UserWithPassword?`
   - The existing `findByPseudonymWithEncodedPassword` can be kept if still used elsewhere (admin bootstrap), or removed if not.

3. **`LoginService`** (`domain/port/input/user/LoginUseCase.kt` or adjacent service file)
   - Replace call to `userRepository.findByPseudonymWithEncodedPassword(command.pseudonym)`
     with `userRepository.findByEmailWithEncodedPassword(command.email)`.
