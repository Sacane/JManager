# Infrastructure — Email-Based Login Adapter

> **Topic**: JPA adapter implementation for `findByEmailWithEncodedPassword`
> **Date**: 2026-06-01
> **Author**: Technical Backend

---

## Context

The domain port `UserRepository` now declares `findByEmailWithEncodedPassword(email: String): UserWithPassword?`
as the lookup strategy for the login flow. The JPA adapter (`UserRepositoryJpaAdapter`) and its underlying
Spring Data repository (`UserPostgresRepository`) must be updated to satisfy this contract.

## Current State

- `UserResource` entity already maps `email` as `@Column(unique = true, nullable = true, length = 255)`.
- V1 Flyway migration (`V1__init_schema.sql`) already carries `email VARCHAR(255) UNIQUE` at the DB level.
- `UserPostgresRepository` exposes `findByUsername` but no `findByEmail` derived query.
- `AuthenticatedUserTest` (shared base for all infrastructure integration tests) calls
  `LoginCommand("test", "test")` positionally — after the domain field rename (`pseudonym → email`)
  this resolves to `email = "test"` instead of `"test@example.com"`, breaking the entire integration
  test suite if not corrected here.

## Analysis

### Spring Data derived query vs. `@Query`

`findByEmail` is a simple equality lookup on a single indexed column. Spring Data's derived query
mechanism (`fun findByEmail(email: String): UserResource?`) is the correct choice:
- No JPQL maintenance burden.
- The generated SQL is `SELECT * FROM user_resource WHERE email = ?` — optimal with the existing unique index.
- A `@Query` would add noise without benefit.

### Uniqueness constraint

Both the DB schema (V1) and the JPA `@Column(unique = true)` already enforce uniqueness.
**No new Flyway migration is required.** The issue's "verify" step is satisfied by inspection.

### `@Transactional` scoping

`findByEmailWithEncodedPassword` is a read-only lookup. It does not open a write transaction; the
`@Transactional` annotation is not necessary here, consistent with the existing
`findByPseudonymWithEncodedPassword` which also carries no transaction annotation.

### `toModelWithPasswords()` reuse

The existing mapper extension `UserResource.toModelWithPasswords()` already produces the correct
`UserWithPassword` aggregate. No new mapper code is needed.

## Recommended Approach

```kotlin
// UserPostgresRepository — add derived query
fun findByEmail(email: String): UserResource?

// UserRepositoryJpaAdapter — implement port method
override fun findByEmailWithEncodedPassword(email: String): UserWithPassword? =
    userPostgresRepository.findByEmail(email)?.toModelWithPasswords()
```

### Why this approach

Symmetric with `findByPseudonymWithEncodedPassword` in both structure and intent. One line in the
adapter, one line in the repository — minimal surface, maximum traceability.

## Implementation Notes

1. `UserPostgresRepository` — add `fun findByEmail(email: String): UserResource?`.
2. `UserRepositoryJpaAdapter` — add `override fun findByEmailWithEncodedPassword`.
3. `AuthenticatedUserTest` — fix `LoginCommand("test", "test")` → `LoginCommand(email = "test@example.com", userPassword = "test")`.
4. `UserRepositoryJpaAdapterTest` — add two integration test scenarios (happy path + unknown email).

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| `email` nullable in schema | Low — existing users created without email cannot log in | Addressed at registration layer (email required); out of scope here |
| Case sensitivity on email lookup | Low | PostgreSQL `=` operator is case-sensitive; emails are stored as entered at registration — consistent behaviour |
| No new migration | None | Uniqueness already enforced since V1; verified by inspection |
