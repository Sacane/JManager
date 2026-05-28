# Application Module — First-Login Consent Collection

## Context

After login, the application layer must check whether the authenticated user has already given
consent. If `HasUserConsentedQuery` returns `false`, the API signals the frontend to display
the consent gate. A `POST /api/user/consent` endpoint records acceptance and unlocks access.

Admin-created beta test accounts (consent = null) are the primary target, but any future
account with missing consent will follow the same path.

---

## Acceptance Criteria

```gherkin
Feature: First-login consent API

  In order to legally collect GDPR consent from admin-created users
  As an authenticated user without existing consent
  I want a dedicated API endpoint to submit my acceptance of TOS and Privacy Policy

  Scenario: Submit consent successfully
    Given an authenticated user with consent = null
    When POST /api/user/consent with { tosAccepted: true, tosVersion: "1.0", privacyAccepted: true }
    Then the response status is 204 No Content
    And GET /api/user/settings returns the user's consent timestamps

  Scenario: Submit consent with TOS not accepted returns 400
    Given an authenticated user with consent = null
    When POST /api/user/consent with { tosAccepted: false, privacyAccepted: true }
    Then the response status is 400 Bad Request
    And the error key is "domain.user.consent.tos_required"

  Scenario: Submit consent with privacy policy not accepted returns 400
    Given an authenticated user with consent = null
    When POST /api/user/consent with { tosAccepted: true, privacyAccepted: false }
    Then the response status is 400 Bad Request
    And the error key is "domain.user.consent.privacy_required"

  Scenario: Submit consent without authentication returns 401
    Given an unauthenticated request
    When POST /api/user/consent
    Then the response status is 401 Unauthorized

  Scenario: GET /api/user/me returns consentRequired flag
    Given an authenticated user with consent = null
    When GET /api/user/me
    Then the response body contains { consentRequired: true }

  Scenario: GET /api/user/me returns consentRequired false after consent
    Given an authenticated user with a non-null ConsentRecord
    When GET /api/user/me
    Then the response body contains { consentRequired: false }
```

---

## Implementation Plan

### New endpoint: `POST /api/user/consent`

Add to `SessionController`:
```kotlin
@PostMapping(path = ["/consent"], consumes = [MediaType.APPLICATION_JSON_VALUE])
fun recordConsent(@Valid @RequestBody dto: RecordConsentDTO): ResponseEntity<Nothing> {
    val now = LocalDateTime.now()
    return commandBus.dispatch(
        RecordConsentCommand(
            userId = UserId(currentUser.id),
            tosAccepted = dto.tosAccepted,
            tosVersion = dto.tosVersion,
            privacyAccepted = dto.privacyAccepted,
            consentTimestamp = now,
        )
    ).toHttpResponse()
}
```

### New DTO: `RecordConsentDTO`
```kotlin
data class RecordConsentDTO(
    val tosAccepted: Boolean = false,
    @field:Size(max = 20)
    val tosVersion: String? = null,
    val privacyAccepted: Boolean = false,
)
```

### Security config

Add to `SecurityConfig`:
```kotlin
authorize("/api/user/consent", authenticated)
```

### `GET /api/user/me` enrichment (optional but recommended)

Return a lightweight `UserStatusDTO` so the frontend can detect consent state immediately
after login without a second request:
```kotlin
data class UserStatusDTO(
    val consentRequired: Boolean,
)
```

### `SessionControllerTest` — new nested class `ConsentEndpointTest`
- `POST /consent with both accepted must return 204`
- `POST /consent with tosAccepted = false must return 400`
- `POST /consent with privacyAccepted = false must return 400`
- `POST /consent without token must return 401`
