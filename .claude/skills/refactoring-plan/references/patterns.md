# Refactoring Patterns — Reference

Read this file during Phase 2 (strategy selection) to identify the pattern best suited to the real state of the project.

---

## How to Choose

Apply these questions in order to the analysed code:

1. **Are the layers clearly separated?** (domain / application / infrastructure)
   - No → start with `Package Restructuring` before any other pattern
   - Yes → continue

2. **Does the domain depend on Spring, JPA, or other frameworks?** (`@Entity` in the domain, `@Repository` injected directly into a domain service…)
   - Yes → `Hexagonal Architecture` takes priority

3. **Do interfaces or classes group operations without strong cohesion?**
   - Yes → `Use Case Split`

4. **Do read and write operations share complex models, or do controllers inject many use cases?**
   - Many injected use cases per controller (3+) and Command/Query objects are in place → `Command/Query Bus`
   - Truly independent read/write models with separate stores → full `CQRS`
   - Simple or moderate complexity → `Use Case Split` is sufficient

5. **Does a class do too many things without being an interface?**
   - Yes → `Extract Service`

Multiple patterns can apply in sequence. In that case, order them in the plan: package restructuring → hexagonal → use case split → command/query bus if needed.

---

## Use Case Split

**Signal**: An interface or service class contains multiple methods covering distinct operations (create, read, update, delete, calculate…).

**What we do**:
- Each method becomes a dedicated interface with a single method (`handle`)
- Each interface has a dedicated implementation (`*Service`)
- Callers inject only the use case they need

**Target structure (Kotlin + Spring Boot) — consolidated 1-file per use case**:
```
domain/port/input/{category}/
├── CreateXUseCase.kt          // data class + interface + @DomainService — all in one file
├── GetXByIdUseCase.kt         // data class + interface + @DomainService — all in one file
└── XDomainHelper.kt           // shared helpers used by multiple services (kept separate)
```

Each `*UseCase.kt` file follows this layout:
```kotlin
// 1. Input data class (Command or Query)
data class CreateXCommand(val field1: String, val field2: Int) : Command<X>

// 2. Port interface
@Port(Side.APPLICATION)
interface CreateXUseCase : CommandHandler<CreateXCommand, X> {
    override val commandClass get() = CreateXCommand::class
}

// 3. Domain service implementation
@DomainService
class CreateXService(
    private val session: SessionManager,
    private val repository: XRepository
) : CreateXUseCase {
    override fun handle(command: CreateXCommand): Result<X> { ... }
}
```

**Typical step order**:
1. Create the use case interfaces in `domain/port/input/`
2. Create the Command/Query objects
3. Create the service implementations with logic extracted from the old `*FeatureImpl`
4. Update callers (controllers, other services)
5. Remove the old interface and implementation
6. Consolidate: merge Command/Query + interface + service into the single `*UseCase.kt` file

**Risks**: Spring injection must be updated in all callers; shared private helpers in the old impl may need extraction.

> **Important (Kotlin)**: If two use case interfaces declare default values for the same parameter and are implemented by the same class, a compile error occurs ("More than one function overridden declares a default value"). Using Command/Query objects solves this — defaults are placed on the data class, not the interface.

---

## Hexagonal Architecture (Ports & Adapters)

**Signal**: JPA entities (`@Entity`) or Spring repositories (`JpaRepository`) are directly imported into domain or application layer classes. The domain is not testable without Spring.

**What we do**:
- Output ports (e.g. `BudgetRepository`) become interfaces in `domain/port/output/`
- JPA implementations go into `infrastructure/persistence/`
- The domain contains no framework annotations

**Target structure**:
```
domain/
├── model/                         // pure domain entities (no @Entity here)
└── port/
    ├── input/                     // use cases (interfaces)
    └── output/                    // output ports (e.g. BudgetRepository)

infrastructure/
├── persistence/
│   ├── BudgetJpaRepository.kt     // extends JpaRepository
│   └── BudgetRepositoryAdapter.kt // implements domain BudgetRepository
└── web/
    └── BudgetController.kt        // @RestController, injects use cases
```

**Typical step order**:
1. Create output port interfaces in `domain/port/output/`
2. Create infrastructure adapters implementing those interfaces
3. Update services to depend on interfaces, not JPA implementations
4. Remove JPA annotations from the domain model (create separate JPA entities if needed)
5. Verify no Spring/JPA import remains in `domain/`

**Risks**: if the domain model is also the JPA entity, the separation is costly — evaluate relevance based on project size.

---

## Command/Query Bus (Mediator)

**Signal**: Controllers inject a large number of individual use cases (3+), leading to bloated constructors and tight coupling between the application layer and every domain operation. The use case split + Command/Query objects are already in place.

**This is NOT full CQRS** (no separate read/write stores, no projections). It is a **Mediator/Bus** pattern layered on top of Use Case Split: all dispatch goes through a single `CommandBus` or `QueryBus`, which resolves the correct handler by the runtime type of the input object.

**Classification rule — Command vs Query**:
- **Command** = any operation that **mutates state** (create, update, delete, link, import…). Input type ends with `Command`. Handler extends `CommandHandler<C, R>`.
- **Query** = any operation that **only reads state** without side effects (find, get, list, calculate, validate…). Input type ends with `Query`. Handler extends `QueryHandler<Q, R>`.
- When in doubt: if it writes anything to the database or triggers any external side effect → Command.

**Domain foundation types** (`domain/port/input/`):
```kotlin
// CommandHandling.kt
interface Command<R>
interface CommandHandler<C : Command<R>, R> {
    val commandClass: KClass<C>
    fun handle(command: C): Result<R>
}

// QueryHandling.kt
interface Query<R>
interface QueryHandler<Q : Query<R>, R> {
    val queryClass: KClass<Q>
    fun handle(query: Q): Result<R>
}
```

**UseCase interface** — extends the appropriate handler (one per use case file):
```kotlin
data class LoginCommand(val pseudonym: String, val userPassword: String) : Command<UserToken>

@Port(Side.APPLICATION)
interface LoginUseCase : CommandHandler<LoginCommand, UserToken> {
    override val commandClass get() = LoginCommand::class
    // handle() is inherited — no need to redeclare it
}
```

**Bus implementations** (`application/bus/`):
```kotlin
interface CommandBus {
    fun <R> dispatch(command: Command<R>): Result<R>
}

@Component
class SpringCommandBus(handlers: List<CommandHandler<*, *>>) : CommandBus {
    private val handlerMap: Map<Class<*>, CommandHandler<*, *>> =
        handlers.associateBy { it.commandClass.java as Class<*> }

    @Suppress("UNCHECKED_CAST")
    override fun <R> dispatch(command: Command<R>): Result<R> {
        val handler = handlerMap[command::class.java] as? CommandHandler<Command<R>, R>
            ?: throw IllegalArgumentException("No handler registered for ${command::class.simpleName}")
        return handler.handle(command)
    }
}
// QueryBus is symmetric — same structure with queryClass.
```

**Controller convention** — inject only buses, never individual use cases:
```kotlin
// Before (N UseCase injections)
class TransactionController(
    private val bookTransactionUseCase: BookTransactionUseCase,
    private val deleteTransactionsByIdsUseCase: DeleteTransactionsByIdsUseCase,
    // ... 12 more ...
)

// After (2 bus injections)
class TransactionController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) {
    fun book(...) = commandBus.dispatch(BookTransactionCommand(...))
    fun findById(...) = queryBus.dispatch(FindTransactionByIdQuery(...))
}
```

**Exception**: use cases with no input parameter (e.g. `AddDefaultTagsUseCase`) or called exclusively outside controllers (e.g. `DataLoader`) keep their direct injection — they are not routed through the bus.

**When NOT to apply**: if the number of injected use cases per controller is small (≤ 2–3) and direct injection is manageable — `Use Case Split` is sufficient.

**Typical step order**:
1. Create `Command<R>`, `Query<R>`, `CommandHandler`, `QueryHandler` base interfaces in domain
2. Create `CommandBus` / `QueryBus` Spring implementations in application (`application/bus/`)
3. For each use case: add `: Command<R>` / `: Query<R>` to the input data class; extend `CommandHandler` / `QueryHandler` in the interface; add `override val commandClass/queryClass` default getter
4. Refactor controllers one by one: replace N use case injections with 2 bus injections; replace `useCase.handle(...)` with `bus.dispatch(...)`
5. Update controller tests: mock `CommandBus`/`QueryBus` instead of individual use cases
6. Full test suite validation

**Design note — KClass property instead of GenericTypeResolver**: Kotlin omits the JVM Signature attribute for interfaces parameterised with `Nothing`, making `GenericTypeResolver` unreliable for handler registration. The chosen solution is an explicit `val commandClass: KClass<C>` / `val queryClass: KClass<Q>` overridden in each UseCase interface with a default getter. This is fully static, type-safe, and requires no changes on `@DomainService` implementations.

---

## Extract Service

**Signal**: A class (not an interface) has too many responsibilities — unrelated methods, too many injected dependencies, 500+ line file.

**What we do**:
- Identify cohesive groups of methods
- Extract each group into a dedicated service class
- The original class can delegate or be removed

**Typical step order**:
1. Map method groups by responsibility
2. Create new services with the corresponding methods
3. Migrate logic method by method
4. Update callers
5. Delete or slim down the original class

---

## Package Restructuring

**Signal** : Les packages sont organisés par type technique (`controller/`, `service/`, `repository/`) plutôt que par feature ou couche architecturale. Difficile de trouver tout ce qui concerne une feature.

**Ce qu'on fait** :
- Réorganiser par couche architecturale (`domain/`, `application/`, `infrastructure/`) ou par feature (`budget/`, `transaction/`)
- Mettre à jour les imports

**Important** : faire ce refactoring **en premier**, avant les autres — déplacer des fichiers est plus simple quand la logique n'a pas encore changé.

**Ordre des étapes type** :
1. Définir la nouvelle structure cible (valider avec le dev)
2. Créer les nouveaux packages
3. Déplacer les fichiers package par package (pas classe par classe)
4. Corriger les imports
5. Vérifier que le projet compile et les tests passent
