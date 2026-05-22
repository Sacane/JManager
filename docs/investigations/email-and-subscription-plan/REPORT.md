# Investigation Report — Email Infrastructure & Subscription Plan Model

**Date:** 2026-05-22
**Status:** Draft

---

## 1. Problem Statement

Two related short-term features must be designed before the beta launch:

1. **Email sending infrastructure** — When a user account is created via `POST /api/user/create`, the server
   must send a confirmation/welcome email to that user's address. This is the first use of an email system
   that will grow to cover future notifications (password reset, plan upgrade, etc.). The implementation
   does not need to be complete (no email verification gate), but the infrastructure must be real and wired.

2. **Subscription plan model** — Three plan tiers must be introduced: `BETA_TESTER`, `FREE`, and `PREMIUM`.
   All users currently in the database must be migrated to `BETA_TESTER` as their default plan. New users
   created during the beta period will also be `BETA_TESTER`. The public registration flow (FREE by
   default) is explicitly out of scope for now.

---

## 2. Context

### Current state — Email

- `spring-boot-starter-mail` is **not** a dependency in any module's `build.gradle.kts`.
- `application.properties` has **no SMTP configuration**.
- The domain has **no notification port** of any kind.
- `UserResource.email` exists as a nullable column on the `user_resource` table.
- `RegisteredUserDTO` does **not** collect an email field — it only has `username`, `password`,
  `confirmPassword`.
- `RegisterUserCommand` similarly has no `email` field.
- `UserRepository.register()` signature: `register(username, password, roles)` — no email parameter.

Consequence: sending a welcome email after registration requires adding email collection to the full
registration pipeline (DTO → Command → Service → Repository), in addition to the notification
infrastructure itself.

### Current state — Subscription plan

- No `SubscriptionPlan` concept exists anywhere in the codebase (domain, infrastructure, or application).
- `User` has a `roles: Set<Role>` field (`USER`, `ADMIN`) for access control — subscription is a
  distinct concern and must not be conflated with roles.
- `UserResource` has no subscription column.
- `UserRepositoryJpaAdapter.register()` creates a `UserResource` with `username`, `password`, and `roles`
  only — no plan.
- The latest Flyway migration is `V21__add_parent_id_to_tag_personal.sql`. The next migration will be V22.

---

## 3. Impact Analysis

### Domain Layer

**Subscription plan:**
- A new `SubscriptionPlan` enum (`BETA_TESTER`, `FREE`, `PREMIUM`) must be added to the domain models
  package. This is a pure value — no behaviour yet.
- `User` aggregate must gain a `subscriptionPlan: SubscriptionPlan` field, defaulting to `BETA_TESTER`.
- `UserRepository.register()` port signature must accept `subscriptionPlan` (with a default of
  `BETA_TESTER` to avoid breaking existing call sites if any).
- `RegisterUserCommand` must be updated to carry `email: String` and optionally `subscriptionPlan`
  (service can default it).
- `RegisterUserService.handle()` must pass `email` and `subscriptionPlan` to the repository.

**Email:**
- A new SPI port `NotificationPort` (or `EmailPort`) must be defined in the domain's output port package.
  The domain must not reference Spring Mail, SMTP, or any third-party email API.
- Minimal contract for immediate scope:

  ```kotlin
  @Port(Side.INFRASTRUCTURE)
  interface NotificationPort {
      fun sendWelcomeEmail(username: String, email: String)
  }
  ```

- `RegisterUserService` injects `NotificationPort` and calls it after a successful registration.
  This is the cleanest hexagonal approach: the use case owns the full registration outcome.

**Domain invariants:**
- `User.email` is currently nullable. If email becomes mandatory for registration (required to send
  welcome mail), the invariant must be enforced in `RegisterUserService`, not in the domain model
  itself (to avoid breaking existing paths like admin bootstrap, which currently creates users
  without email).

### Infrastructure Layer

**Subscription plan:**
- `UserResource` must gain a `subscriptionPlan` column mapped to the new enum.
- Flyway **V22** migration: `ALTER TABLE user_resource ADD COLUMN subscription_plan VARCHAR(50)
  NOT NULL DEFAULT 'BETA_TESTER'` — this correctly back-fills all existing users with `BETA_TESTER`
  in a single migration, with no data loss and no manual intervention.
- `UserRepositoryJpaAdapter.register()` must persist the `subscriptionPlan` on the new `UserResource`.
- `DatasourceMapper` (the `toModel()` / `asResource()` extensions) must be updated to map the new field.

**Email:**
- Add `spring-boot-starter-mail` to `infrastructure/build.gradle.kts` (Spring Mail belongs in the
  infrastructure module alongside the SMTP adapter, not in `application`).
- New class `SpringMailNotificationAdapter` implements `NotificationPort`. It uses
  `JavaMailSender` and `SimpleMailMessage` (or `MimeMessage` for HTML templates later).
- SMTP configuration lives in `application.properties` (profile-aware: dev can use a dummy SMTP or
  Mailpit; prod uses a real SMTP provider).
- **Async sending is mandatory**: the welcome email must not block the HTTP response. Annotate the
  adapter method with `@Async` and enable `@EnableAsync` on the Spring Boot application class. A
  failed email must be logged and swallowed — it must never cause a 500 on registration.

### Application Layer

**DTO changes:**
- `RegisteredUserDTO` must add an `email` field:
  ```
  @field:NotBlank @field:Email @field:Size(max = 255) val email: String
  ```
- `UserDTO` already has an `email: String?` field — no change needed there.
- `SessionController.createUser()` passes the email from the DTO to `RegisterUserCommand`.

**Security:**
- No new public surface is created — `POST /api/user/create` remains the only registration endpoint.
- No authorisation change required.

**Subscription in API responses:**
- `UserDTO` should expose `subscriptionPlan: String` so the frontend can gate features in future.
  Add the field now even if the frontend does not consume it yet.

### Client Layer

Not applicable for the subscription model (no UI needed now — plan is set server-side and not yet
used for feature gating).

For email: the registration flow currently has no frontend page. The email is triggered server-side
on `POST /api/user/create` — no client change is needed to make it work.

If a `client/pages/register.vue` page is added in future, it will need an `email` input field, but
that is explicitly out of scope here.

### Cross-Cutting Concerns

| Concern | Assessment |
|---|---|
| **Backward compatibility** | The V22 Flyway migration uses `DEFAULT 'BETA_TESTER'` — zero downtime, zero data loss. Existing API consumers are unaffected. |
| **Email deliverability** | A raw SMTP server (VPS-hosted) has poor deliverability (spam filters). A transactional email service (Brevo, Mailgun, Resend) is strongly preferred even for the beta. |
| **Async failure isolation** | If the SMTP call fails after a successful `register()`, the user account still exists. The failure must be logged server-side with enough context to retry manually if needed. No rollback. |
| **Dev/test environment** | Sending real emails in tests or local dev is unacceptable. The `NotificationPort` fake for domain tests should be a no-op. For integration tests, use a Mailpit container (Testcontainers) or configure `spring.mail.host=localhost` with `spring.mail.test-connection=false`. |
| **Performance** | `@Async` offloads SMTP I/O from the request thread — registration HTTP latency is unaffected. |
| **Future extension** | The `NotificationPort` contract is minimal by design. Adding `sendPasswordResetEmail()`, `sendPlanUpgradeEmail()`, etc. are additive changes with no rework. |

---

## 4. Solution Approaches

### Approach A — Domain port + async infrastructure adapter (recommended)

**Summary.** `NotificationPort` is defined in the domain as an SPI port. `RegisterUserService` calls it
after a successful registration. The infrastructure adapter (`SpringMailNotificationAdapter`) implements
the port using Spring Mail with `@Async`. SMTP config lives in `application.properties`.

| Attribute | Detail |
|---|---|
| **Layers touched** | Domain (new port + service change), Infrastructure (new adapter + Spring Mail dep), Application (DTO + Controller) |
| **Pros** | Fully hexagonal — domain owns the registration outcome. Port is trivially faked in domain tests (no-op implementation). Easy to swap SMTP provider later. |
| **Cons / Risks** | Slightly more files to touch (port + adapter). Requires `@EnableAsync` on the app class. |
| **Fit for this project** | High. Consistent with existing pattern: `Hasher`, `TokenGenerator`, `SessionManager` are all domain-defined ports with infrastructure adapters. |

### Approach B — Application-layer fire-and-forget after command success

**Summary.** The domain is not touched. After `commandBus.dispatch(RegisterUserCommand(...))` succeeds,
`SessionController.createUser()` directly calls a Spring Mail bean to send the email. No domain port, no
infrastructure adapter — just a service injected into the controller.

| Attribute | Detail |
|---|---|
| **Layers touched** | Application only (controller + new mail service bean) |
| **Pros** | Fewer files to modify. No domain change. Quick to implement. |
| **Cons / Risks** | Breaks hexagonal discipline — infrastructure concern (email) leaks into the application adapter layer. The registration use case is no longer self-contained in the domain. Harder to test in isolation. |
| **Fit for this project** | Low. Every other cross-cutting concern (hashing, token generation, session) uses the port pattern. Diverging here would be inconsistent and create precedent for future short-cuts. |

---

## 5. Recommended Approach

**Approach A for email. Single approach for subscription plan (no meaningful alternative).**

For email:
- Define `NotificationPort` in `domain/port/output/`.
- Inject it into `RegisterUserService` and call `sendWelcomeEmail()` after successful persistence.
- Implement `SpringMailNotificationAdapter` in `infrastructure` with `@Async`.
- Add `spring-boot-starter-mail` to `infrastructure/build.gradle.kts`.
- Use a transactional email provider via SMTP relay (see Open Questions §6 for provider choice).
- Fake implementation for domain tests: a simple no-op or a `MutableList<String>` collector for assertions.

For subscription plan:
- Add `SubscriptionPlan` enum to `domain/models/`.
- Add `subscriptionPlan: SubscriptionPlan = SubscriptionPlan.BETA_TESTER` to `User`.
- V22 Flyway migration: `DEFAULT 'BETA_TESTER'` back-fills existing users atomically.
- Update `UserResource`, `DatasourceMapper`, `UserRepositoryJpaAdapter.register()`.
- Update `RegisterUserService` to default new registrations to `BETA_TESTER`.
- Expose `subscriptionPlan` in `UserDTO`.

**Implementation order:**
1. Subscription plan (domain → infra migration → adapter → DTO) — self-contained, no external dependency.
2. Email (domain port → infra adapter → SMTP config) — depends on having an SMTP provider configured.

---

## 6. Open Questions

1. **SMTP provider for beta.** Three viable options for a SaaS beta:
   - **Brevo (ex-Sendinblue)** — free tier: 300 emails/day. French company, GDPR-friendly. Spring Mail
     compatible via SMTP relay. Best fit given the project's likely EU deployment.
   - **Resend** — free tier: 3,000 emails/month. Modern API, excellent developer experience.
     Requires their SDK or SMTP relay.
   - **Mailgun** — free tier: 100 emails/day (trial). Well-established but more setup friction.
   - Self-hosted SMTP (VPS Postfix/Exim): not recommended — deliverability issues without careful SPF/DKIM/DMARC setup.

2. **Email for admin bootstrap.** The `createAdminIfNotExists` flow does not go through `RegisterUserService`.
   Should the admin account also receive a welcome email? Likely no — the admin is created at startup, not
   by a human action. `NotificationPort` should not be called from `AdminFeatureImpl`.

3. **`email` field: mandatory or optional for the beta?** Currently nullable in `UserResource`.
   For the beta with manually pre-registered users, making it mandatory in `RegisteredUserDTO` is correct
   (you need it to send the welcome email). But the admin-created users (bootstrap) bypass this DTO — no
   change needed there.

4. **HTML email template vs plain text.** `SimpleMailMessage` (plain text) is sufficient for a welcome
   email at this stage. HTML templates (Thymeleaf, Freemarker) can be added later without changing the
   port contract — the adapter handles the rendering internally.

---

## 7. Next Steps

- **Subscription plan**: → `/create-issue` for the `SubscriptionPlan` enum + V22 migration + domain/infra
  plumbing. Self-contained, no external dependency, can be done immediately.
- **Email infrastructure**: → `/create-issue` once the SMTP provider is chosen (Open Question 1). The
  implementation is straightforward once the provider is selected and credentials are available.
- Both features are independent and can be issued and implemented in parallel.
