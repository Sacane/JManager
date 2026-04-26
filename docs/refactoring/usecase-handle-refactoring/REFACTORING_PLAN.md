# REFACTORING_PLAN — UseCase Service Split + Command/Query + `handle`

**Generated on**: 2026-04-25  
**Applied pattern**: Use Case Split + Command/Query Object  
**Stack**: Kotlin 21 + Spring Boot 3.4.0 (Hexagonal Architecture)  
**Overall status**: ✅ Complete — 19 / 19

---

## Initial Analysis

### What was detected

- 45 `*UseCase` interfaces exist in `domain/port/input/{category}/`, each exposing **a single method named after its role** (`getCategoryDistribution`, `bookTransaction`, etc.)
- `*FeatureImpl` classes (e.g. `StatsFeatureImpl`, `BookletFeatureImpl`) still implement **both** the deprecated `*Feature` interface AND all new `*UseCase` interfaces — one method satisfies both simultaneously
- Renaming methods to `handle` causes **JVM signature collisions** in `*FeatureImpl` classes when two UseCases from the same impl share the same parameter signature (e.g. `getCategoryDistribution` and `getTrendStats` in `StatsFeatureImpl`)
- The only clean case already in place is `GetUsersService` (Admin): one class, one interface, in `domain/port/input/admin/`
- Shared private helpers exist in `*FeatureImpl` classes (`domainFailure()`, `validateDateRange()`, `userOwnsBooklet()`, etc.) — they must be extracted or duplicated during the split

### What will NOT be modified

- The internal business logic of each method (only relocated)
- SPI ports (`*Repository`, `SessionManager`, etc.)
- The domain model (`Booklet`, `Transaction`, `User`, etc.)
- Deprecated `*Feature` interfaces throughout the split phase (deletion is the final step)
- Infrastructure integration tests

### Justification of the chosen pattern

**Use Case Split**: `*FeatureImpl` classes satisfy multiple UseCases simultaneously, making uniform `handle` renaming impossible (JVM collision). Each service must implement exactly one UseCase interface.

**Command/Query Object**: once services are split, multiple `handle(token, bookletId?, startDate?, endDate?)` signatures in **separate classes** no longer collide. Command/Query objects are introduced to improve readability and maintainability — each `handle(cmd: XCommand)` clearly expresses the input contract.

---

## Target Convention

### Structure of a split UseCase

```
domain/port/input/{category}/
├── GetCategoryDistributionUseCase.kt     // interface { fun handle(query: GetCategoryDistributionQuery): Result<...> }
├── GetCategoryDistributionQuery.kt       // data class GetCategoryDistributionQuery(val token: SessionToken, ...)
└── GetCategoryDistributionService.kt     // @DomainService : GetCategoryDistributionUseCase { override fun handle(...) }
```

### Full example

```kotlin
// GetCategoryDistributionQuery.kt
data class GetCategoryDistributionQuery(
    val token: SessionToken,
    val bookletId: UUID? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

// GetCategoryDistributionUseCase.kt
@Port(Side.APPLICATION)
interface GetCategoryDistributionUseCase {
    fun handle(query: GetCategoryDistributionQuery): Result<CategoryDistributionOutput>
}

// GetCategoryDistributionService.kt
@DomainService
class GetCategoryDistributionService(
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val calculator: CategoryDistributionCalculator
) : GetCategoryDistributionUseCase {
    override fun handle(query: GetCategoryDistributionQuery): Result<CategoryDistributionOutput> { ... }
}
```

> **Note on defaults in Query objects**: default values (`= null`) CAN be placed on Query objects (simple data classes, not interfaces) — no risk of Kotlin double-default compile error.

---

## Refactoring Steps

---

### Step 1 — Admin: rename `getUsers` → `handle` (split already in place)

**Status**: ✅ Done  
**Blocking for**: None (Admin is independent)

**Actions:**
1. Create `GetUsersQuery.kt` in `domain/port/input/admin/`: `data class GetUsersQuery(val token: SessionToken, val pageNumber: Int = 0, val pageSize: Int = 20)`
2. Update `GetUsersUseCase.kt`: `fun handle(query: GetUsersQuery): Result<Page<User>>`
3. Update `GetUsersService.kt`: `override fun handle(query: GetUsersQuery)`
4. Update `application/api/admin/Controller.kt`

**Validation criterion:**
> The project compiles. `GetUsersService.getUsers` no longer exists. The Admin controller calls `.handle(GetUsersQuery(...))`.

---

### Step 2 — User: split `UserFeatureImpl` into 7 services

**Status**: ✅ Done  
**Depends on**: None  
**Objective**: Create 7 independent service classes in `domain/port/input/user/`.

**UseCases to split:**
| UseCase | Key dependencies |
|---|---|
| `LoginUseCase` | `SessionManager`, `UserRepository` |
| `LogoutUseCase` | `SessionManager` |
| `RefreshSessionUseCase` | `SessionManager` |
| `RegisterUserUseCase` | `UserRepository` |
| `CreateAdminIfNotExistsUseCase` | `UserRepository` |
| `GetUserSettingsUseCase` | `SessionManager`, `UserRepository` |
| `UpdateUserSettingsUseCase` | `SessionManager`, `UserRepository` |

**Actions:**
1. Read the implementation of each method in `UserFeatureImpl`
2. Create `LoginService.kt`, `LogoutService.kt`, `RefreshSessionService.kt`, `RegisterUserService.kt`, `CreateAdminIfNotExistsService.kt`, `GetUserSettingsService.kt`, `UpdateUserSettingsService.kt` in `domain/port/input/user/` — each `@DomainService` implementing a single UseCase, containing logic extracted from `UserFeatureImpl`
3. Remove `UserFeature` from the supertypes of `UserFeatureImpl` (which becomes empty)
4. Delete `UserFeatureImpl` (or keep empty temporarily)

**Validation criterion:**
> The project compiles. Spring resolves injection of each User UseCase from the new services. No controller references `UserFeatureImpl` or `UserFeature` anymore.

---

### Step 3 — User: Command/Query + rename `handle`

**Status**: ✅ Done  
**Depends on**: Step 2  
**Objective**: Introduce the 7 User Command/Query objects and rename methods to `handle`.

**Actions:**
1. Create 7 Query/Command objects in `domain/port/input/user/` (e.g. `LoginCommand(pseudonym, userPassword)`, `LogoutCommand(token)`, etc.)
2. Update each UseCase interface: `fun handle(cmd: XCommand): Result<...>`
3. Update each Service: `override fun handle(cmd: XCommand)`
4. Update `application/api/session/Controller.kt` and `application/configuration/DataLoader.kt`
5. Update domain tests `UserFeatureTest`

**Validation criterion:**
> Compiles. All User UseCase methods are named `handle`. `.\gradlew :domain:test --tests "*.UserFeatureTest"` passes.

---

### Step 4 — Tag: split `TagFeatureImpl` into 6 services

**Status**: ✅ Done  
**Depends on**: None (can be parallelised with Step 2)  
**Objective**: Create 6 services in `domain/port/input/tag/`.

**UseCases to split:**
| UseCase | Key dependencies |
|---|---|
| `AddTagUseCase` | `SessionManager`, `TagRepository` |
| `GetAllTagsUseCase` | `SessionManager`, `TagRepository` |
| `AddDefaultTagsUseCase` | `TagRepository` |
| `DeleteTagUseCase` | `SessionManager`, `TagRepository` |
| `DefaultTagUseCase` | `SessionManager`, `TagRepository` |
| `EditTagUseCase` | `SessionManager`, `TagRepository` |

**Actions:**
1. Create 6 `*Service` classes in `domain/port/input/tag/`, each with logic extracted from `TagFeatureImpl`
2. Remove `TagFeature` from the supertypes of `TagFeatureImpl`
3. Delete `TagFeatureImpl` (or empty it)

**Validation criterion:**
> Compiles. Spring injects the Tag services everywhere `TagFeature` was injected.

---

### Step 5 — Tag: Command/Query + rename `handle`

**Status**: ✅ Done  
**Depends on**: Step 4

**Actions:**
1. Create 6 Command/Query objects in `domain/port/input/tag/`
2. Update interfaces + services + controllers (`TagController`, `StatsController`, `DataLoader`)
3. Update `TagFeatureTest`

**Validation criterion:**
> Compiles. `.\gradlew :domain:test --tests "*.TagFeatureTest"` passes.

---

### Step 6 — Transaction: split `TransactionFeatureImpl` into 6 services

**Status**: ✅ Done  
**Depends on**: None

**UseCases to split:**
| UseCase | Key dependencies |
|---|---|
| `BookTransactionUseCase` | `SessionManager`, `BookletRepository`, `TagRepository` |
| `RetrieveTransactionsByMonthAndYearUseCase` | `SessionManager`, `BookletRepository` |
| `EditTransactionUseCase` | `SessionManager`, `BookletRepository` |
| `FindTransactionByIdUseCase` | `SessionManager`, `BookletRepository` |
| `DeleteTransactionsByIdsUseCase` | `SessionManager`, `BookletRepository` |
| `ConfirmPreviewTransactionUseCase` | `SessionManager`, `BookletRepository` |

**Actions:**
1. Create 6 `*Service` classes in `domain/port/input/transaction/`
2. Remove `TransactionFeature` from supertypes, delete `TransactionFeatureImpl`

**Validation criterion:**
> Compiles. `TransactionController` injects 6 Transaction services.

---

### Step 7 — Transaction: Command/Query + rename `handle`

**Status**: ✅ Done

**Actions:**
1. Create 6 Command/Query objects in `domain/port/input/transaction/`
2. Update interfaces + services + `TransactionController` + `TransactionStateTestAdapter`

**Validation criterion:**
> Compiles. `TransactionController` uses `.handle(XCommand(...))` for all transactions.

---

### Step 8 — RegularTransaction: split into 8 services

**Status**: ✅ Done  
**Depends on**: None

**UseCases**: `GetAllRegularTransactions`, `BookRegularTransaction`, `GetRegularTransactionById`, `UpdateRegularTransaction`, `DeleteRegularTransaction`, `DeleteRegularTransactions`, `LinkRegularTransactionToBooklet`, `UnlinkRegularTransactionFromBooklet`

> ⚠️ `RegularTransactionFeatureImpl` has important shared dependencies: `UnitOfWorkTransactionProvider`, `RegularTransactionTrackerRepository`, `Paginator`. Each service that needs them receives them via injection.

**Actions:**
1. Create 8 `*Service` classes in `domain/port/input/regularTransaction/`
2. Remove `RegularTransactionFeature` from supertypes, delete `RegularTransactionFeatureImpl`

**Validation criterion:**
> Compiles. `TransactionController` injects 8 RegularTransaction services.

---

### Step 9 — RegularTransaction: Command/Query + rename `handle`

**Status**: ✅ Done  
**Depends on**: Step 8

**Actions:**
1. Create 8 Command/Query objects in `domain/port/input/regularTransaction/`
2. Update interfaces + services + `TransactionController`

**Validation criterion:**
> Compiles. `TransactionController` uses `.handle(...)` for all Regular Transactions.

---

### Step 10 — Stats: split `StatsFeatureImpl` into 5 services

**Status**: ✅ Done  
**Depends on**: None

> ⚠️ `StatsFeatureImpl` contains `domainFailure()` and `validateDateRange()` — extract into a `StatsDomainHelper` or duplicate.

**UseCases:**
| UseCase | Dedicated calculator |
|---|---|
| `GetMonthlyBookletStatsUseCase` | `MonthlyStatsCalculator` |
| `GetCategoryDistributionUseCase` | `CategoryDistributionCalculator` |
| `GetTrendStatsUseCase` | `TrendCalculator` |
| `GetPrevisionalTransactionsUseCase` | `PrevisionalTransactionFilter` |
| `GetDailyTrendStatsUseCase` | `DailyTrendCalculator` |

**Actions:**
1. Create `StatsDomainHelper.kt` (if shared logic is non-trivial) or duplicate helpers
2. Create 5 `*Service` classes in `domain/port/input/stats/`
3. Remove `StatsFeature` from supertypes, delete `StatsFeatureImpl`
4. Update `FakeFactory.statsFeature` (type `StatsFeatureImpl` → separate types or remove)

**Validation criterion:**
> Compiles. `StatsController` injects 5 Stats services.

---

### Step 11 — Stats: Command/Query + rename `handle`

**Status**: ✅ Done  
**Depends on**: Step 10

**Actions:**
1. Create 5 Query objects in `domain/port/input/stats/`
2. Update interfaces + services + `StatsController`
3. Update `StatsFeatureTest`

**Validation criterion:**
> Compiles. `.\gradlew :domain:test --tests "*.StatsFeatureTest"` passes.

---

### Step 12 — Booklet: split `BookletFeatureImpl` into 9 services

**Status**: ✅ Done  
**Depends on**: None

> ⚠️ `BookletFeatureImpl` is the largest impl (~600 lines). Contains `domainFailure()`, `userOwnsBooklet()` — extract if shared between services.

**UseCases**: `FindBookletById`, `EditBooklet`, `DeleteBookletById`, `FindByLabelAndUserId`, `FindAllRegisteredBooklets`, `SaveBooklet`, `LoadTransactionsForBookletForAMonth`, `LoadBalancesForBookletForAMonth`, `RegenerateDeletedPrevisionalTransactions`

**Actions:**
1. Identify shared helpers in `BookletFeatureImpl`, create `BookletDomainHelper.kt` if needed
2. Create 9 `*Service` classes in `domain/port/input/booklet/`
3. Remove `BookletFeature` from supertypes, delete `BookletFeatureImpl`
4. Update `FakeFactory.bookletFeature`

**Validation criterion:**
> Compiles. `BookletController` and `TransactionController` inject the Booklet services.

---

### Step 13 — Booklet: Command/Query + rename `handle`

**Status**: ✅ Done  
**Depends on**: Step 12

> ⚠️ `LoadTransactionsForBookletForAMonthCommand` will have ~10 fields — this is expected for a complex operation.

**Actions:**
1. Create 9 Command/Query objects in `domain/port/input/booklet/`
2. Update interfaces + services + `BookletController` + `TransactionController`
3. Update `BookletFeatureTest`

**Validation criterion:**
> Compiles. `.\gradlew :domain:test --tests "*.BookletFeatureTest"` passes.

---

### Step 14 — CSV: split `FileImportExportFeatureImpl` into 3 services

**Status**: ✅ Done  
**Depends on**: None

**UseCases**: `ValidateCsvFile`, `ImportTransactionsFromCsv`, `ExportTransactionsToCsv`

**Actions:**
1. Create 3 `*Service` classes in `domain/port/input/csv/`
2. Remove `FileImportExportFeature` from supertypes, delete `FileImportExportFeatureImpl`

**Validation criterion:**
> Compiles. `CsvImportController` injects 3 CSV services.

---

### Step 15 — CSV: Command/Query + rename `handle`

**Status**: ✅ Done  
**Depends on**: Step 14

**Actions:**
1. Create 3 Command/Query objects in `domain/port/input/csv/`
2. Update interfaces + services + `CsvImportController`

**Validation criterion:**
> Compiles. `CsvImportController` uses `.handle(...)` for all CSV operations.

---

### Step 16 — Delete deprecated `*Feature` interfaces

**Status**: ✅ Done  
**Depends on**: Steps 2, 4, 6, 8, 10, 12, 14 (all split implementations complete)  
**Objective**: Permanently remove all deprecated `*Feature` interfaces and their files.

**Files to delete:**
- `domain/port/api/UserFeature.kt`
- `domain/port/api/TagFeature.kt`
- `domain/port/api/TransactionFeature.kt`
- `domain/port/api/RegularTransactionFeature.kt`
- `domain/port/api/StatsFeature.kt`
- `domain/port/api/BookletFeature.kt`
- `domain/port/api/FileImportExportFeature.kt`
- `domain/port/api/AdminFeature.kt`

**Actions:**
1. Verify no file references these interfaces anymore (`grep -r "UserFeature"`, etc.)
2. Delete the files
3. Remove orphan imports in FakeFactory and tests

**Validation criterion:**
> `.\gradlew build` passes entirely. No references to deprecated `*Feature` interfaces remain in the codebase.

---

### Step 17 — Final validation: full test suite

**Status**: ✅ Done  
**Depends on**: Step 16

**Actions:**
1. `.\gradlew :domain:test`
2. `.\gradlew :application:test`
3. `.\gradlew :infrastructure:test`

**Validation criterion:**
> All tests pass. No regression.

---

### Step 18 — Consolidate Command/Query + Service into UseCase files

**Status**: ✅ Done  
**Depends on**: Step 17  
**Objective**: Merge the Command/Query data classes and Service implementations into their corresponding UseCase interface files. Each use case becomes a single file containing: the input data class, the port interface, and the `@DomainService` implementation.

**Rationale**: The current structure produces 3 files per use case (`XCommand.kt`, `XUseCase.kt`, `XService.kt`). Since all three are tightly coupled — the Command/Query is referenced only by its UseCase, and the Service implements only that UseCase — they belong together. Consolidating reduces ~135 files to ~48 across all categories, making navigation and maintenance significantly easier.

**Target convention** (replaces the 3-file structure):

```
domain/port/input/{category}/
├── GetCategoryDistributionUseCase.kt     // contains Query + interface + @DomainService
└── StatsDomainHelper.kt                  // shared helpers stay in their own file
```

```kotlin
// GetCategoryDistributionUseCase.kt

data class GetCategoryDistributionQuery(
    val token: SessionToken,
    val bookletId: UUID? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

@Port(Side.APPLICATION)
interface GetCategoryDistributionUseCase {
    fun handle(query: GetCategoryDistributionQuery): Result<CategoryDistributionOutput>
}

@DomainService
class GetCategoryDistributionService(
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val calculator: CategoryDistributionCalculator
) : GetCategoryDistributionUseCase {
    override fun handle(query: GetCategoryDistributionQuery): Result<CategoryDistributionOutput> { ... }
}
```

**Files kept separate** (NOT merged):
- `StatsDomainHelper.kt`, `BookletDomainHelper.kt`, `CsvDomainHelper.kt` — shared helpers used by multiple services
- `BookletLoadingResult.kt`, `TransactionDeletionResult.kt` — return types used across packages (controllers, tests)

**Per-category actions:**

| Category | UseCase files | Command/Query files to merge | Service files to merge | Files deleted |
|---|---|---|---|---|
| admin | 1 (`GetUsers`) | 1 (`GetUsersQuery`) | 1 | **2** |
| user | 7 | 7 | 7 | **14** |
| tag | 6 (incl. `AddDefaultTags` — no Command) | 5 | 6 | **11** |
| transaction | 6 | 6 | 6 | **12** |
| regularTransaction | 8 | 8 | 8 | **16** |
| stats | 5 | 5 | 5 | **10** |
| booklet | 9 | 9 | 9 | **18** |
| csv | 3 (Commands already inlined) | 0 | 3 | **3** |
| **Total** | **45** | **41** | **45** | **86** |

**Procedure for each UseCase**:
1. Open `XUseCase.kt`
2. If a separate `XCommand.kt` / `XQuery.kt` exists, move its data class to the top of `XUseCase.kt` (before the interface)
3. Move the entire content of `XService.kt` (class + imports) to the bottom of `XUseCase.kt` (after the interface)
4. Consolidate imports (remove duplicates, remove same-package imports)
5. Delete `XCommand.kt` / `XQuery.kt` and `XService.kt`

**Import safety**: All external callers (controllers, tests) use wildcard imports (`import fr.sacane.jmanager.domain.port.input.user.*`), so class relocations within the same package require **no caller changes**.

**Execution order**: Process categories one at a time. Compile after each category to catch issues early:
1. admin → compile
2. user → compile
3. tag → compile
4. transaction → compile
5. regularTransaction → compile
6. stats → compile
7. booklet → compile
8. csv → compile

**Validation criterion:**
> `.\gradlew :domain:compileKotlin :domain:compileTestKotlin :application:compileKotlin :application:compileTestKotlin :infrastructure:compileKotlin :infrastructure:compileTestKotlin` passes. Each category has exactly one file per use case (plus helpers). No orphan `*Command.kt`, `*Query.kt`, or `*Service.kt` files remain.

---

### Step 19 — Final validation: full test suite (post-consolidation)

**Status**: ✅ Done  
**Depends on**: Step 18

**Actions:**
1. `.\gradlew :domain:test`
2. `.\gradlew :application:test`
3. `.\gradlew :infrastructure:test`

**Validation criterion:**
> All tests pass. No regression. Each `domain/port/input/{category}/` package contains only UseCase files and optional shared helpers.

---

## Recommended Execution Order

```
Step 1 (Admin rename)
    ↓
Step 2 ──── Step 4 ──── Step 6 ──── Step 8 ──── Step 10 ──── Step 12 ──── Step 14
(User split) (Tag split)  (Tx split)   (RT split)   (Stats split) (Bklt split)  (CSV split)
    ↓             ↓           ↓             ↓             ↓             ↓             ↓
Step 3      Step 5      Step 7      Step 9      Step 11     Step 13     Step 15
(User C/Q)   (Tag C/Q)    (Tx C/Q)    (RT C/Q)     (Stats C/Q)  (Bklt C/Q)   (CSV C/Q)
                                                                                    ↓
                                                                              Step 16 (cleanup)
                                                                                    ↓
                                                                              Step 17 (tests)
                                                                                    ↓
                                                                              Step 18 (consolidate)
                                                                                    ↓
                                                                              Step 19 (tests)
```

*Split steps (2, 4, 6, 8, 10, 12, 14) are independent from each other and can be done in any order. Command/Query steps (3, 5, 7, 9, 11, 13, 15) each depend on their respective split step.*

---

## Validation Protocol

At each step:
1. The AI announces the step and what it will do
2. The AI produces the modifications
3. The developer verifies according to the step's validation criterion
4. The developer replies **"OK"** (or requests an adjustment)
5. The AI updates the step status and moves to the next one

**Never skip a step without explicit confirmation.**

---

## Identified Risks

| Risk | Probability | Mitigation |
|------|-------------|------------|
| Shared helpers duplicated during split (`domainFailure`, `validateDateRange`) | High | Extract into `*DomainHelper.kt` or utility class in `domain/utils/` before splitting |
| `BookletFeatureImpl` very long — risk of missing logic | High | Read the entire file before creating the Booklet services |
| `RegularTransactionFeatureImpl` with `UnitOfWorkTransactionProvider` — atomic cross-service transaction | Medium | Keep `UnitOfWork` injection in each service that uses it |
| FakeFactory: references to `*FeatureImpl` in tests | Low | Replace with the new individual services in FakeFactory at each step |
| Spring Autowiring: ambiguous bean if multiple implementations of the same interface | Very low | Each UseCase has exactly one `@DomainService` implementation |

---
