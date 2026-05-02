# Caching Strategy — Candidate Analysis

> **Topic**: Caching — identification and optimisation recommendations  
> **Date**: 2026-05-02  
> **Author**: Technical Backend Skill

---

## Context

The goal is to identify existing backend calls that can be cached without functional regression,
in order to reduce the load on PostgreSQL and improve perceived response times for the frontend.
The project currently has no caching dependency (no Spring Cache, no Caffeine, no Redis).

---

## Current State

- **No cache** is configured in the project.
- `application/build.gradle.kts`: `spring-boot-starter-cache` and `caffeine` are absent.
- `infrastructure/build.gradle.kts`: same.
- Every HTTP call unconditionally triggers a SQL query, even for near-static data
  (default tags, booklet list, user settings).

---

## Analysis

### Cache eligibility criteria

For each use case, the decision rests on four axes:

| Axis | Question |
|---|---|
| **Read frequency** | Is it read often within a session? |
| **Mutability** | Is it written/modified frequently? |
| **Scope** | Shared (global) or isolated (per user)? |
| **Invalidation** | Do we know precisely when to evict? |

---

### Identified candidates

#### ✅ STRONG CANDIDATE — `DefaultTag` (system default tag)

**Use case**: `DefaultTagQuery` → `TagRepository.defaultTag()`

**Critical observation**: in `StatsController.getPrevisionalTransactions`, the method
dispatches the QueryBus **twice** in sequence:
```kotlin
// Call 1 — DefaultTagQuery (SQL query on default_tag table)
val defaultTag = queryBus.dispatch(DefaultTagQuery(UserId(currentUser.id))).mapNotNullOrFailure()
    ?: throw NotFoundException(...)
// Call 2 — GetPrevisionalTransactionsQuery (another SQL query)
return queryBus.dispatch(GetPrevisionalTransactionsQuery(...))
    .map { it.toDTO(defaultTag) }
    .toHttpResponse()
```
The default tag (`"No category"`) is a system singleton: it never changes after bootstrap.
This is the **most cost-effective** cache candidate in the project.

- **Frequency**: high — called on every `GET /api/stats/previsional` and `GET /api/tag/default`
- **Mutability**: none — created once at startup via `DataLoader`
- **Invalidation**: never required (or application restart)
- **Regression risk**: none

---

#### ✅ STRONG CANDIDATE — `GetAllTags` (personal + default tags)

**Use case**: `GetAllTagsQuery` → `TagRepository.getAllDefault(userId)`

A user's tag list only changes when an `AddTagCommand`, `DeleteTagCommand`, or `EditTagCommand`
is dispatched. Between these mutations, every frontend navigation to a page displaying a
transaction form triggers a `GET /api/tag`.

- **Frequency**: high — present on every transaction form
- **Mutability**: low — a user's tag list rarely changes within a session
- **Invalidation**: precise — the 3 mutating commands (`Add`, `Delete`, `Edit`) are known
- **Scope**: per `userId`
- **Regression risk**: low if eviction is correctly wired to the mutating commands

---

#### ✅ STRONG CANDIDATE — `FindAllRegisteredBooklets` (booklet list)

**Use case**: `FindAllRegisteredBookletsQuery` → `UserRepository.findUserByIdWithBooklets(userId)`

The user's booklet list is displayed in the sidebar on every page. It is loaded on every SPA
navigation and only changes on `SaveBookletCommand` or `DeleteBookletByIdCommand`.

- **Frequency**: very high — loaded on every page navigation (sidebar)
- **Mutability**: very low — booklets are rarely created or deleted
- **Invalidation**: precise — 2 known mutating commands (`Save`, `Delete`)
- **Scope**: per `userId`
- **Regression risk**: low if eviction is wired on `Save` and `Delete`

---

#### ✅ MEDIUM CANDIDATE — `GetUserSettings`

**Use case**: `GetUserSettingsQuery` → `UserRepository.findUserByIdWithBooklets(userId)`

Settings (`projectionWindowDays`, booklet monthly cycles) only change on an explicit user
update (`UpdateUserSettingsCommand`).

- **Frequency**: moderate — loaded on every user context initialisation
- **Mutability**: very low
- **Invalidation**: precise — only `UpdateUserSettingsCommand`
- **Scope**: per `userId`
- **Regression risk**: none if correctly invalidated

---

#### ⚠️ CONDITIONAL CANDIDATE — `GetMonthlyBookletStats` / `GetCategoryDistribution` / `GetTrendStats`

**Use cases**: stats endpoints `/api/stats/*`

These computations aggregate **all transactions** of a booklet or user. They are expensive
in both CPU and SQL. However:

- The query carries variable parameters: `year`, `startDate`, `endDate`, `bookletId`
- The cache key must cover `(userId, bookletId?, startDate?, endDate?, year?)`, which
  generates **many distinct entries**
- The underlying data changes on every `BookTransactionCommand`, `EditTransactionCommand`,
  or `DeleteTransactionsByIdsCommand` — frequent mutations

**Recommendation**: cache **only closed historical months/years** (prior to the current month),
never the current month. Example: stats for 2024 and 2025 are immutable; stats for May 2026
must not be cached as they evolve during the day.

- **Regression risk**: **HIGH** if the current month is cached — to be enabled only in Phase 2
  after validating that Phase 1 invalidations are stable

---

#### ❌ NOT A CANDIDATE — `LoadTransactionsForBookletForAMonth`

**Reason**: current-month transactions are modified very frequently (add, delete, edit).
The minimum viable TTL would be too short to be worthwhile, and fine-grained invalidation
by `(bookletId, month, year)` is complex to maintain correctly.

---

#### ❌ NOT A CANDIDATE — `GetAllRegularTransactions` (paginated)

**Reason**: pagination parameters (`pageNumber`, `pageSize`) make the cache ineffective
(too many distinct entries) for a data volume that remains low in this context.

---

## Recommended Approach

### Library choice: Caffeine (in-process local cache)

The project is deployed as a single instance (no cluster identified). A local **Caffeine**
cache via the **Spring Cache** abstraction is the optimal solution:
- Zero additional infrastructure (no Redis to operate)
- Sub-microsecond latency (JVM heap)
- Native Spring Boot integration via `spring-boot-starter-cache`

> If the project evolves toward a multi-instance deployment in the future, migrating to Redis
> will only require changing the `CacheManager` configuration; the `@Cacheable`/`@CacheEvict`
> annotations remain unchanged.

---

### Architecture: where to place the cache in the hexagon?

The cache **must not** contaminate the domain. It must sit in the **infrastructure** layer
as an SPI adapter decorating existing adapters — not in the application layer.

The cleanest and least invasive approach for this project is to place **Spring Cache annotations
on the SPI adapters** (infrastructure):

```
application → domain (port) → [cache decorator] → infrastructure adapter → PostgreSQL
```

Concretely: `@Cacheable` on the JPA adapter methods (`TagRepositoryJpaAdapter`,
`UserRepositoryJpaAdapter`, `BookletJpaRepositoryAdapter`), and `@CacheEvict` on the
corresponding mutation methods. The domain knows nothing about the cache.

---

### Phase 1 — Recommended implementation (low risk)

#### 1. Dependencies to add in `application/build.gradle.kts`

```kotlin
implementation("org.springframework.boot:spring-boot-starter-cache:${springBootVersion}")
implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")
```

#### 2. `CacheManager` configuration (`application/` layer)

```kotlin
// application/src/main/kotlin/.../configuration/CacheConfig.kt
@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(): CacheManager {
        val specs = mapOf(
            "defaultTag"    to newSpec(maximumSize = 1,    expireAfterWrite = Duration.ofDays(1)),
            "allTags"       to newSpec(maximumSize = 500,  expireAfterWrite = Duration.ofMinutes(30)),
            "allBooklets"   to newSpec(maximumSize = 500,  expireAfterWrite = Duration.ofMinutes(30)),
            "userSettings"  to newSpec(maximumSize = 500,  expireAfterWrite = Duration.ofMinutes(60)),
        )

        val caffeineCacheManager = CaffeineCacheManager()
        caffeineCacheManager.setCacheNames(specs.keys)
        caffeineCacheManager.setCaffeineSpec(
            // default spec for unregistered caches (safety net)
            CaffeineSpec.parse("maximumSize=100,expireAfterWrite=5m")
        )
        // Configure each cache individually
        val cacheMap = specs.entries.associate { (name, spec) ->
            name to CaffeineCache(name, Caffeine.from(spec).build())
        }
        return SimpleCacheManager().apply { setCaches(cacheMap.values.toList()) }
    }

    private fun newSpec(maximumSize: Long, expireAfterWrite: Duration): CaffeineSpec =
        CaffeineSpec.parse("maximumSize=$maximumSize,expireAfterWrite=${expireAfterWrite.toMinutes()}m")
}
```

#### 3. Annotations on SPI adapters (infrastructure)

```kotlin
// TagRepositoryJpaAdapter.kt

@Cacheable(cacheNames = ["defaultTag"])
override fun defaultTag(): Tag {
    val found = defaultTagPostgresRepository.findAll().firstOrNull { it.name == Tag.noneTag().label }
    return found?.toDomain() ?: error("No default tag found")
}

@Cacheable(cacheNames = ["allTags"], key = "#userId.value")
override fun getAllDefault(userId: UserId): List<Tag> { ... }

@CacheEvict(cacheNames = ["allTags"], key = "#userId.value")
override fun save(userId: UserId, tag: Tag): Tag? { ... }

@CacheEvict(cacheNames = ["allTags"], key = "#userId.value")
override fun deleteById(tagId: UUID): Boolean { ... }

@CacheEvict(cacheNames = ["allTags"], key = "#userId.value")
override fun patch(tag: Tag): Tag? { ... }
```

```kotlin
// UserRepositoryJpaAdapter.kt

@Cacheable(cacheNames = ["allBooklets"], key = "#userId.value")
override fun findUserByIdWithBooklets(userId: UserId): User? { ... }

// To be evicted on saveBooklet and deleteBooklet in BookletJpaRepositoryAdapter
@CacheEvict(cacheNames = ["allBooklets"], key = "#userId.value")
override fun save(...): Booklet? { ... }

@Cacheable(cacheNames = ["userSettings"], key = "#userId.value")
// Note: findUserByIdWithBooklets is already used for settings —
// a separate cache can be applied on the service method if the same query
// must not be shared between allBooklets and userSettings.
```

> **Note**: `deleteById` in `TagRepositoryJpaAdapter` does not receive `userId` as a parameter.
> You must either add `userId` to the `deleteById` signature in the `TagRepository` port,
> or use `@CacheEvict(cacheNames = ["allTags"], allEntries = true)` as a conservative fallback
> (evicts all entries from the `allTags` cache).

---

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| Missing eviction on `deleteById` (tag): `userId` absent from signature | Medium — stale read possible | Add `userId` to the port or use `allEntries = true` |
| `allBooklets` cache shared with `userSettings` (same SQL query) | Low | Accept duplication or create a dedicated `UserSettingsRepositoryAdapter` |
| Current-month stats accidentally cached | High — stale data visible to user | Do not cache stats in Phase 1; implement in Phase 2 with short TTL (5 min) and key on `(userId, bookletId, year, month)` |
| JVM heap pressure (`maximumSize` too large) | Low — modest data volumes | Proposed `maximumSize` values are very conservative for a typical single-user load |
| `@Transactional` + `@Cacheable` on the same method | Medium — cache populated before commit | Place `@Cacheable` only on `@Transactional(readOnly = true)` methods |

---

## Implementation Notes

### Recommended implementation order

1. **Add dependencies** in `application/build.gradle.kts`
2. **Create `CacheConfig.kt`** in `application/.../configuration/`
3. **Annotate `TagRepositoryJpaAdapter`**:
   - `@Cacheable("defaultTag")` on `defaultTag()`
   - `@Cacheable("allTags", key = "#userId.value")` on `getAllDefault()`
   - `@CacheEvict("allTags", allEntries = true)` on `save()`, `deleteById()`, `patch()`
4. **Annotate `UserRepositoryJpaAdapter`**:
   - `@Cacheable("allBooklets", key = "#userId.value")` on `findUserByIdWithBooklets()`
   - `@CacheEvict("allBooklets", allEntries = true)` on booklet mutation operations
   - `@Cacheable("userSettings", key = "#userId.value")` — evaluate according to decoupling needs
5. **Tests**: verify that existing integration tests still pass — Spring Test does not activate
   the cache by default (`@EnableCaching` is absent from test contexts); ensure `CacheConfig`
   is not loaded in infrastructure test contexts if that would cause undesired behaviour.
6. **Phase 2 (optional)**: introduce the historical stats cache with a composite key and
   short TTL, after validating that Phase 1 invalidations are stable.

---

## References

- [Spring Cache Abstraction — official docs](https://docs.spring.io/spring-framework/docs/current/reference/html/integration.html#cache)
- [Caffeine — GitHub](https://github.com/ben-manes/caffeine)
- [Caffeine + Spring Boot — Baeldung](https://www.baeldung.com/spring-boot-caffeine-cache)
- [Cache-aside pattern — Microsoft Architecture Center](https://learn.microsoft.com/en-us/azure/architecture/patterns/cache-aside)
