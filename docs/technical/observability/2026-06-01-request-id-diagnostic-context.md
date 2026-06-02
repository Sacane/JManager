# Request-ID Diagnostic Context in Error Responses

> **Topic**: Observability — per-request correlation ID in API error responses
> **Date**: 2026-06-01
> **Author**: Technical Backend

---

## Context
When an API error occurs, users must be able to copy a stable identifier and send it to support so
the failing request can be located in logs and correlated to database state. Currently, error
responses expose only a numeric `code` and a message — there is no request-scoped identifier.

## Current State

### MDC infrastructure (domain + application)
- `MdcContextProvider` / `MdcKeys` (`BOOKLET_ID`, `TRANSACTION_ID`) — domain, no Spring dependency.
- `LoggingCommandBus` / `LoggingQueryBus` — inject those keys into SLF4J MDC for the duration of
  the bus dispatch, then **remove them in the `finally` block** before the result (or exception)
  returns to the controller.
- Consequence: by the time an exception reaches `ProblemDetailHandler`, the domain MDC keys have
  already been cleared. They **cannot** be forwarded to the error response from the handler.

### Spring Security filter chain
`SecurityConfig` registers `JwtCookieAuthenticationFilter` before
`UsernamePasswordAuthenticationFilter`. There is no existing request-scoped ID filter.

### Error response shape
`ProblemDetailHandler.buildResponse()` produces a Spring `ProblemDetail` with `code` and `errorKey`
custom properties. No `requestId` or `userId` today.

### User identity
`JmanagerUserAuthDetail` (carrying `id: UUID`) is stored as the principal in
`SecurityContextHolder`. A safe null-capable read is needed in the handler to avoid throwing
`UnauthorizedRequestException` for unauthenticated error paths.

## Analysis

### Why a servlet-level filter, not a Security chain filter
Adding `requestId` injection inside the Spring Security filter chain (via `addFilterBefore` in
`SecurityConfig`) would work for API requests, but the filter would only be active within the
security chain. Registering the filter as a `@Component` with `@Order(Ordered.HIGHEST_PRECEDENCE)`
at the servlet container level means it runs **before** the `DelegatingFilterProxy` and therefore
before Spring Security itself. This guarantees `requestId` is in the MDC for every request:
authentication failures, CORS pre-flights, and Spring Security's own exceptions.

### Why the `finally` block in the bus does NOT need to change
The bus removes domain MDC keys (`bookletId`, `transactionId`) after dispatch — that is correct
behaviour (prevents context leaks across dispatches). The `requestId` is set independently at the
HTTP boundary, outside the bus lifecycle, so it survives the bus `finally` block and is still
present in the MDC when `ProblemDetailHandler` runs.

Support flow: grep logs by `requestId` → find all log lines for that request, including those
emitted during the bus dispatch **before** the `finally` removed `bookletId`/`transactionId`.

### Security consideration
Exposing `userId` in error responses is acceptable for this application (authenticated users
reporting their own errors). It must be omitted for unauthenticated paths (401/403 on public
endpoints) to avoid leaking internal identifiers to anonymous callers.

## Recommended Approach

### Step 1 — `MdcKeys.REQUEST_ID` constant (domain)
Add `REQUEST_ID = "requestId"` to the existing `MdcKeys` object in
`domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/MdcContext.kt`.
No Spring dependency — the domain only holds the string constant.

### Step 2 — `RequestIdFilter` (application)

```kotlin
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestIdFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        MDC.put(MdcKeys.REQUEST_ID, UUID.randomUUID().toString())
        try {
            chain.doFilter(request, response)
        } finally {
            MDC.remove(MdcKeys.REQUEST_ID)
        }
    }
}
```

`@Order(Ordered.HIGHEST_PRECEDENCE)` places it as the first servlet filter, before Spring Security.
No `FilterRegistrationBean` needed — Spring Boot auto-registers `@Component` servlet filters.

### Step 3 — `ProblemDetailHandler.buildResponse()` (application)

Add two property injections at the end of `buildResponse()`:

```kotlin
MDC.get(MdcKeys.REQUEST_ID)?.let { problemDetail.setProperty("requestId", it) }
currentUserIdOrNull()?.let { problemDetail.setProperty("userId", it.toString()) }
```

Add a private safe-read helper (no exception for unauthenticated paths):

```kotlin
private fun currentUserIdOrNull(): UUID? =
    (SecurityContextHolder.getContext().authentication?.principal as? JmanagerUserAuthDetail)?.id
```

## Implementation Notes

| File | Change |
|---|---|
| `domain/.../MdcContext.kt` | Add `REQUEST_ID = "requestId"` to `MdcKeys` |
| `application/.../api/RequestIdFilter.kt` | New file — `OncePerRequestFilter` with `@Component @Order(HIGHEST_PRECEDENCE)` |
| `application/.../api/ProblemDetailHandler.kt` | `buildResponse()` reads MDC + SecurityContext; add `currentUserIdOrNull()` helper |

No new Gradle dependency required. All needed types (`OncePerRequestFilter`, `MDC`,
`SecurityContextHolder`, `Ordered`) are already on the classpath via `spring-boot-starter-web` and
`spring-boot-starter-security`.

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| MDC leak between requests in thread-pool | High | `finally` in `RequestIdFilter` removes the key unconditionally — safe |
| `userId` exposed in 4xx responses | Low | `currentUserIdOrNull()` returns `null` for unauthenticated paths — omitted |
| `requestId` absent if filter not reached (e.g. container-level 400) | Low | `MDC.get()` returns `null` → `setProperty` skipped, no NPE |
| Filter order conflicts with future filters | Low | `HIGHEST_PRECEDENCE` is idiomatic for cross-cutting servlet filters |

## References
- [SLF4J MDC documentation](https://logback.qos.ch/manual/mdc.html)
- [Spring `OncePerRequestFilter` Javadoc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html)
- [Spring Boot filter ordering](https://docs.spring.io/spring-boot/docs/current/reference/html/web.html#web.servlet.embedded-container.customizing.samesite)
