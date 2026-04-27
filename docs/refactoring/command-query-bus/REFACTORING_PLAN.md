# REFACTORING_PLAN — Command/Query Bus Pattern

**Generated on**: 2026-04-27  
**Applied pattern**: Command/Query Bus (Mediator)  
**Stack**: Kotlin 21 + Spring Boot 3.4.0 (Hexagonal Architecture)  
**Overall status**: ✅ Complete — All 10 steps done

---

## Initial Analysis

### What was detected

- 45 `*UseCase` interfaces in `domain/port/input/{category}/`, each with a single `handle(command/query)` method following the UseCase Split + Command/Query convention established by the previous refactoring
- Controllers inject **every individual UseCase** they need, leading to massive constructor parameter lists:
  - `TransactionController`: **14 UseCase injections**
  - `BookletController`: **7 UseCase injections**
  - `SessionController`: **6 UseCase injections + 2 other dependencies**
  - `StatsController`: **6 UseCase injections**
  - `TagController`: **5 UseCase injections**
  - `CsvImportController`: **3 UseCase injections + 1 repository**
  - `AdminController`: **1 UseCase injection**
- Every controller method follows the same pattern: build Command/Query from DTO → call `useCase.handle(...)` → map result → return HTTP response
- Command/Query data classes and UseCase interfaces have **no common supertype** — there is no base interface enabling polymorphic dispatching
- `AddDefaultTagsUseCase` is a special case: `handle()` takes no parameters and returns `Unit` — it is only called from `DataLoader` (not a controller)
- `@DomainService` annotation is resolved by Spring `ComponentScan` with `FilterType.ANNOTATION` in `HexagonInjectionConfiguration`

### What will NOT be modified

- Domain services (`*Service`) and their internal business logic
- SPI ports (`*Repository`, `SessionManager`, etc.)
- Domain models (`Booklet`, `Transaction`, `User`, etc.)
- Domain tests (they test services directly, not through the bus)
- Infrastructure integration tests
- The `DataLoader` component (keeps direct UseCase injection)
- The `AddDefaultTagsUseCase` (no Command parameter, stays as-is)

### Justification of the chosen pattern

**Command/Query Bus**: The current architecture already follows Command/Query separation — each UseCase exposes `handle(command)` or `handle(query)` with explicit input objects. The missing piece is a **common dispatch mechanism** that eliminates the need for controllers to inject every individual UseCase. A bus collects all handlers via Spring's dependency injection, resolves the correct handler at runtime based on the Command/Query type, and dispatches. This reduces controller constructors from N UseCase parameters to 2 (`CommandBus`, `QueryBus`), while the domain dispatch contract remains clean and framework-free.

---

## Target Convention

### Foundation types (domain)

```kotlin
// domain/port/input/CommandHandling.kt
package fr.sacane.jmanager.domain.port.input

import fr.sacane.jmanager.domain.utils.Result
import kotlin.reflect.KClass

/**
 * Marker interface for command objects (write operations).
 * @param R the result type produced by the handler.
 */
interface Command<R>

/**
 * Handler contract for a command.
 * Each UseCase performing a write operation extends this interface.
 */
interface CommandHandler<C : Command<R>, R> {
    /** The concrete [Command] type this handler is responsible for. */
    val commandClass: KClass<C>
    fun handle(command: C): Result<R>
}
```

```kotlin
// domain/port/input/QueryHandling.kt
package fr.sacane.jmanager.domain.port.input

import fr.sacane.jmanager.domain.utils.Result
import kotlin.reflect.KClass

/**
 * Marker interface for query objects (read operations).
 * @param R the result type produced by the handler.
 */
interface Query<R>

/**
 * Handler contract for a query.
 * Each UseCase performing a read operation extends this interface.
 */
interface QueryHandler<Q : Query<R>, R> {
    /** The concrete [Query] type this handler is responsible for. */
    val queryClass: KClass<Q>
    fun handle(query: Q): Result<R>
}
```

> **Design note — `Command<Nothing>` and KClass**: Kotlin omits the JVM Signature attribute for interfaces parameterised with `Nothing`, which causes `GenericTypeResolver` and `getGenericInterfaces()` to return raw types, making type resolution impossible. The chosen solution is an explicit abstract `val commandClass: KClass<C>` property on the handler interface, overridden by each UseCase interface with a default getter (`override val commandClass get() = XxxCommand::class`). This approach is fully static (no reflection), type-safe, and requires zero changes on the `@DomainService` implementation classes.

### Updated UseCase example

```kotlin
// Before
data class LoginCommand(val pseudonym: String, val userPassword: String)

@Port(Side.APPLICATION)
interface LoginUseCase {
    fun handle(command: LoginCommand): Result<UserToken>
}

// After
data class LoginCommand(val pseudonym: String, val userPassword: String) : Command<UserToken>

@Port(Side.APPLICATION)
interface LoginUseCase : CommandHandler<LoginCommand, UserToken> {
    override val commandClass get() = LoginCommand::class
}
```

> The UseCase interface no longer needs to redeclare `handle()` — it is inherited from `CommandHandler`.  
> Each UseCase interface provides a default getter for `commandClass`/`queryClass`.  
> The Service implementation requires **no changes** — it inherits the property from the interface.

### Bus (application)

```kotlin
// application/bus/CommandBus.kt
interface CommandBus {
    fun <R> dispatch(command: Command<R>): Result<R>
}

@Component
class SpringCommandBus(handlers: List<CommandHandler<*, *>>) : CommandBus {
    // commandClass.java returns Class<out Command<*>> due to star projection;
    // cast to Class<*> is safe because we only use this map as a lookup key.
    private val handlerMap: Map<Class<*>, CommandHandler<*, *>> =
        handlers.associateBy { it.commandClass.java as Class<*> }

    @Suppress("UNCHECKED_CAST")
    override fun <R> dispatch(command: Command<R>): Result<R> {
        val handler = handlerMap[command::class.java] as? CommandHandler<Command<R>, R>
            ?: throw IllegalArgumentException("No handler registered for ${command::class.simpleName}")
        return handler.handle(command)
    }
}
```

### Controller transformation

```kotlin
// Before — TransactionController (14 injections)
class TransactionController(
    private val bookTransactionUseCase: BookTransactionUseCase,
    private val deleteTransactionsByIdsUseCase: DeleteTransactionsByIdsUseCase,
    // ... 12 more UseCase injections
)

// After — TransactionController (2 injections)
class TransactionController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
)

// Method change:
// Before: bookTransactionUseCase.handle(BookTransactionCommand(...))
// After:  commandBus.dispatch(BookTransactionCommand(...))
```

---

## Classification — Command vs Query

Classification follows the existing naming convention: data classes ending with `Command` → `CommandBus`, ending with `Query` → `QueryBus`.

### Full inventory (44 UseCases)

| Category | UseCase | Input type | Result type | Bus |
|---|---|---|---|---|
| **admin** | `GetUsersUseCase` | `GetUsersQuery` | `Page<User>` | Query |
| **user** | `LoginUseCase` | `LoginCommand` | `UserToken` | Command |
| **user** | `LogoutUseCase` | `LogoutCommand` | `Nothing` | Command |
| **user** | `RefreshSessionUseCase` | `RefreshSessionCommand` | `UserToken` | Command |
| **user** | `RegisterUserUseCase` | `RegisterUserCommand` | `User` | Command |
| **user** | `CreateAdminIfNotExistsUseCase` | `CreateAdminIfNotExistsCommand` | `User` | Command |
| **user** | `GetUserSettingsUseCase` | `GetUserSettingsQuery` | `UserSettings` | Query |
| **user** | `UpdateUserSettingsUseCase` | `UpdateUserSettingsCommand` | `UserSettings` | Command |
| **tag** | `AddTagUseCase` | `AddTagCommand` | `Tag` | Command |
| **tag** | `GetAllTagsUseCase` | `GetAllTagsQuery` | `List<Tag>` | Query |
| **tag** | `DeleteTagUseCase` | `DeleteTagCommand` | `Nothing` | Command |
| **tag** | `DefaultTagUseCase` | `DefaultTagQuery` | `Tag` | Query |
| **tag** | `EditTagUseCase` | `EditTagCommand` | `Tag` | Command |
| **transaction** | `BookTransactionUseCase` | `BookTransactionCommand` | `TransactionResumeResult` | Command |
| **transaction** | `RetrieveTransactionsByMonthAndYearUseCase` | `RetrieveTransactionsByMonthAndYearQuery` | `List<Transaction>` | Query |
| **transaction** | `EditTransactionUseCase` | `EditTransactionCommand` | `TransactionResumeResult` | Command |
| **transaction** | `FindTransactionByIdUseCase` | `FindTransactionByIdQuery` | `Transaction` | Query |
| **transaction** | `DeleteTransactionsByIdsUseCase` | `DeleteTransactionsByIdsCommand` | `TransactionDeletionResult` | Command |
| **transaction** | `ConfirmPreviewTransactionUseCase` | `ConfirmPreviewTransactionCommand` | `TransactionResumeResult` | Command |
| **regularTransaction** | `GetAllRegularTransactionsUseCase` | `GetAllRegularTransactionsQuery` | `Page<RegularTransaction>` | Query |
| **regularTransaction** | `BookRegularTransactionUseCase` | `BookRegularTransactionCommand` | `RegularTransaction` | Command |
| **regularTransaction** | `GetRegularTransactionByIdUseCase` | `GetRegularTransactionByIdQuery` | `RegularTransaction` | Query |
| **regularTransaction** | `UpdateRegularTransactionUseCase` | `UpdateRegularTransactionCommand` | `RegularTransaction` | Command |
| **regularTransaction** | `DeleteRegularTransactionUseCase` | `DeleteRegularTransactionCommand` | `Boolean` | Command |
| **regularTransaction** | `DeleteRegularTransactionsUseCase` | `DeleteRegularTransactionsCommand` | `List<String>` | Command |
| **regularTransaction** | `LinkRegularTransactionToBookletUseCase` | `LinkRegularTransactionToBookletCommand` | `RegularTransaction` | Command |
| **regularTransaction** | `UnlinkRegularTransactionFromBookletUseCase` | `UnlinkRegularTransactionFromBookletCommand` | `RegularTransaction` | Command |
| **stats** | `GetMonthlyBookletStatsUseCase` | `GetMonthlyBookletStatsQuery` | `MonthlyBookletStatsOutput` | Query |
| **stats** | `GetCategoryDistributionUseCase` | `GetCategoryDistributionQuery` | `CategoryDistributionOutput` | Query |
| **stats** | `GetTrendStatsUseCase` | `GetTrendStatsQuery` | `TrendStatsOutput` | Query |
| **stats** | `GetPrevisionalTransactionsUseCase` | `GetPrevisionalTransactionsQuery` | `PrevisionalTransactionsOutput` | Query |
| **stats** | `GetDailyTrendStatsUseCase` | `GetDailyTrendStatsQuery` | `DailyTrendStatsOutput` | Query |
| **booklet** | `SaveBookletUseCase` | `SaveBookletCommand` | `Booklet` | Command |
| **booklet** | `FindAllRegisteredBookletsUseCase` | `FindAllRegisteredBookletsQuery` | `List<Booklet>` | Query |
| **booklet** | `DeleteBookletByIdUseCase` | `DeleteBookletByIdCommand` | `Nothing` | Command |
| **booklet** | `FindBookletByIdUseCase` | `FindBookletByIdQuery` | `Booklet` | Query |
| **booklet** | `LoadTransactionsForBookletForAMonthUseCase` | `LoadTransactionsForBookletForAMonthQuery` | `BookletLoadingResult` | Query |
| **booklet** | `LoadBalancesForBookletForAMonthUseCase` | `LoadBalancesForBookletForAMonthQuery` | `BookletBalances` | Query |
| **booklet** | `RegenerateDeletedPrevisionalTransactionsUseCase` | `RegenerateDeletedPrevisionalTransactionsCommand` | `List<Transaction>` | Command |
| **booklet** | `EditBookletUseCase` | `EditBookletCommand` | `Booklet` | Command |
| **booklet** | `FindByLabelAndUserIdUseCase` | `FindByLabelAndUserIdQuery` | `Booklet` | Query |
| **csv** | `ValidateCsvFileUseCase` | `ValidateCsvFileQuery` | `CsvValidationReport` | Query |
| **csv** | `ImportTransactionsFromCsvUseCase` | `ImportTransactionsFromCsvCommand` | `CsvImportResult` | Command |
| **csv** | `ExportTransactionsToCsvUseCase` | `ExportTransactionsToCsvCommand` | `String` | Command |

**Excluded**: `AddDefaultTagsUseCase` — `handle()` takes no parameters, not controller-routed, stays as-is.

---

## Refactoring Steps

---

### Step 1 — Create foundation types in domain

**Status**: ✅ Complete  
**Blocking for**: Steps 2, 3, 4, 5

**Objective**: Introduce `Command<R>`, `Query<R>`, `CommandHandler<C, R>`, `QueryHandler<Q, R>` base interfaces in the domain layer.

**Actions:**
1. Create `domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/CommandHandling.kt`:
   - `Command<R>` marker interface
   - `CommandHandler<C : Command<R>, R>` interface declaring `fun handle(command: C): Result<R>`
2. Create `domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/QueryHandling.kt`:
   - `Query<R>` marker interface
   - `QueryHandler<Q : Query<R>, R>` interface declaring `fun handle(query: Q): Result<R>`

**Validation criterion:**
> `.\gradlew :domain:compileKotlin` passes. No existing code is affected — these are new, unused interfaces.

---

### Step 2 — Create bus infrastructure in application

**Status**: ✅ Complete  
**Depends on**: Step 1  
**Blocking for**: Steps 6, 7, 8, 9

**Objective**: Create `CommandBus` and `QueryBus` interfaces with Spring-backed implementations that auto-discover handlers at startup.

**Actions:**
1. Create `application/src/main/kotlin/fr/sacane/jmanager/application/bus/CommandBus.kt`:
   - `CommandBus` interface with `fun <R> dispatch(command: Command<R>): Result<R>`
   - `SpringCommandBus` `@Component` implementation using `GenericTypeResolver` to build a `Map<Class<*>, CommandHandler<*, *>>` at construction time
   - Startup log listing all discovered command handlers
2. Create `application/src/main/kotlin/fr/sacane/jmanager/application/bus/QueryBus.kt`:
   - `QueryBus` interface with `fun <R> dispatch(query: Query<R>): Result<R>`
   - `SpringQueryBus` `@Component` implementation, same approach
   - Startup log listing all discovered query handlers

**Validation criterion:**
> `.\gradlew :application:compileKotlin` passes. Bus beans are created but discover zero handlers (no UseCase extends the base interfaces yet).

---

### Step 3 — Update admin + user + tag domain types

**Status**: ✅ Complete  
**Depends on**: Step 1  
**Blocking for**: Steps 6, 7

**Objective**: Make 13 Command/Query data classes implement `Command<R>` or `Query<R>`, and 13 UseCase interfaces extend `CommandHandler` or `QueryHandler`. (AddDefaultTagsUseCase excluded.)

**UseCases to update:**

| UseCase | Data class | Marker | Result type |
|---|---|---|---|
| `GetUsersUseCase` | `GetUsersQuery` | `Query` | `Page<User>` |
| `LoginUseCase` | `LoginCommand` | `Command` | `UserToken` |
| `LogoutUseCase` | `LogoutCommand` | `Command` | `Nothing` |
| `RefreshSessionUseCase` | `RefreshSessionCommand` | `Command` | `UserToken` |
| `RegisterUserUseCase` | `RegisterUserCommand` | `Command` | `User` |
| `CreateAdminIfNotExistsUseCase` | `CreateAdminIfNotExistsCommand` | `Command` | `User` |
| `GetUserSettingsUseCase` | `GetUserSettingsQuery` | `Query` | `UserSettings` |
| `UpdateUserSettingsUseCase` | `UpdateUserSettingsCommand` | `Command` | `UserSettings` |
| `AddTagUseCase` | `AddTagCommand` | `Command` | `Tag` |
| `GetAllTagsUseCase` | `GetAllTagsQuery` | `Query` | `List<Tag>` |
| `DeleteTagUseCase` | `DeleteTagCommand` | `Command` | `Nothing` |
| `DefaultTagUseCase` | `DefaultTagQuery` | `Query` | `Tag` |
| `EditTagUseCase` | `EditTagCommand` | `Command` | `Tag` |

**Actions:**
1. For each UseCase file listed above:
   a. Add `: Command<ReturnType>` or `: Query<ReturnType>` to the data class declaration
   b. Replace the UseCase interface body with `: CommandHandler<DataClass, ReturnType>` or `: QueryHandler<DataClass, ReturnType>` (remove the explicit `handle()` declaration — it is inherited)
   c. Add import for `Command`/`Query`/`CommandHandler`/`QueryHandler` from `fr.sacane.jmanager.domain.port.input`

**Validation criterion:**
> `.\gradlew :domain:compileKotlin :domain:compileTestKotlin :application:compileKotlin :application:compileTestKotlin` passes. All existing code continues to work — adding supertypes is non-breaking.

---

### Step 4 — Update transaction + regularTransaction domain types

**Status**: ✅ Complete  
**Depends on**: Step 1  
**Blocking for**: Step 9

**Objective**: Make 14 Command/Query data classes implement markers and 14 UseCase interfaces extend handlers.

**UseCases to update:**

| UseCase | Data class | Marker | Result type |
|---|---|---|---|
| `BookTransactionUseCase` | `BookTransactionCommand` | `Command` | `TransactionResumeResult` |
| `RetrieveTransactionsByMonthAndYearUseCase` | `RetrieveTransactionsByMonthAndYearQuery` | `Query` | `List<Transaction>` |
| `EditTransactionUseCase` | `EditTransactionCommand` | `Command` | `TransactionResumeResult` |
| `FindTransactionByIdUseCase` | `FindTransactionByIdQuery` | `Query` | `Transaction` |
| `DeleteTransactionsByIdsUseCase` | `DeleteTransactionsByIdsCommand` | `Command` | `TransactionDeletionResult` |
| `ConfirmPreviewTransactionUseCase` | `ConfirmPreviewTransactionCommand` | `Command` | `TransactionResumeResult` |
| `GetAllRegularTransactionsUseCase` | `GetAllRegularTransactionsQuery` | `Query` | `Page<RegularTransaction>` |
| `BookRegularTransactionUseCase` | `BookRegularTransactionCommand` | `Command` | `RegularTransaction` |
| `GetRegularTransactionByIdUseCase` | `GetRegularTransactionByIdQuery` | `Query` | `RegularTransaction` |
| `UpdateRegularTransactionUseCase` | `UpdateRegularTransactionCommand` | `Command` | `RegularTransaction` |
| `DeleteRegularTransactionUseCase` | `DeleteRegularTransactionCommand` | `Command` | `Boolean` |
| `DeleteRegularTransactionsUseCase` | `DeleteRegularTransactionsCommand` | `Command` | `List<String>` |
| `LinkRegularTransactionToBookletUseCase` | `LinkRegularTransactionToBookletCommand` | `Command` | `RegularTransaction` |
| `UnlinkRegularTransactionFromBookletUseCase` | `UnlinkRegularTransactionFromBookletCommand` | `Command` | `RegularTransaction` |

**Actions:**
Same as Step 3, for each UseCase file.

**Validation criterion:**
> `.\gradlew :domain:compileKotlin :domain:compileTestKotlin :application:compileKotlin :application:compileTestKotlin` passes.

---

### Step 5 — Update stats + booklet + csv domain types

**Status**: ✅ Complete  
**Depends on**: Step 1  
**Blocking for**: Steps 7, 8

**Objective**: Make remaining 17 Command/Query data classes implement markers and UseCase interfaces extend handlers.

**UseCases to update:**

| UseCase | Data class | Marker | Result type |
|---|---|---|---|
| `GetMonthlyBookletStatsUseCase` | `GetMonthlyBookletStatsQuery` | `Query` | `MonthlyBookletStatsOutput` |
| `GetCategoryDistributionUseCase` | `GetCategoryDistributionQuery` | `Query` | `CategoryDistributionOutput` |
| `GetTrendStatsUseCase` | `GetTrendStatsQuery` | `Query` | `TrendStatsOutput` |
| `GetPrevisionalTransactionsUseCase` | `GetPrevisionalTransactionsQuery` | `Query` | `PrevisionalTransactionsOutput` |
| `GetDailyTrendStatsUseCase` | `GetDailyTrendStatsQuery` | `Query` | `DailyTrendStatsOutput` |
| `SaveBookletUseCase` | `SaveBookletCommand` | `Command` | `Booklet` |
| `FindAllRegisteredBookletsUseCase` | `FindAllRegisteredBookletsQuery` | `Query` | `List<Booklet>` |
| `DeleteBookletByIdUseCase` | `DeleteBookletByIdCommand` | `Command` | `Nothing` |
| `FindBookletByIdUseCase` | `FindBookletByIdQuery` | `Query` | `Booklet` |
| `LoadTransactionsForBookletForAMonthUseCase` | `LoadTransactionsForBookletForAMonthQuery` | `Query` | `BookletLoadingResult` |
| `LoadBalancesForBookletForAMonthUseCase` | `LoadBalancesForBookletForAMonthQuery` | `Query` | `BookletBalances` |
| `RegenerateDeletedPrevisionalTransactionsUseCase` | `RegenerateDeletedPrevisionalTransactionsCommand` | `Command` | `List<Transaction>` |
| `EditBookletUseCase` | `EditBookletCommand` | `Command` | `Booklet` |
| `FindByLabelAndUserIdUseCase` | `FindByLabelAndUserIdQuery` | `Query` | `Booklet` |
| `ValidateCsvFileUseCase` | `ValidateCsvFileQuery` | `Query` | `CsvValidationReport` |
| `ImportTransactionsFromCsvUseCase` | `ImportTransactionsFromCsvCommand` | `Command` | `CsvImportResult` |
| `ExportTransactionsToCsvUseCase` | `ExportTransactionsToCsvCommand` | `Command` | `String` |

**Actions:**
Same as Steps 3–4, for each UseCase file.

**Validation criterion:**
> `.\gradlew :domain:compileKotlin :domain:compileTestKotlin :application:compileKotlin :application:compileTestKotlin` passes.  
> At this point, `SpringCommandBus` and `SpringQueryBus` discover all 44 handlers at startup.

---

### Step 6 — Refactor AdminController + SessionController

**Status**: ✅ Complete  
**Depends on**: Steps 2, 3

**Objective**: Replace individual UseCase injections with bus dispatch in AdminController (1 UseCase → `QueryBus`) and SessionController (6 UseCases → `CommandBus` + `QueryBus`).

**Actions:**
1. `AdminController`: replace `GetUsersUseCase` injection with `QueryBus`, update `getCreatedUsers()` to use `queryBus.dispatch(GetUsersQuery(...))`
2. `SessionController`: replace `LoginUseCase`, `LogoutUseCase`, `RefreshSessionUseCase`, `RegisterUserUseCase` injections with `CommandBus`; replace `GetUserSettingsUseCase`, `UpdateUserSettingsUseCase` injections with respective bus (UpdateUserSettings → `CommandBus`, GetUserSettings → `QueryBus`). Keep `loginRateLimiter` and `secureCookie` injections unchanged
3. Remove all unused UseCase import lines
4. Update corresponding controller tests: replace individual UseCase mocks with `CommandBus` / `QueryBus` mocks

**Validation criterion:**
> `.\gradlew :application:compileKotlin :application:compileTestKotlin` passes. `.\gradlew :application:test` passes.

---

### Step 7 — Refactor TagController + StatsController

**Status**: ✅ Complete  
**Depends on**: Steps 2, 3, 5

**Objective**: Replace UseCase injections with bus dispatch in TagController (5 UseCases) and StatsController (6 UseCases, including `DefaultTagUseCase` shared with TagController).

**Actions:**
1. `TagController`: replace 5 UseCase injections with `CommandBus` + `QueryBus`; update each endpoint method (`addPersonalTag`, `getAllTags`, `deleteTag`, `defaultTag`, `editTag`) to use `commandBus.dispatch(...)` or `queryBus.dispatch(...)`
2. `StatsController`: replace 5 stats UseCases + `DefaultTagUseCase` with `QueryBus` (+ `CommandBus` if needed); update each endpoint method
3. Remove unused imports
4. Update controller tests

**Validation criterion:**
> `.\gradlew :application:test` passes.

---

### Step 8 — Refactor BookletController + CsvImportController

**Status**: ✅ Complete  
**Depends on**: Steps 2, 5

**Objective**: Replace UseCase injections with bus dispatch in BookletController (7 UseCases) and CsvImportController (3 UseCases).

**Actions:**
1. `BookletController`: replace 7 UseCase injections with `CommandBus` + `QueryBus`; update all endpoint methods
2. `CsvImportController`: replace 3 UseCase injections with `CommandBus` + `QueryBus`; keep `TransactionRepository` injection unchanged (out of scope — existing SPI leak)
3. Remove unused imports
4. Update controller tests

**Validation criterion:**
> `.\gradlew :application:test` passes.

---

### Step 9 — Refactor TransactionController

**Status**: ✅ Complete  
**Depends on**: Steps 2, 4, 5

**Objective**: Replace 14 UseCase injections with `CommandBus` + `QueryBus` — the biggest controller refactoring. Constructor goes from 14 parameters to 2.

**Actions:**
1. Replace 14 UseCase constructor parameters with `CommandBus` + `QueryBus`
2. Update all endpoint methods (`createTransaction`, `deleteByIds`, `getTransactionsByMonthAndYearAndBookletId`, `patchTransaction`, `findById`, `confirmPreviewTransaction`, `getAllRegularTransactions`, `createMonthlyTransaction`, etc.) to use `commandBus.dispatch(...)` or `queryBus.dispatch(...)`
3. Remove ~30 unused UseCase import lines
4. Update controller tests

**Validation criterion:**
> `.\gradlew :application:test` passes. Constructor has exactly 2 bus parameters (down from 14 UseCase parameters).

---

### Step 10 — Final validation: full test suite

**Status**: ✅ Complete  
**Depends on**: Steps 6, 7, 8, 9

**Actions:**
1. `.\gradlew :domain:test`
2. `.\gradlew :application:test`
3. `.\gradlew :infrastructure:test`

**Validation criterion:**
> All tests pass. No regression. Every controller uses the bus for dispatching. No controller directly injects a UseCase interface (except `DataLoader`, which is not a controller and keeps direct injection).

---

## Recommended Execution Order

```
Step 1 (Foundation types)
    ↓
Step 2 (Bus infrastructure)
    ↓
Step 3 ──────── Step 4 ──────── Step 5
(admin+user+    (transaction+    (stats+booklet+
 tag types)      RT types)        csv types)
    ↓                ↓                ↓
Step 6          Step 9 ◄──── Step 8 ◄─┘
(Admin+                      (Booklet+
 Session)                     CSV)
    ↓
Step 7
(Tag+Stats)
    ↓
Step 10 (Final validation)
```

*Domain type update steps (3, 4, 5) are independent and can be done in any order. Controller steps (6–9) can also be done in any order once their type-update dependencies are met.*

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
| `GenericTypeResolver` fails to resolve handler type for complex inheritance chains | Low | Log all registrations at startup; verify handler count matches expected (44) |
| Two handlers registered for the same Command/Query type (duplicate beans) | Very low | Each UseCase has exactly one `@DomainService` implementation; Spring enforces single-interface bindings |
| Type erasure causes incorrect dispatch at runtime | Low | Handler map uses `Class<*>` keys (runtime class of the command/query object); dispatch is type-safe via `Command<R>` propagation |
| `AddDefaultTagsUseCase` breaks pattern (no Command parameter) | None | Explicitly excluded — stays as-is, called only by `DataLoader` |
| Controller tests need bus mocking instead of UseCase mocking | Medium | Each test creates a mock `CommandBus`/`QueryBus`; dispatch behavior is configured per command/query type. Consider an `InMemoryCommandBus` test helper if mocking becomes repetitive |
| `CsvImportController` also injects `TransactionRepository` directly (SPI leak) | Low (existing issue) | Out of scope — not addressed in this refactoring |
| Some UseCases used across multiple controllers (`DefaultTagUseCase` in Tag + Stats, `LoadTransactionsForBookletForAMonthUseCase` in Booklet + Transaction) | None | The bus handles this seamlessly — a single handler is registered once and dispatched from anywhere |
