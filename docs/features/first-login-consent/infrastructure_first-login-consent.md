# Infrastructure Module — First-Login Consent Collection

## Context

The `ConsentRecord` aggregate field (added in GDPR P4) is persisted via the three Flyway V24
columns (`tos_accepted_at`, `tos_version`, `privacy_accepted_at`) on `user_resource`.

Admin-created accounts have these columns set to `NULL`. The infrastructure adapter must expose
a `recordConsent()` method that sets these columns for a given user, and the `toModel()` mapper
must already propagate them (done in P4).

---

## Acceptance Criteria

```gherkin
Feature: Persisting first-login consent

  In order to satisfy GDPR Art. 6 traceability requirements
  As the infrastructure persistence adapter
  I want to update the three consent columns for an existing user

  Scenario: Record consent for an existing user
    Given a user persisted with tos_accepted_at = NULL
    When recordConsent is called with valid ConsentRecord
    Then tos_accepted_at, tos_version, and privacy_accepted_at are persisted
    And findUserById returns a User with a non-null ConsentRecord

  Scenario: Record consent for an unknown user returns failure
    Given no user exists for the given UserId
    When recordConsent is called
    Then the result is a failure with state USER_NOT_FOUND
```

---

## Implementation Plan

1. **`UserRepository.recordConsent()`** adapter in `UserRepositoryJpaAdapter`:
   ```kotlin
   @Transactional
   override fun recordConsent(userId: UserId, consent: ConsentRecord): Result<Unit> {
       val id = userId.value ?: return failure(USER_NOT_FOUND, ...)
       val resource = userPostgresRepository.findById(id).orElse(null)
           ?: return failure(USER_NOT_FOUND, ...)
       resource.tosAcceptedAt = consent.tosAcceptedAt
       resource.tosVersion = consent.tosVersion
       resource.privacyAcceptedAt = consent.privacyAcceptedAt
       userPostgresRepository.save(resource)
       return success(Unit)
   }
   ```

2. **`UserRepositoryJpaAdapterTest`** — two integration tests:
   - `recordConsent should persist all three columns`
   - `recordConsent on unknown user should return USER_NOT_FOUND`
