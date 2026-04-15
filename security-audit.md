# Security Audit Report — JManager

> **Date:** April 2, 2026
> **Scope:** Frontend (Vue/Nuxt) + Backend (Kotlin/Spring Boot + PostgreSQL)
> **Out of scope:** Business domain layer

---

## AXIS 1 — Frontend: Injection & Click Spam

### 🔴 Critical

| ID | File | Issue |
|----|------|-------|
| F-01 | `client/pages/login.vue` (L100) | Login button **has no `:loading`/`:disabled` state**. A user can spam the submit and trigger N authentication requests in parallel. |
| F-02 | `client/composables/useAuth.ts` (L47) | User data decoded from the JWT is **stored in `localStorage`** in plain text. Vulnerable to theft via XSS if a third-party dependency is compromised. |
| F-03 | `client/pages/login.vue` (L77) | Error message `"Username and password do not match"` enables **user enumeration** if the backend differentiates between "unknown user" and "wrong password". |

### 🟡 Medium

| ID | File | Issue |
|----|------|-------|
| F-04 | `client/components/dialog/TransactionCreationDialog.vue` | Create button: the `:disabled` depends on a parent prop that is not systematically passed — double submit possible. |
| F-05 | `client/components/dialog/CsvImportDialog.vue` (L163) | CSV file MIME type **validated client-side only**; easily bypassed. Extension is checked, but file content is not scanned to detect injection formulas (`=CMD`, `=HYPERLINK`…). |
| F-06 | `client/utils/errorCodeMap.ts` | Some error codes expose internal details (e.g. precise error code on wrong password). Should be merged into a generic message on the client side. |

### 🟢 Positive Points
- No `v-html` or dynamic `innerHTML` detected → no direct XSS risk
- `useLoading` well implemented with scope-based locking — protects the majority of API calls
- `withCredentials: true` correctly configured for cookies
- MIME + 5MB size limit + empty file check on CSV import ✅

---

## AXIS 2 — Backend: Incoming Payload Validation

### 🔴 Critical

| ID | File | Issue |
|----|------|-------|
| B1-01 | All DTOs (`transaction`, `session`, `booklet`, `tag`, `csv`) | **Zero `@Valid` / Bean Validation annotations** across the entire API layer. String fields can be empty, null, or arbitrarily large. |
| B1-02 | `infra/.../session/Controller.kt` (L29) | Login/Register: `username` and `password` **have no length or format constraint**. A 100MB payload is accepted. |
| B1-03 | `infra/.../admin/Controller.kt` (L31) | Pagination `page` and `size` **not validated**: a negative value or `size=1000000` → memory exhaustion risk. |
| B1-04 | `infra/.../ProblemDetailHandler.kt` (L81) | The generic handler exposes **`ex.message`** in the HTTP response → leaks internal information (SQL errors, stack traces). |

### 🟡 Medium

| ID | File | Issue |
|----|------|-------|
| B1-05 | `infra/.../booklet/Controller.kt` (L35) | `currency` not validated against ISO 4217 format; `labelAccount` accepts any length. |
| B1-06 | `infra/.../tag/DTO.kt` (L16) | Tag RGB components (red, green, blue) **not bounded to [0-255]**. |
| B1-07 | `infra/.../stats/Controller.kt` (L33) | `year` parameter has no validation — negative value or `year=9999999` is accepted. |
| B1-08 | `infra/.../session/Controller.kt` (L54) | `projectionWindowDays` has no min/max bound. |
| B1-09 | All POST/PUT/PATCH controllers | No `consumes = "application/json"` → any Content-Type accepted for `@RequestBody` endpoints. |
| B1-10 | `application.properties` | No `server.tomcat.max-http-post-size` configured (default is 10MB for JSON bodies). |

### 🟢 Positive Points
- Manual date validation `startDate`/`endDate` (range check) ✅
- CSV upload validation: 5MB size, `.csv` extension, empty file check ✅
- Centralized exception handling via `ProblemDetailHandler` with `@ControllerAdvice` ✅
- JJWT library used for token validation ✅

---

## AXIS 3 — Backend: Authentication

### 🔴 Critical

| ID | File | Issue |
|----|------|-------|
| B2-01 | `infra/.../session/Controller.kt` (L36) | **No brute force protection** on `/api/user/auth`. Unlimited login attempts accepted without rate limiting or account lockout. |

### 🟠 High

| ID | File | Issue |
|----|------|-------|
| B2-02 | `domain/.../SessionManager.kt` (L64) | Sessions stored **in memory only** (`MutableMap` in-process). All tokens are lost on server restart. Incompatible with a multi-instance deployment. |
| B2-03 | `domain/.../SessionManager.kt` | **No token blacklist**. After logout, if the JWT has not expired and an attacker had a copy, it remains valid until its 1-hour expiry. |

### 🟡 Medium

| ID | File | Issue |
|----|------|-------|
| B2-04 | `infra/.../session/Controller.kt` (L44) | `token` cookie: **`Secure` flag absent** in local (acceptable), but **`SameSite` not configured** in prod → incomplete defense-in-depth. |
| B2-05 | `infra/.../session/Filter.kt` (L45) | Returns **HTTP 404** on auth error instead of **HTTP 401** → authentication errors are hidden behind misleading "not found" responses. |
| B2-06 | `infra/.../utils/JwtTokenGenerator.kt` (L73) | `println("Error reading token $token: ...")` → **the token itself is logged** in plain text on errors. Replace with a logger with masking. |
| B2-07 | `domain/.../AccessToken.kt` (L18) | `refreshToken` is generated but **never used**. The `/api/user/auth/refresh/{id}` endpoint is called client-side but not implemented on the backend. |
| B2-08 | `infra/src/main/resources/application-local.properties` (L9) | Hardcoded dev admin credentials `admin:admin`. Acceptable locally, must ensure they never leak to production. |

### 🟢 Positive Points
- **BCrypt STRENGTH=12** for password hashing ✅
- JWT signed with **HmacSHA256** + systematic expiry verification ✅
- Cookie `httpOnly=true` → JS cannot read the token ✅
- Cookie `Secure=true` in production ✅
- Stateless (`SessionCreationPolicy.STATELESS`) → no session fixation risk ✅
- Logout properly implemented: token removed from memory + cookie expired ✅
- RBAC implemented (`ADMIN` / `USER`) with `/api/admin/**` route protection ✅
- CORS restricted to `localhost:3000` in local (no wildcard `*`) ✅

---

## AXIS 4 — Backend: Database Access

### 🔴 Critical

| ID | File | Issue |
|----|------|-------|
| B3-01 | `infra/.../BookletJpaRepositoryAdapter.kt` (L50) | `deleteAccountById()` **performs no ownership check**. Any authenticated user can delete any account by knowing its UUID. |
| B3-02 | `infra/.../BookletJpaRepositoryAdapter.kt` (L60) | `update()` and `updateMonthlyPeriodStartDay()` have the **same missing ownership check**. |

### 🟠 High

| ID | File | Issue |
|----|------|-------|
| B3-03 | `infra/.../TransactionRepositoryJpaAdapter.kt` (L36) | Account balance update (`account.amount`) performed as an **application-level read-modify-write** with no lock. Under two concurrent requests, one of the updates is silently lost (race condition on balance). |
| B3-04 | N/A | **Zero `@Version` / `@Lock`** across the entire JPA layer. No protection against concurrent modifications (no optimistic or pessimistic locking). |

### 🟡 Medium

| ID | File | Issue |
|----|------|-------|
| B3-05 | `application.properties` | **No HikariCP configuration**: no `maximumPoolSize`, `connectionTimeout`, `leakDetectionThreshold`. Tomcat defaults apply with no load guarantee. |
| B3-06 | `infra/.../DatasourceConfig.kt` | Using `DriverManagerDataSource` (no pool) instead of `HikariDataSource`. |

### 🟢 Positive Points
- **Zero SQL injection**: all queries use parameterized JPQL or JPA named parameters ✅
- `@Transactional` correctly applied on all write operations ✅
- `spring.jpa.open-in-view=false` ✅
- `hibernate.globally_quoted_identifiers=true` ✅
- No N+1 problem: `LEFT JOIN FETCH` correctly used on all associations ✅
- Cascades correctly configured (no `CascadeType.ALL` on ManyToMany) ✅
- `ddl-auto=none` in production ✅

---

## Summary Table — Priorities

| Priority | ID | Axis | Action |
|----------|----|------|--------|
| 🔴 **P0** | B3-01, B3-02 | Database | Add ownership verification in `deleteAccountById`, `update`, `updateMonthlyPeriodStartDay` |
| 🔴 **P0** | B1-01 | Endpoints | Add `@Valid` + Bean Validation annotations on all DTOs |
| 🔴 **P0** | B2-01 | Auth | Implement rate limiting on `/api/user/auth` (e.g. bucket4j) |
| 🔴 **P0** | F-01 | Frontend | Add `:loading`/`:disabled` to the login button |
| 🟠 **P1** | B1-04 | Endpoints | Remove `ex.message` from public HTTP error responses |
| 🟠 **P1** | B3-03, B3-04 | Database | Add `@Version` on critical entities + lock on balance update |
| 🟠 **P1** | B2-02 | Auth | Migrate session store to Redis or PostgreSQL |
| 🟠 **P1** | F-02 | Frontend | Remove token from `localStorage` (backend httpOnly cookies are sufficient) |
| 🟡 **P2** | B1-02, B1-03 | Endpoints | Validate text fields (length, format) + bound numeric parameters |
| 🟡 **P2** | B2-04 | Auth | Add `SameSite=Strict` on the `token` cookie |
| 🟡 **P2** | B2-03 | Auth | Implement a revoked token blacklist |
| 🟡 **P2** | B3-05 | Database | Configure HikariCP in `application.properties` |
| 🟡 **P2** | B2-05 | Auth | Fix JWT filter: return 401 instead of 404 |
| 🟡 **P2** | B2-06 | Auth | Mask token values in logs (never log raw token value) |
| 🟢 **P3** | F-03 | Frontend | Unify login error message (anti-enumeration) |
| 🟢 **P3** | F-04, F-05 | Frontend | Consolidate disabled states + scan for CSV formula injection |
| 🟢 **P3** | B1-09 | Endpoints | Add `consumes = "application/json"` on `@RequestBody` endpoints |

---

> IDs (`F-xx`, `B1-xx`, `B2-xx`, `B3-xx`) can be used directly to create tracking issues.
> P0 items must be addressed first as they represent either unauthorized data access (IDOR on account deletion), or unbounded brute-force and injection attack surfaces.
