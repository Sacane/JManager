# Feature Flag — Application Layer

> **Topic**: REST endpoints, caching, and security wiring for the feature flag system
> **Date**: 2026-05-29
> **Author**: Technical Backend

---

## Context

The domain and infrastructure layers of the feature flag system are complete. This layer exposes two REST
endpoints: a **public** read endpoint the Nuxt frontend calls at boot, and an **admin-only** toggle endpoint.
Two non-trivial technical concerns must be resolved: where to place the cache for flag reads, and how to wire
the unknown-key 400 error without polluting the domain with HTTP concerns.

## Current State

- `SecurityConfig` already has a catch-all `authorize("/api/admin/**", hasRole("ADMIN"))` rule — the toggle
  endpoint fits under it with no new security rule needed.
- `CacheConfig` uses Caffeine with named caches declared as beans; adding a new cache requires one line.
- `CommandBus` / `QueryBus` dispatch by class type; `GetAllFeatureFlagsQuery` (no-arg class) and
  `ToggleFeatureFlagCommand` are already registered via `@DomainService` scan.
- `toHttpResponse()` maps domain `Result` states to HTTP exceptions — no `FeatureKey`-specific state exists,
  so unknown-key parsing is handled defensively in the controller before dispatch.

## Analysis

### Caching

`GET /api/feature-flags` is the highest-frequency read in the feature flag system — the frontend fetches it
at every boot and potentially on route change. Flags change rarely (admin-driven). A **short TTL Caffeine
cache** on `FeatureFlagRepositoryJpaAdapter.findAll()` is the right level:

- Consistent with existing caching pattern (`@Cacheable` on `UserRepositoryJpaAdapter.findUserByIdWithBooklets`).
- Cache lives in the adapter (infrastructure) not the controller (application) — correct hexagonal placement.
- `@CacheEvict(allEntries = true)` on `upsert()` ensures the cache is invalidated immediately on every toggle.
- **TTL**: 5 minutes. Staleness is acceptable — flags rarely change, and an admin who just toggled gets an
  immediate eviction via `upsert()` anyway.
- No distributed cache (Redis) needed: flag data is tiny, read-after-write consistency is guaranteed by
  eviction on write, and a single-instance Caffeine cache suffices for the current deployment topology.

### Security

`GET /api/feature-flags` must be **public** — the frontend fetches it at boot, before authentication. A new
`authorize("/api/feature-flags", permitAll)` rule must be inserted **before** the catch-all
`authorize("/api/**", authenticated)` in `SecurityConfig`. Order is significant in Spring Security's rule list.

`PATCH /api/admin/feature-flags/{key}` falls under the existing `authorize("/api/admin/**", hasRole("ADMIN"))`
rule — no new security config required.

### Unknown-key parsing

The domain `ToggleFeatureFlagCommand` takes a `FeatureKey` (enum). When the HTTP path variable `{key}` does
not match any `FeatureKey` entry, the controller must return 400 before dispatching. This is a pure HTTP
concern; the domain never sees the invalid string. The existing `InvalidRequestException` is used for this.

### Response shape

`List<FeatureFlagDTO>` (not `Map<String, Boolean>`) allows adding fields (e.g. `description`) without a
breaking change, and matches the REST resource convention used by all other controllers.

## Recommended Approach

```kotlin
// FeatureFlagDTO.kt
data class FeatureFlagDTO(val key: String, val enabled: Boolean)
data class ToggleFeatureFlagRequest(val enabled: Boolean)

// FeatureFlagController.kt — two endpoints
@GetMapping               // public  — GET /api/feature-flags
@PatchMapping("/{key}")   // admin   — PATCH /api/admin/feature-flags/{key}
```

```yaml
# SecurityConfig addition (before /api/** authenticated)
authorize("/api/feature-flags", permitAll)
```

```kotlin
// CacheConfig addition
buildCache("featureFlags", maximumSize = 1, expireAfterWrite = Duration.ofMinutes(5))

// FeatureFlagRepositoryJpaAdapter
@Cacheable("featureFlags")
override fun findAll(): List<FeatureFlag> = ...

@CacheEvict("featureFlags", allEntries = true)
override fun upsert(flag: FeatureFlag): FeatureFlag = ...
```

### Why this approach

- **Public `GET`**: The frontend composable fetches flags at boot without credentials — making this endpoint
  authenticated would break the client before login.
- **Cache at adapter level**: Consistent with the project's existing pattern. Placing the cache at the
  controller would bypass it when the domain calls `findAll()` directly (future use cases).
- **`List` not `Map`**: Extensible without breaking JSON consumers.
- **`PATCH` not `PUT`**: Toggling is a partial update to a resource (the flag's `enabled` field), not a full
  replacement — `PATCH` is semantically correct.

## Implementation Notes

1. Add `"featureFlags"` cache to `CacheConfig`.
2. Add `@Cacheable` / `@CacheEvict` to `FeatureFlagRepositoryJpaAdapter` (infrastructure, already created).
3. Add `authorize("/api/feature-flags", permitAll)` to `SecurityConfig` before `/api/**`.
4. Create `application/src/main/kotlin/.../api/featureflag/FeatureFlagController.kt` with both endpoints.
5. Create `FeatureFlagDTO` and `ToggleFeatureFlagRequest` DTOs in the same package.
6. Integration tests with RestAssured + Testcontainers extending `AuthenticatedUserTest`.

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| Caffeine cache is in-process — two app instances can have stale state for up to 5 min | Low | Acceptable for current single-instance topology; switch to Redis when horizontally scaled |
| Unknown `FeatureKey` string returns 400 but is not logged — hard to debug bad client calls | Low | Add warn-level log in the controller before throwing |
| `GetAllFeatureFlagsQuery` is a no-arg class — bus lookup relies on `query::class.java` | None | Verified: SpringQueryBus uses `query::class.java` correctly for concrete instances |

## References
- [Spring Security — `authorizeHttpRequests` ordering](https://docs.spring.io/spring-security/reference/servlet/authorization/authorize-http-requests.html)
- [Spring Cache — `@CacheEvict` allEntries](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/annotation/CacheEvict.html)
- [RFC 5789 — PATCH method](https://www.rfc-editor.org/rfc/rfc5789)
