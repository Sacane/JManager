# Investigation Report — Secure & Efficient User Registration

**Date:** 2026-05-22
**Status:** Draft

---

## 1. Problem Statement

The application currently provides a basic self-registration endpoint (`POST /api/user/create`) with minimal
security controls: password match validation, BCrypt hashing, and Bean Validation for input lengths. No rate
limiting protects the registration endpoint, no email is collected, password strength is unconstrained (a
one-character password is accepted), and the user must manually log in after registering. The goal is to
identify the most secure and efficient registration path given the existing architecture and deployment context.

---

## 2. Context

### What already exists

| Mechanism | State |
|---|---|
| Password hashing | BCrypt v2b, strength 12 — solid |
| JWT + refresh token rotation | In place, HTTP-only SameSite=Strict cookies |
| Rate limiting | Only on `POST /api/user/auth` (login) — **not** on registration |
| Bean Validation on DTOs | `@NotBlank @Size(min=1, max=100)` — minimum length is 1 |
| Generic login error messages | In place (no user enumeration on login) |
| Email field | Exists on `UserResource` (nullable, unique) but **absent from `RegisteredUserDTO`** |
| Auto-login after registration | Does not exist — user receives a `UserDTO` with no token |
| Email verification | Does not exist — no SMTP infrastructure, no verification token entity |
| Frontend registration page | Does not exist — only `client/pages/login.vue` exists |
| `is_enabled` column | Exists on `UserResource` (default `true`) — available for soft-disabling accounts |

### Key gap summary

- `POST /api/user/create` is **completely unprotected from spam/bots** — no rate limit, no CAPTCHA.
- Registration **collects no email**: account recovery is impossible.
- **Password strength is not enforced**: `@Size(min=1)` means a single character is accepted.
- **No distinction between duplicate username and a true internal error** in the registration failure path:
  `userRepository.register()` returns `null` on any failure, and the controller maps it to a generic
  `INVALID` result — the client cannot tell the user "this username is taken."
- **No auto-login after registration**: the response is a `UserDTO` with no token, forcing a second round
  trip to `/api/user/auth`.
- **No registration page** on the frontend.

### Relevant architectural constraints

- Hexagonal architecture — domain must stay free of infrastructure dependencies.
- No email infrastructure currently exists (no Spring Mail, no SMTP configuration).
- `InMemorySessionManager` is the session store — no Redis yet (noted as future improvement in history).
- The app has an admin bootstrap and a user management admin panel — it is designed for a small, controlled
  user base, not mass public self-registration.

---

## 3. Impact Analysis

### Domain Layer

The `RegisterUserCommand` / `RegisterUserService` must be extended to:
- Accept and validate a password strength rule (new domain invariant).
- Distinguish `DUPLICATE_USERNAME` from `INVALID` in the failure path.
- Optionally: accept an `email` field and enforce uniqueness at the port contract level.

If email verification is added, a new `EmailVerificationToken` value object and a corresponding SPI port
(`EmailVerificationRepository`) are required. The `RegisterUserService` would save a pending token;
a new `VerifyEmailUseCase` would activate the account.

### Infrastructure Layer

- `UserPostgresRepository` needs a `findByUsername` query to surface `DUPLICATE_USERNAME` distinctly
  (the method already exists — the adapter just needs to check before delegating to `register()`).
- If email is collected: `UserResource.email` and the DB column are already present. The `register()`
  adapter method needs to persist the email.
- If email verification is added: a new `email_verification_token` table (Flyway migration), a new JPA
  entity, and a Spring Mail adapter are required.

### Application Layer

- `RegisteredUserDTO`: add `email` field with `@NotBlank @Email` validation; add minimum password length
  constraint (`@Size(min=8)`).
- `SessionController.createUser()`:
  - Rate limiting on `/api/user/create` (reuse `LoginRateLimiter` or a dedicated one).
  - After successful registration, dispatch `LoginCommand` and set auth cookies in the same response
    (auto-login) to avoid the double round-trip.
  - Map `DUPLICATE_USERNAME` to HTTP 409 Conflict.
- If email verification: add `POST /api/user/verify?token=XXX` public endpoint; block login for
  unverified accounts by checking `is_enabled` in the JWT filter or login service.

### Client Layer

- A `client/pages/register.vue` page is missing entirely.
- `useAuth.ts` already has a `register()` composable function that calls `POST /api/user/create` — it just
  needs to be wired to a page and updated to handle the new response shape (token + user, if auto-login
  is implemented).
- If email verification: a `client/pages/verify-email.vue` page is needed with a loading/success/error
  state that calls the verify endpoint on mount.

### Cross-Cutting Concerns

| Concern | Current state | Impact |
|---|---|---|
| **Security: bot abuse** | No protection on registration | High — rate limit is mandatory regardless of approach |
| **Security: password strength** | Min length 1 | High — trivially brute-forceable accounts |
| **Security: user enumeration** | Registration returns distinct INVALID for all failures | Medium — DUPLICATE_USERNAME feedback must be carefully worded |
| **UX: double round-trip** | User registers then logs in manually | Low-to-medium — one extra click, fixable with auto-login |
| **Backward compatibility** | No breaking change if email is optional in DTO | None if `email` field is nullable in DTO |
| **Testability** | Domain service is easily testable with fakes | Good — new domain rules follow same pattern |

---

## 4. Solution Approaches

### Approach A — Hardened self-registration (no new infrastructure)

**Summary.** Tighten the existing registration flow without introducing any new external dependency.
Add rate limiting on the registration endpoint, enforce a minimum password strength rule in the domain,
collect email (stored, not verified), add auto-login after registration, surface duplicate-username errors
distinctly, and build a registration page on the frontend.

| Attribute | Detail |
|---|---|
| **Layers touched** | Domain (password strength rule, distinct error), Application (rate limit, auto-login, DTO), Client (register page) |
| **Pros** | Zero new infrastructure. Deliverable in a single sprint. Immediately closes the most critical gaps (bot abuse, weak passwords). |
| **Cons / Risks** | Email is stored but not verified — anyone can register with a fake or another person's email address. No account recovery mechanism. |
| **Fit for this project** | High. Given no SMTP infrastructure and a small, controlled user base, this is the correct first step. |

Key changes:
```
// Domain — new invariant in RegisterUserService
if (command.password.length < 8) → failure(PASSWORD_TOO_SHORT)
if (!command.password.any { it.isDigit() || !it.isLetterOrDigit() }) → failure(PASSWORD_TOO_WEAK)

// Domain — distinguish duplicate username
userRepository.existsByUsername(username)  // new SPI method
  → failure(DUPLICATE_USERNAME)  before calling register()

// Application — rate limit registration
@PostMapping("/create")
fun createUser(...) {
    if (!registrationRateLimiter.isAllowed(request.remoteAddr)) → 429
    ...
    // after successful register: dispatch LoginCommand, set cookies, return UserStorageDTO
}

// Application — DTO
data class RegisteredUserDTO(
    @NotBlank @Size(min=3, max=100) val username: String,
    @NotBlank @Size(min=8, max=100) val password: String,
    @NotBlank @Size(min=8, max=100) val confirmPassword: String,
    @Email @Size(max=255) val email: String? = null,
)
```

---

### Approach B — Email-verified self-registration

**Summary.** Extend Approach A with a mandatory email field and a verification step: after registration,
the account is created with `is_enabled = false` and a time-limited token is emailed to the user. The
account is only activated once the user clicks the verification link. Login is blocked for unverified
accounts. This is the industry-standard pattern for any public-facing identity system.

| Attribute | Detail |
|---|---|
| **Layers touched** | Domain (new `EmailVerificationToken` entity + SPI port + `VerifyEmailUseCase`), Infrastructure (new table, Spring Mail adapter), Application (new verify endpoint, login guard), Client (new verify-email page) |
| **Pros** | Guarantees the user controls the email address. Enables account recovery. Prevents registration with fake emails. |
| **Cons / Risks** | Requires SMTP configuration and infrastructure. Adds friction (user must check their inbox before accessing the app). Verification token management adds complexity (expiry, resend). |
| **Fit for this project** | Medium. Appropriate if the app is intended for more than a handful of users. Overkill for a purely personal or single-household deployment. |

Additional domain contract:
```kotlin
interface EmailVerificationRepository {
    fun save(token: EmailVerificationToken)
    fun findByToken(token: UUID): EmailVerificationToken?
    fun deleteByUserId(userId: UserId)
}

data class EmailVerificationToken(
    val token: UUID,
    val userId: UserId,
    val expiresAt: Instant
)
```

New application endpoints:
```
POST /api/user/create       → creates account (is_enabled=false), sends verification email
POST /api/user/verify       → activates account (is_enabled=true), returns UserStorageDTO with token
POST /api/user/resend-verification → resends email (rate-limited)
```

---

### Approach C — Admin-gated invitation

**Summary.** Disable public self-registration entirely. Admin generates one-time invitation links
(`POST /api/admin/invite`). The invitation email contains a URL to a `client/pages/accept-invite.vue`
page where the recipient sets their username and password. Account is immediately active after setup.
No open registration surface at all.

| Attribute | Detail |
|---|---|
| **Layers touched** | Domain (new `InvitationToken` entity + SPI port + `CreateInvitationUseCase` + `AcceptInvitationUseCase`), Infrastructure (new table, Spring Mail adapter), Application (disable `/api/user/create` or restrict to admin), Client (new accept-invite page) |
| **Pros** | Complete control over who joins. Eliminates the registration attack surface entirely. No spam/bot risk on any public endpoint. |
| **Cons / Risks** | Requires SMTP (same as B). Adds admin operational overhead for every new user. Registration page becomes useless unless repurposed as the invite-acceptance flow. |
| **Fit for this project** | Medium-high for a private deployment. The existing admin bootstrap and admin user-management panel suggest the app is already designed with a controlled, small user base in mind. |

---

## 5. Recommended Approach

**Implement Approach A immediately, with Approach B as the declared next milestone.**

Rationale:

1. The most critical security gaps (no rate limiting, no minimum password strength, no error distinction)
   exist today and can be closed without any external dependency.
2. Auto-login after registration is a pure UX improvement that eliminates a double round-trip at zero cost.
3. Email collection (stored, optional for now) positions the app to add verification later without a
   schema migration.
4. Approach B is the correct long-term target if the app serves more than a very small, trusted group —
   but it requires SMTP infrastructure that does not exist yet. Committing to it before setting up the
   mail stack would create an incomplete feature.
5. Approach C is only justified if the user base is intentionally closed and admin-controlled. Given the
   existing `POST /api/user/create` endpoint and the absence of any stated restriction, the current
   intent appears to be self-registration — Approach C would be a policy decision, not a technical one.

**Priority order for Approach A:**
1. Rate limit `POST /api/user/create` (closes the highest-risk gap, reuses `LoginRateLimiter`).
2. Enforce minimum password strength in the domain (8 chars + at least one non-letter).
3. Distinguish `DUPLICATE_USERNAME` from generic `INVALID` in the registration failure path.
4. Add optional `email` field to `RegisteredUserDTO` and persist it.
5. Auto-login after registration (dispatch `LoginCommand` in controller, return `UserStorageDTO` with cookies).
6. Build `client/pages/register.vue` with form validation matching backend constraints.

---

## 6. Open Questions

1. **Is open self-registration intentional?** The app has an admin bootstrap and a user-management panel,
   suggesting a small, controlled group. Should anyone be allowed to register, or should registration
   require an admin-issued invitation (Approach C)?

2. **Is SMTP infrastructure available or planned?** Without it, Approach B (email verification) cannot
   be implemented. Is a mail server or a transactional email service (e.g. Mailgun, SendGrid) in scope?

3. **Password strength policy.** What is the intended minimum? 8 characters is a common baseline, but
   the user may want stricter rules (e.g. at least one uppercase, one digit, one special character).
   These rules belong in the domain as invariants.

4. **Username policy.** Should usernames be case-insensitive for uniqueness checks? Currently the DB
   column is `unique` on the raw value — "Alice" and "alice" would be treated as different users.

---

## 7. Next Steps

- Implement Approach A security hardening → `/create-issue` to produce a structured issue.
- If SMTP infrastructure is available or planned: escalate to Approach B → `/create-issue` for the
  email-verification feature.
- If the user base is intentionally closed: consider Approach C → `/create-issue` for admin-invitation flow.
- Clarify open questions 1–4 before starting implementation to avoid rework.
