# REFACTORING_PLAN — Extract Authentication from Domain Layer

**Generated on**: 2026-05-01  
**Applied pattern**: Hexagonal Architecture (removal of cross-cutting concern from domain)  
**Stack**: Kotlin + Spring Boot 3  
**Overall status**: ✅ Step 2 / 11 complete

---

## Initial Analysis

### What was detected

- **Double authentication**: the `JwtCookieAuthenticationFilter` (application layer) already validates JWTs and populates `SecurityContext` with the authenticated user (`currentUser`). Every domain service then re-authenticates the same token via `session.authenticate(command.token)`, resulting in redundant validation.
- **42 use cases** wrap their entire body inside `session.authenticate(token) { userId -> ... }`, creating massive boilerplate duplication of a cross-cutting concern.
- Every `Command` and `Query` data class carries a `token: SessionToken` field that is purely technical — it has no domain meaning.
- Every domain service injects `SessionManager` solely to call `authenticate()`, even though their actual domain logic only needs the `UserId`.
- `InMemorySessionManager` is annotated `@DomainService` and lives in `domain/port/output/`, but it implements JWT decoding, session expiration, role-weight checks — all infrastructure/application-level concerns.
- The `FeatureTest` base class in domain tests creates sessions and generates fake tokens just to call use cases — domain tests should not need to manipulate authentication mechanics.

### What will NOT be modified

- **Business logic** inside each use case (ownership checks, validation rules, calculations) — stays exactly as-is
- **Domain models** (`Booklet`, `Transaction`, `Tag`, `UserId`, `Amount`, etc.)
- **Output ports** (repository interfaces in `domain/port/output/repository/`)
- **The `JwtCookieAuthenticationFilter`** — it is correctly placed in the application layer
- **The `CommandBus` / `QueryBus` pattern** — kept as-is
- **Login, Register, RefreshSession, CreateAdminIfNotExists, AddDefaultTags** — these use cases legitimately interact with `SessionManager` for session lifecycle, they don't use `authenticate()` as a guard
- **Existing API test helpers** (`AuthenticatedUserTest`) — they use the real auth flow and will be adapted minimally
- **The `SessionManager` interface** as a SPI port for session lifecycle (addSession, removeSession, saveRefreshToken, etc.) — only the `authenticate()` convenience method is removed

### Justification of the chosen pattern

The authentication concern has leaked into the domain layer, violating the Hexagonal Architecture principle that the domain must remain isolated from infrastructure concerns. Token validation, session expiration, and role checks are not business rules — they are application-layer security concerns. The `JwtCookieAuthenticationFilter` already performs this work at the HTTP boundary, making the domain-level authentication fully redundant. Extracting it restores the domain's purity, eliminates ~42 instances of boilerplate duplication, and makes domain tests simpler and more focused on business behavior.

---

## Refactoring Steps

---

### Step 1 — Pilot: Migrate EditBookletUseCase end-to-end

**Status**: ✅ Done  
**Objective**: Validate the migration pattern on a single use case across all layers (domain → controller → tests) before scaling to the rest.  
**Blocking for**: Steps 2–8

**Actions:**

1. In `EditBookletCommand`, replace `val token: SessionToken` with `val userId: UserId`.
2. In `EditBookletService`:
   - Remove the `session: SessionManager` constructor parameter.
   - Replace `session.authenticate(command.token) { userId ->` with direct use of `command.userId`.
   - Remove the `authenticate` lambda wrapper — the body becomes the direct `handle()` body.
3. In the controller that dispatches `EditBookletCommand`, replace `SessionToken(currentUser.token)` with `UserId(currentUser.id)`.
4. In `FeatureTest`, add a new helper `launchWithUserId(action: UserIdBooklet.() -> Unit)` that provides a `UserId` and a `Booklet` without creating a session or generating a token. Keep the old helpers intact for use cases not yet migrated.
5. Update `BookletFeatureTest` tests that exercise `EditBookletUseCase` to use the new helper with `userId` instead of `SessionToken`.
6. Run `./gradlew domain:test` and `./gradlew application:test` — all must pass.

**Validation criterion:**
> The project compiles without error. `EditBookletService` no longer imports `SessionManager` or `SessionToken`. Domain tests for EditBooklet pass without creating sessions. The controller passes `UserId` instead of `SessionToken`.

---

### Step 2 — Migrate remaining booklet use cases (8 use cases)

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Apply the validated pattern to all booklet use cases.

**Use cases:**
- `DeleteBookletByIdUseCase`
- `FindBookletByIdUseCase`
- `FindAllRegisteredBookletsUseCase`
- `FindByLabelAndUserIdUseCase`
- `SaveBookletUseCase`
- `RegenerateDeletedPrevisionalTransactionsUseCase`
- `LoadTransactionsForBookletForAMonthUseCase`
- `LoadBalancesForBookletForAMonthUseCase`

**Actions:**
1. For each use case: replace `token: SessionToken` with `userId: UserId` in the Command/Query data class.
2. For each service: remove `SessionManager` dependency, unwrap the `session.authenticate` lambda, use `command.userId` / `query.userId` directly.
3. Update the booklet controller(s) to pass `UserId(currentUser.id)` instead of `SessionToken(currentUser.token)`.
4. Update `BookletFeatureTest` tests to use the new `userId`-based helper.
5. Run `./gradlew domain:test` and `./gradlew application:test`.

**Validation criterion:**
> No booklet use case service imports `SessionManager`. All booklet Commands/Queries use `userId: UserId`. All tests pass.

---

### Step 3 — Migrate tag use cases (5 use cases)

**Status**: ⏳ To do  
**Depends on**: Step 1  
**Non-blocking**: Can run in parallel with Steps 2, 4–8

**Use cases:**
- `AddTagUseCase`
- `EditTagUseCase`
- `DeleteTagUseCase`
- `DefaultTagUseCase`
- `GetAllTagsUseCase`

**Actions:**
1. For each use case: replace `token: SessionToken` with `userId: UserId` in the Command/Query.
2. For each service: remove `SessionManager`, unwrap `session.authenticate`, use `command.userId` / `query.userId`.
3. Update the tag controller to pass `UserId(currentUser.id)`.
4. Update tag-related domain tests.
5. Run `./gradlew domain:test` and `./gradlew application:test`.

**Validation criterion:**
> No tag service imports `SessionManager`. All tag Commands/Queries use `userId: UserId`. All tests pass.

---

### Step 4 — Migrate transaction use cases (8 use cases)

**Status**: ⏳ To do  
**Depends on**: Step 1  
**Non-blocking**: Can run in parallel with Steps 2, 3, 5–8

**Use cases:**
- `BookTransactionUseCase`
- `EditTransactionUseCase`
- `FindTransactionByIdUseCase`
- `RetrieveTransactionsByMonthAndYearUseCase`
- `ConfirmPreviewTransactionUseCase`
- `ConfirmVirtualTransactionUseCase`
- `DeleteTransactionsByIdsUseCase`
- `ExcludeVirtualTransactionUseCase`

**Actions:**
1. For each use case: replace `token: SessionToken` with `userId: UserId` in the Command/Query.
2. For each service: remove `SessionManager`, unwrap `session.authenticate`, use `command.userId` / `query.userId`.
3. Note: `EditTransactionUseCase` passes `roleUser` as `requiredRoles` — this role check is removed from the domain (Spring Security already enforces roles at the API level).
4. Update `TransactionController` to pass `UserId(currentUser.id)`.
5. Update transaction-related domain tests.
6. Run `./gradlew domain:test` and `./gradlew application:test`.

**Validation criterion:**
> No transaction service imports `SessionManager`. All transaction Commands/Queries use `userId: UserId`. All tests pass.

---

### Step 5 — Migrate regular transaction use cases (8 use cases)

**Status**: ⏳ To do  
**Depends on**: Step 1  
**Non-blocking**: Can run in parallel with Steps 2–4, 6–8

**Use cases:**
- `BookRegularTransactionUseCase`
- `DeleteRegularTransactionUseCase`
- `DeleteRegularTransactionsUseCase`
- `LinkRegularTransactionToBookletUseCase`
- `UnlinkRegularTransactionFromBookletUseCase`
- `UpdateRegularTransactionUseCase`
- `GetAllRegularTransactionsUseCase`
- `GetRegularTransactionByIdUseCase`

**Actions:**
1. For each use case: replace `token: SessionToken` with `userId: UserId`.
2. For each service: remove `SessionManager`, unwrap `session.authenticate`, use `command.userId` / `query.userId`.
3. Update the regular transaction controller to pass `UserId(currentUser.id)`.
4. Update `RegularTransactionFeatureTest`.
5. Run `./gradlew domain:test` and `./gradlew application:test`.

**Validation criterion:**
> No regular transaction service imports `SessionManager`. All Commands/Queries use `userId: UserId`. All tests pass.

---

### Step 6 — Migrate stats use cases (5 use cases)

**Status**: ⏳ To do  
**Depends on**: Step 1  
**Non-blocking**: Can run in parallel with Steps 2–5, 7–8

**Use cases:**
- `GetTrendStatsUseCase`
- `GetPrevisionalTransactionsUseCase`
- `GetMonthlyBookletStatsUseCase`
- `GetDailyTrendStatsUseCase`
- `GetCategoryDistributionUseCase`

**Actions:**
1. For each use case: replace `token: SessionToken` with `userId: UserId`.
2. For each service: remove `SessionManager`, unwrap `session.authenticate`, use `query.userId`.
3. Update the stats controller to pass `UserId(currentUser.id)`.
4. Update `StatsFeatureTest`.
5. Run `./gradlew domain:test` and `./gradlew application:test`.

**Validation criterion:**
> No stats service imports `SessionManager`. All stats Queries use `userId: UserId`. All tests pass.

---

### Step 7 — Migrate CSV use cases (3 use cases)

**Status**: ⏳ To do  
**Depends on**: Step 1  
**Non-blocking**: Can run in parallel with Steps 2–6, 8

**Use cases:**
- `ImportTransactionsFromCsvUseCase`
- `ExportTransactionsToCsvUseCase`
- `ValidateCsvFileUseCase`

**Actions:**
1. For each use case: replace `token: SessionToken` with `userId: UserId`.
2. For each service: remove `SessionManager`, unwrap `sessionManager.authenticate`, use `command.userId` / `query.userId`.
3. Update the CSV controller to pass `UserId(currentUser.id)`.
4. Update `FileImportExportFeatureTest`.
5. Run `./gradlew domain:test` and `./gradlew application:test`.

**Validation criterion:**
> No CSV service imports `SessionManager`. All CSV Commands/Queries use `userId: UserId`. All tests pass.

---

### Step 8 — Migrate user settings & admin use cases (3 use cases)

**Status**: ⏳ To do  
**Depends on**: Step 1  
**Non-blocking**: Can run in parallel with Steps 2–7

**Use cases:**
- `GetUserSettingsUseCase`
- `UpdateUserSettingsUseCase`
- `GetUsersUseCase` (admin — note: this one used `requiredRoles = roleAdmin`, which will be handled by Spring Security `@PreAuthorize` instead)

**Actions:**
1. For each use case: replace `token: SessionToken` with `userId: UserId`.
2. For each service: remove `SessionManager`, unwrap `session.authenticate` / `sessionManager.authenticate`, use `command.userId` / `query.userId`.
3. For `GetUsersUseCase`: the `requiredRoles = roleAdmin` check is removed from the domain. Ensure the admin controller endpoint is secured with `@PreAuthorize("hasRole('ADMIN')")` or equivalent Spring Security configuration.
4. Update `SessionController` and admin controller to pass `UserId(currentUser.id)`.
5. Update `UserFeatureTest` and `AdminFeatureTest` tests.
6. Run `./gradlew domain:test` and `./gradlew application:test`.

**Validation criterion:**
> No user/admin service imports `SessionManager` for authentication. The admin endpoint is secured via Spring Security. All Commands/Queries use `userId: UserId`. All tests pass.

---

### Step 9 — Refactor LogoutUseCase

**Status**: ⏳ To do  
**Depends on**: Steps 2–8  
**Objective**: Logout legitimately needs `SessionManager` for session lifecycle, but it should no longer use `authenticate()` as a guard wrapper.

**Actions:**
1. In `LogoutCommand`, replace `val token: SessionToken` with `val userId: UserId` and `val token: SessionToken` (keep token for session removal, add userId for identification).
2. In `LogoutService`:
   - Remove the `session.authenticate(command.token)` wrapper.
   - Call `session.findSessionByToken(command.token)` directly.
   - Call `session.removeSession(command.userId, command.token)` and `session.blacklistRefreshToken(...)` directly.
3. Update the logout controller to pass both `UserId(currentUser.id)` and `SessionToken(currentUser.token)`.
4. Update logout-related tests.
5. Run `./gradlew domain:test` and `./gradlew application:test`.

**Validation criterion:**
> `LogoutService` no longer calls `session.authenticate()`. It still uses `SessionManager` for `removeSession` and `blacklistRefreshToken` (legitimate session lifecycle). Tests pass.

---

### Step 10 — Clean up SessionManager interface & relocate implementation

**Status**: ⏳ To do  
**Depends on**: Step 9  
**Objective**: Remove the `authenticate()` method from `SessionManager` and move `InMemorySessionManager` out of the domain.

**Actions:**
1. Remove the `authenticate()` method from the `SessionManager` interface (it should have zero callers at this point).
2. Remove the `authenticateRefreshToken()` method from `SessionManager` if `RefreshSessionUseCase` is the only caller — evaluate whether to keep it on the interface or inline it.
3. Remove `@DomainService` from `InMemorySessionManager`.
4. Move `InMemorySessionManager` from `domain/port/output/SessionManager.kt` to `application/` (e.g. `application/session/InMemorySessionManager.kt`), annotate it with `@Component` or `@Service`.
5. Keep the `SessionManager` interface in `domain/port/output/` — it's still a legitimate SPI port for session lifecycle.
6. Update `HexagonInjectionConfiguration` if needed.
7. Update `FakeFactory` and `FeatureTest` — domain tests should use a minimal fake `SessionManager` that only supports `addSession` / `removeSession` / `findSessionByToken` (for Login/Logout tests).
8. Remove `Role`, `weight()`, and role-checking logic from `SessionManager` if no longer needed there.
9. Run `./gradlew test` (all modules).

**Validation criterion:**
> `SessionManager` interface no longer has an `authenticate()` method. `InMemorySessionManager` lives in the application module. No domain service calls `session.authenticate()`. The full test suite passes.

---

### Step 11 — Final cleanup & validation

**Status**: ⏳ To do  
**Depends on**: Step 10  
**Objective**: Remove all traces of the old pattern and verify the domain is clean.

**Actions:**
1. Remove the old `launchWithConnectedUserInstance` and `launchWithConnectedUserWithoutBooklet` helpers from `FeatureTest` if they are no longer used.
2. Run a grep across `domain/src/main/` for `SessionToken` — the only occurrences should be in use cases that legitimately handle sessions (Login, Logout, RefreshSession).
3. Run a grep across `domain/src/main/` for `session.authenticate` — must return **zero** results.
4. Run a grep across `domain/src/main/` for `import.*SessionManager` — should only appear in Login, Logout, RefreshSession services.
5. Run `./gradlew test` — full green across all modules.
6. Run `./gradlew build` — ensure no warnings or errors.
7. Update `Changelog.md` with the refactoring entry.

**Validation criterion:**
> Zero occurrences of `session.authenticate` in `domain/src/main/`. No `SessionToken` in domain Commands/Queries (except Login/Logout/RefreshSession). Full test suite green. Changelog updated.

---

## Recommended Execution Order

```
Step 1 (pilot)
    ├──► Step 2 (booklets)
    ├──► Step 3 (tags)         ──┐
    ├──► Step 4 (transactions)   │
    ├──► Step 5 (regular tx)     ├──► Step 9 (logout) ──► Step 10 (cleanup SM) ──► Step 11 (final)
    ├──► Step 6 (stats)          │
    ├──► Step 7 (csv)            │
    └──► Step 8 (user/admin)  ──┘
```

*Steps 2–8 are independent and can be executed in any order or in parallel after Step 1 is validated.*

---

## Validation Protocol

At each step:
1. The AI announces the step and what it will do
2. The AI produces the modifications
3. The dev verifies according to the step's validation criterion
4. The dev replies **"OK"** (or requests an adjustment)
5. The AI updates the step status and moves to the next one

**Never skip a step without explicit confirmation.**

---

## Identified Risks

| Risk | Probability | Mitigation |
|------|-------------|------------|
| Spring Security role enforcement missing for admin endpoints | Medium | Step 8 explicitly adds `@PreAuthorize` or verifies existing `SecurityConfig` rules |
| Domain tests break during intermediate steps (old helpers still reference token) | Medium | Step 1 adds new helpers without removing old ones; old helpers removed only in Step 11 |
| `RefreshSessionUseCase` depends on `authenticateRefreshToken()` which stays in SessionManager | Low | Evaluate in Step 10 whether to keep or inline — it's still a valid session lifecycle operation |
| Some controllers use `currentUser.token` for other purposes (e.g. passing to external services) | Low | Grep for `currentUser.token` usage outside of Command/Query construction and handle case by case |
| `InMemorySessionManager` relocation breaks Spring component scanning | Low | Step 10 verifies `@ComponentScan` or adds explicit `@Bean` definition |
| Integration tests (`AuthenticatedUserTest`) break when Commands/Queries change | Medium | Integration tests are updated alongside each category step; the real auth flow still works, only command construction changes |

---

## Affected Files Summary

### Per use case category (Steps 1–8)

| Category | Use cases | Domain files | Controller files | Test files |
|----------|-----------|--------------|------------------|------------|
| Booklet | 9 | 9 `*UseCase.kt` | Booklet controller | `BookletFeatureTest` |
| Tag | 5 | 5 `*UseCase.kt` | Tag controller | Tag-related tests |
| Transaction | 8 | 8 `*UseCase.kt` | `TransactionController` | Transaction tests |
| Regular Tx | 8 | 8 `*UseCase.kt` | RegularTx controller | `RegularTransactionFeatureTest` |
| Stats | 5 | 5 `*UseCase.kt` | Stats controller | `StatsFeatureTest` |
| CSV | 3 | 3 `*UseCase.kt` | CSV controller | `FileImportExportFeatureTest` |
| User/Admin | 3 | 3 `*UseCase.kt` | `SessionController`, Admin controller | `UserFeatureTest`, `AdminFeatureTest` |
| Logout | 1 | 1 `*UseCase.kt` | `SessionController` | `UserFeatureTest` |

### Cleanup (Steps 10–11)

- `domain/port/output/SessionManager.kt` — remove `authenticate()`, extract `InMemorySessionManager`
- `application/` — new location for `InMemorySessionManager`
- `domain/src/test/**/FeatureTest.kt` — remove old token-based helpers
- `domain/src/test/**/FakeFactory.kt` — simplify session fake
- `Changelog.md`

---

## References
- [Hexagonal Architecture — Alistair Cockburn](https://alistair.cockburn.us/hexagonal-architecture/)
- Previous refactoring: `docs/refactoring/command-query-bus/`
