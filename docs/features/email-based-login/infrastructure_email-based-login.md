# Infrastructure Module — Login by Email

**Context**
The domain port `UserRepository` will expose a new method `findByEmailWithEncodedPassword(email: String): UserWithPassword?`. The persistence adapter (JPA) must implement this method so that the authentication flow can look up a user by their email address.

**Acceptance Criteria**
Feature: Persist and retrieve users by email for login
  In order to support email-based authentication
  As the system
  I want to fetch a user with their encoded password using their email address

Scenario: Find existing user by email
  Given a persisted user with email "alice@example.com" and a hashed password
  When the adapter calls findByEmailWithEncodedPassword("alice@example.com")
  Then it returns a UserWithPassword containing the User and the stored hashed password

Scenario: Unknown email returns null
  Given no persisted user with email "ghost@example.com"
  When the adapter calls findByEmailWithEncodedPassword("ghost@example.com")
  Then it returns null

**Implementation Plan**
1. **Spring Data repository** — add a derived query method:
   ```kotlin
   fun findByEmail(email: String): UserEntity?
   ```
   (or a `@Query` if the existing schema stores email on a related table).

2. **Persistence adapter** — implement `findByEmailWithEncodedPassword`:
   - Call the Spring Data method above.
   - Map `UserEntity → UserWithPassword` using the existing mapper.

3. **Verify email uniqueness constraint** — confirm `email` column has a `UNIQUE` constraint in the JPA entity / Flyway migration. Add one if missing.
