# Domain Module — First-Login Consent Collection

## Context

Beta test accounts are created by an admin, bypassing the self-registration flow where consent
is normally collected. Under GDPR Art. 6 and Art. 7, consent must come from the data subject
themselves — an admin cannot accept on their behalf.

The existing `ConsentRecord` on the `User` aggregate is `null` for admin-created accounts.
The domain needs a dedicated use case that records the user's explicit consent at first login,
setting `tosAcceptedAt`, `tosVersion`, and `privacyAcceptedAt` on the aggregate.

A new `RecordConsentCommand` handles this. A guard query (`HasUserConsentedQuery`) allows the
application layer to determine whether the consent gate should be shown before granting access.

---

## Acceptance Criteria

```gherkin
Feature: First-login consent collection

  In order to comply with GDPR Art. 6 (lawful basis) and Art. 7 (consent conditions)
  As a beta-test user whose account was created by an admin
  I want to explicitly accept the Terms of Service and Privacy Policy at first login
  So that my consent is recorded with a server-side timestamp under my own identity

  Scenario: User records consent successfully
    Given an authenticated user with consent = null
    When the user submits RecordConsentCommand with tosAccepted = true and privacyAccepted = true
    Then the result is a success
    And the user's ConsentRecord is persisted with a non-null tosAcceptedAt and privacyAcceptedAt

  Scenario: User attempts to record consent with TOS not accepted
    Given an authenticated user with consent = null
    When the user submits RecordConsentCommand with tosAccepted = false
    Then the result is a failure with state INVALID
    And the error key is "domain.user.consent.tos_required"

  Scenario: User attempts to record consent with privacy policy not accepted
    Given an authenticated user with consent = null
    When the user submits RecordConsentCommand with privacyAccepted = false
    Then the result is a failure with state INVALID
    And the error key is "domain.user.consent.privacy_required"

  Scenario: Consent query returns false for user without consent
    Given an authenticated user with consent = null
    When HasUserConsentedQuery is dispatched for that user
    Then the result is false

  Scenario: Consent query returns true for user who already consented
    Given an authenticated user with a non-null ConsentRecord
    When HasUserConsentedQuery is dispatched for that user
    Then the result is true

  Scenario: Admin-created user cannot access the app before consenting
    Given an authenticated user with consent = null
    When HasUserConsentedQuery is dispatched
    Then the application layer must redirect the user to the consent gate
```

---

## Implementation Plan

1. **`RecordConsentCommand`** — new command in `domain/port/input/user/`:
   ```kotlin
   data class RecordConsentCommand(
       val userId: UserId,
       val tosAccepted: Boolean,
       val tosVersion: String?,
       val privacyAccepted: Boolean,
       val consentTimestamp: LocalDateTime,   // provided by application layer (server-side now())
   ) : Command<Unit>
   ```

2. **`RecordConsentService`** — validates both flags are `true`, then calls
   `userRepository.recordConsent(userId, ConsentRecord(...))`.

3. **`HasUserConsentedQuery`** — new query returning `Boolean`:
   ```kotlin
   data class HasUserConsentedQuery(val userId: UserId) : Query<Boolean>
   ```
   The handler reads `userRepository.findUserById(userId)?.consent != null`.

4. **`UserRepository` port** — new method:
   ```kotlin
   fun recordConsent(userId: UserId, consent: ConsentRecord): Result<Unit>
   ```

5. **`InMemoryUserRepository`** — implement `recordConsent()` for fake.

6. **`UserFixture`** — add helper `aUserWithConsent()` for test setup.
