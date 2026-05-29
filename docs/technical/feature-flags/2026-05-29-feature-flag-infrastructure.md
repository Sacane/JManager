# Feature Flag — Infrastructure Layer

> **Topic**: JPA persistence adapter + idempotent startup seeding for runtime-controlled feature flags
> **Date**: 2026-05-29
> **Author**: Technical Backend

---

## Context

The domain layer defines `FeatureFlagRepository` (output port), `FeatureKey` (closed enum registry), and
`FeatureFlag` (value object). The infrastructure layer must provide the concrete adapter that persists flag
state in PostgreSQL and seeds every known key at startup so the admin can toggle them immediately via the
future REST endpoint.

## Current State

Flyway is the migration tool (25 versions already applied, next is V26). JPA entities follow the pattern of a
plain `@Entity` class with `var` fields and no-arg constructor via `kotlin-jpa` plugin. Spring Data
`JpaRepository` is the repository abstraction. Startup seeding for default tags and the admin account is done
via `ApplicationListener<ContextRefreshedEvent>` in `DataLoader` (application module) — the same pattern is
used here but in the infrastructure module so the seeder can reference the JPA repository directly without
crossing the hexagonal boundary upward.

## Analysis

### Table design

A `feature_flag` table with a `VARCHAR(100)` natural primary key (the enum name) is the right choice:
- No UUID surrogate key is needed — the flag key is globally unique and immutable.
- `VARCHAR(100)` covers the longest current key (`EMAIL_VERIFICATION_SIMPLE_USER_REGISTRATION` = 43 chars)
  with headroom for future keys.
- The PK index covers all lookup patterns (`findByKey` = `findById` = primary key lookup, O(log n)).
- No FK: flags are independent of any user or entity.

### Entity key strategy

Storing the enum name as a raw `String` in the entity (rather than `@Enumerated(EnumType.STRING)` on the PK)
decouples the JPA mapping from the domain enum. The adapter does the translation. This means a future enum
rename does not cascade into the JPA layer — only the adapter mapping changes.

### Seeding strategy

The seeder iterates `FeatureKey.entries` and `INSERT`s a disabled row for each key not yet present. It uses
`jpaRepository.existsById(key.name)` to check presence — one indexed primary key lookup per key, which is
negligible at startup. The guard is:
- **Idempotent**: re-running the seeder (e.g. app restart) never overwrites a row whose `enabled` was changed
  by an admin at runtime.
- **Additive**: adding a new `FeatureKey` entry automatically creates a new disabled row on next startup.

`ContextRefreshedEvent` fires after Flyway has completed migrations, so the table is guaranteed to exist.
A `seeded: Boolean` guard prevents double-seeding on multi-context Spring setups (test environments).

### Upsert implementation

`upsert` does a `findById` + mutate + `save` (JPA merge semantics). This is correct for a table with a
natural PK — Spring Data's `save` calls `merge` when the entity already exists (determined by `@Id` being
non-null and present in the persistence context). No custom `@Modifying @Query` needed.

## Recommended Approach

```sql
-- V26__add_feature_flag_table.sql
CREATE TABLE IF NOT EXISTS feature_flag (
    key     VARCHAR(100) NOT NULL,
    enabled BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_feature_flag PRIMARY KEY (key)
);
```

```kotlin
// FeatureFlagEntity.kt
@Entity
@Table(name = "feature_flag")
class FeatureFlagEntity(
    @Id @Column(name = "key", nullable = false, length = 100) val key: String = "",
    @Column(name = "enabled", nullable = false) var enabled: Boolean = false,
)
```

```kotlin
// FeatureFlagRepositoryJpaAdapter.kt — upsert excerpt
override fun upsert(flag: FeatureFlag): FeatureFlag {
    val entity = jpaRepository.findById(flag.key.name)
        .orElse(FeatureFlagEntity(key = flag.key.name))
    entity.enabled = flag.enabled
    return jpaRepository.save(entity).toDomain()!!
}
```

### Why this approach

- **Alternatives ruled out**: a `@Modifying @Query("INSERT INTO … ON CONFLICT DO UPDATE …")` native query
  would work but ties the adapter to PostgreSQL-specific SQL — unnecessary when JPA merge covers the use case.
- **No `@Version` column**: optimistic locking is not needed for a single-column, admin-only write table.
  Concurrent toggles from multiple admin sessions are unlikely and last-write-wins is acceptable.
- **No cache layer**: flag reads will be cached at the application layer via `@Cacheable` when the REST
  endpoint is added — caching in the adapter itself would bypass cache invalidation on toggle.

## Implementation Notes

1. Add `V26__add_feature_flag_table.sql` to `infrastructure/src/main/resources/db/migration/`.
2. Create `FeatureFlagEntity` in `infrastructure/.../entity/`.
3. Create `FeatureFlagJpaRepository : JpaRepository<FeatureFlagEntity, String>` in `.../repositories/`.
4. Create `FeatureFlagRepositoryJpaAdapter` implementing the domain port in `.../adapters/`.
5. Create `FeatureFlagSeeder` in `.../configuration/` as `ApplicationListener<ContextRefreshedEvent>`.
6. Integration tests with Testcontainers PostgreSQL following the `DataRetentionCandidateJpaAdapterIT` pattern.

No new dependency is required — Flyway, Spring Data JPA, and PostgreSQL driver are already on the classpath.

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| Seeder fires before DataLoader (ordering not guaranteed) | Low — seeder is independent of tags/admin | No inter-dependency; both listen to the same event |
| Enum rename breaks mapping if stored raw | Low — FeatureKey entries are stable once shipped | `toDomain()` returns `null` for unknown keys; unknown rows are silently ignored |
| `existsById` N queries at startup | Negligible — O(flag count), PK lookup | Acceptable; could batch with `findAllById` if key count grows large |

## References
- [Spring Data JPA — `save` merge semantics](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.entity-persistence)
- [Flyway naming conventions](https://documentation.red-gate.com/fd/migrations-184127470.html)
