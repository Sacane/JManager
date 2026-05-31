# Email Verification — Infrastructure Layer

> **Topic**: Infrastructure adapters for email verification
> **Date**: 2026-05-31
> **Author**: Technical Backend

---

## Context

The domain layer now defines `EmailVerificationTokenRepository`, `SecureTokenGenerator`, `NotificationPort.sendVerificationEmail`, and `UserRepository.markEmailVerified`. This report covers the infrastructure implementation: schema migration, JPA entity/adapter, secure token generation, and Spring Mail extension.

## Current State

- `user_resource` table has no `email_verified` column. Existing rows are unverified by default.
- `SpringMailNotificationAdapter` implements `NotificationPort` with `sendWelcomeEmail` only.
- `EmailTemplates` is an internal `object` with a shared `baseLayout`.
- `HexagonInjectionConfiguration` component-scans `@DomainService` classes — domain services with non-bean constructor args (e.g. `Clock`, `Duration`) must be declared explicitly as `@Bean`.

## Analysis

### Schema

Two DDL changes needed in a single migration:
1. `ALTER TABLE user_resource ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE` — safe zero-downtime; existing rows default to `false`.
2. `CREATE TABLE email_verification_token` with `ON DELETE CASCADE` on the FK to `user_resource.id_user` — ensures tokens are cleaned up when a user is deleted without requiring explicit domain logic.

### Token entity

No surrogate UUID needed: the token value is already unique and immutable (32-byte base64url = 43 chars, stored as `VARCHAR(64)` with a `PRIMARY KEY` constraint). No JPA `@OneToMany` on `UserResource` — token lifecycle is managed exclusively via `EmailVerificationTokenRepository`.

### SecureTokenGenerator

`SecureRandom` with `Base64.getUrlEncoder().withoutPadding()` over 32 bytes produces a 43-char URL-safe string. No dependency beyond the JDK; no additional library needed.

### Spring wiring — `EmailVerificationIssuer`

`EmailVerificationIssuer` is annotated `@DomainService` which is picked up by the component-scan filter in `HexagonInjectionConfiguration`. However, it requires `Clock` (not a default Spring bean) and `Duration` (a plain value). If left to component scan, Spring will fail to auto-wire. **Resolution**: remove `@DomainService` from `EmailVerificationIssuer` and declare it explicitly as `@Bean` in `EmailVerificationConfiguration`, following the same pattern used for `TokenGenerator`.

`VerifyEmailService` also needs `Clock` — solved by declaring a `@Bean fun systemClock(): Clock = Clock.systemUTC()` in `EmailVerificationConfiguration`.

### TTL configuration

Exposed via `app.email-verification.token-ttl` (ISO-8601 duration, e.g. `PT24H`). Bound with `@ConfigurationProperties`. Registered in `JpaAutoConfiguration` alongside `RetentionProperties`.

## Recommended Approach

```kotlin
// EmailVerificationProperties.kt
@ConfigurationProperties(prefix = "app.email-verification")
data class EmailVerificationProperties(
    val tokenTtl: Duration = Duration.ofHours(24),
)

// EmailVerificationConfiguration.kt
@Configuration
@EnableConfigurationProperties(EmailVerificationProperties::class)
class EmailVerificationConfiguration {
    @Bean fun systemClock(): Clock = Clock.systemUTC()

    @Bean fun emailVerificationIssuer(
        tokenRepo: EmailVerificationTokenRepository,
        generator: SecureTokenGenerator,
        notification: NotificationPort,
        clock: Clock,
        props: EmailVerificationProperties,
    ): EmailVerificationIssuer = EmailVerificationIssuer(tokenRepo, generator, notification, clock, props.tokenTtl)
}
```

### Why this approach
- Keeps `EmailVerificationIssuer` a pure Kotlin class (no Spring annotations in domain).
- Explicit `@Bean` mirrors the existing `tokenGenerator` pattern in `HexagonInjectionConfiguration`.
- `Clock.systemUTC()` is a well-known, testable abstraction — tests override it via constructor injection.

## Implementation Notes

1. Flyway `V27__add_email_verification.sql`
2. `UserResource` — add `emailVerified: Boolean = false` field
3. `UserRepositoryJpaAdapter` — update `register()` + add `markEmailVerified()`
4. `DatasourceMapper` — add `emailVerified` to `toModel()` and `toModelWithPasswords()`
5. `EmailVerificationTokenEntity` + `EmailVerificationTokenJpaRepository` (new)
6. `EmailVerificationTokenRepositoryJpaAdapter` (new)
7. `SecureTokenGeneratorAdapter` (new)
8. `EmailTemplates.verificationEmail()` (new template function)
9. `SpringMailNotificationAdapter.sendVerificationEmail()` (new method)
10. `EmailVerificationProperties` + `EmailVerificationConfiguration` (new)
11. Remove `@DomainService` from domain's `EmailVerificationIssuer`
12. `JpaAutoConfiguration` — register `EmailVerificationProperties`
13. Integration test `EmailVerificationTokenRepositoryJpaAdapterIT`

## Trade-offs & Risks

| Concern | Impact | Mitigation |
|---|---|---|
| Existing users start with `emailVerified = false` | Medium — users see "unverified" banner on first login after deploy | Backfill script to set existing users to `true` before enabling UI |
| Token table grows unbounded if tokens expire but verification never completes | Low | Future: scheduled cleanup job for expired rows; `expiresAt` index helps queries |
| `@Async` mail sending — failure is silent (logged only) | Low | Consistent with existing welcome-email behaviour; structured logging in place |

## References
- [Spring Boot `@ConfigurationProperties`](https://docs.spring.io/spring-boot/docs/current/reference/html/configuration-metadata.html)
- [java.security.SecureRandom](https://docs.oracle.com/en/java/docs/api/java.base/java/security/SecureRandom.html)
- [Flyway zero-downtime migrations](https://flywaydb.org/documentation/concepts/migrations)
