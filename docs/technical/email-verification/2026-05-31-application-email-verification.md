# Email Verification — Application Layer

> **Topic**: REST endpoints + security wiring + user payload
> **Date**: 2026-05-31
> **Author**: Technical Backend

---

## Context

The domain use cases `VerifyEmailUseCase` and `ResendVerificationEmailUseCase` are wired. This report covers exposing them via REST, mapping the two new `ResultState` values to the correct HTTP codes (410 Gone, 409 Conflict), opening the public verify endpoint in `SecurityConfig`, and surfacing `emailVerified` on the authenticated user status payload.

## Current State

- `SessionController` at `api/user/**` handles login, logout, refresh, consent, settings.
- `toHttpResponse()` in `ApiMappingExtensions.kt` maps `ResultState` to HTTP exceptions — it does not yet handle `EMAIL_VERIFICATION_TOKEN_EXPIRED` or `EMAIL_ALREADY_VERIFIED`.
- `GET /api/user/me` returns `UserStatusDTO(consentRequired)` via `HasUserConsentedQuery`.
- `SecurityConfig` has an explicit allowlist; any new public endpoint must be explicitly permitted.
- No `GoneException` (410) exists yet — only `NotFoundException` (404) and `ConflictException` (409).

## Analysis

### HTTP status mapping

| Domain `ResultState` | HTTP code | Rationale |
|---|---|---|
| `EMAIL_VERIFICATION_TOKEN_EXPIRED` | 410 Gone | The resource (token) existed but is permanently expired — 410 is semantically correct and lets the client distinguish expiry from "never existed" |
| `EMAIL_ALREADY_VERIFIED` | 409 Conflict | A business invariant conflict: resending to an already-verified address |

### Public endpoint — security concern

`GET /api/verify-email?token=` must be public (no auth cookie required — the user may not be logged in when they click the email link). Adding it to the `permitAll` list in `SecurityConfig` is the only required change. The endpoint accepts a query param — no request body to validate, so no CSRF concern (GET is read-only in HTTP semantics even though it mutates state here; the token acts as the CSRF protection itself).

### Surfacing `emailVerified` on `/me`

`GET /api/user/me` calls `HasUserConsentedQuery` which loads the user and returns `Boolean`. Adding a second query `GetUserEmailVerifiedQuery` keeps the domain interface focused and avoids changing existing use case contracts. Two queries = two DB reads, but `/me` is not a hot-path endpoint — acceptable.

### Controller placement

The two new endpoints belong in a dedicated `EmailVerificationController` rather than `SessionController` to respect SRP: session lifecycle vs. email verification lifecycle are different concerns.

## Recommended Approach

```kotlin
// GET /api/verify-email?token=  →  200 / 410 / 404
// POST /api/verify-email/resend →  200 / 409
@RestController
@RequestMapping("api/verify-email")
class EmailVerificationController(private val commandBus: CommandBus) {

    @GetMapping
    fun verify(@RequestParam token: String): ResponseEntity<Void> =
        commandBus.dispatch(VerifyEmailCommand(token))
            .map { null as Void? }
            .toHttpResponse()

    @PostMapping("/resend")
    fun resend(): ResponseEntity<Void> =
        commandBus.dispatch(ResendVerificationEmailCommand(UserId(currentUser.id)))
            .map { null as Void? }
            .toHttpResponse()
}
```

## Implementation Notes

1. `GoneException` — new exception class in `Exception.kt`
2. `ProblemDetailHandler` — add `@ExceptionHandler(GoneException::class)` → 410
3. `ApiMappingExtensions.toHttpResponse()` — add `EMAIL_VERIFICATION_TOKEN_EXPIRED → GoneException`, `EMAIL_ALREADY_VERIFIED → ConflictException`
4. `SecurityConfig` — add `authorize("/api/verify-email", permitAll)`
5. Domain: add thin `GetUserEmailVerifiedQuery` + service (minimal, follows `HasUserConsentedUseCase` pattern)
6. `UserStatusDTO` — add `emailVerified: Boolean`
7. `SessionController.getUserStatus()` — call both queries, combine into `UserStatusDTO`
8. `EmailVerificationController` — new file

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| Two DB reads in `/me` | Low | `/me` is lightweight; not a hot path |
| GET endpoint mutating state | Low | Industry standard for email verification links; token is the anti-CSRF mechanism |
| 410 vs 400 for expired token | Low | 410 is semantically correct and enables the client to distinguish cases cleanly |

## References
- [RFC 7231 §6.5.9 — 410 Gone](https://www.rfc-editor.org/rfc/rfc7231#section-6.5.9)
- [Spring Security `permitAll` vs `anonymous`](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
