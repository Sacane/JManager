---
description: 'Backend development guidelines for the JManager project (application, domain, infrastructure)'
applyTo: '{application,domain,infrastructure}/**'
---

# Backend Development Guidelines

Guidelines to follow whenever working on any of the three backend layers: `domain`, `infrastructure`, or `application`.

---

## 1. Architecture: Hexagonal (Ports & Adapters)

### 1.1 Closed Rule

- The **domain** layer must never depend on infrastructure or application concerns.
- The **infrastructure** layer implements ports defined in the domain. It never exposes framework types to the domain.
- The **application** layer wires domain and infrastructure together. It does not contain business logic.

### 1.2 Port Contracts

- Every interaction between domain and infrastructure passes through a **port** (interface defined in `domain`).
- An adapter in `infrastructure` implements the port. The adapter is the only place where technical details (JPA, HTTP clients, messaging) appear.
- Never bypass a port by injecting an adapter directly into domain code.

### 1.3 DTO and Entity Boundaries

- **DTOs** live exclusively in `application`. They carry API-level annotations (`@JsonProperty`, validation annotations, etc.) and serve as the contract between the REST layer and the outside world.
- **Entities** live exclusively in `infrastructure`. They are technical objects mapped to the persistence framework (`@Entity`, `@Table`, `@Column`, etc.) and must never leak into domain or application code.
- **Domain objects** carry no framework annotation whatsoever.
- Mapping between domain objects and DTOs happens in the application layer (controllers, mappers).
- Mapping between domain objects and entities happens in the infrastructure layer (adapters).

---

> **Development workflow (TDD, SOLID, duplication, design patterns)** is defined in the cross-cutting file  
> [`development-workflow.md`](development-workflow.md) and applies to all layers including backend.

---

## 3. Use Case Architecture: Command/Query Bus

### 3.1 Consolidated UseCase File Structure

Every use case lives in `domain/port/input/{category}/` as a **single file** containing three elements in order:

1. The **input data class** (Command or Query)
2. The **port interface** extending `CommandHandler` or `QueryHandler`
3. The **`@DomainService` implementation**

```kotlin
// LoginUseCase.kt

data class LoginCommand(val pseudonym: String, val userPassword: String) : Command<UserToken>

@Port(Side.APPLICATION)
interface LoginUseCase : CommandHandler<LoginCommand, UserToken> {
    override val commandClass get() = LoginCommand::class
}

@DomainService
class LoginService(
    private val session: SessionManager,
    private val userRepository: UserRepository
) : LoginUseCase {
    override fun handle(command: LoginCommand): Result<UserToken> { ... }
}
```

Shared helpers used by multiple services in the same category are extracted to a dedicated `*DomainHelper.kt` file (e.g. `BookletDomainHelper.kt`, `StatsDomainHelper.kt`).

### 3.2 Command vs Query Classification

- **Command** — any operation that **mutates state**: create, update, delete, link, import, book, confirm. Input type **ends with `Command`**, interface extends `CommandHandler<C, R>`.
- **Query** — any operation that **only reads state** without side effects: find, get, list, calculate, validate. Input type **ends with `Query`**, interface extends `QueryHandler<Q, R>`.
- When in doubt: if it writes to the database or triggers any external side effect → **Command**.

### 3.3 Bus Dispatch in Controllers

Controllers **never** inject individual use cases directly. They inject `CommandBus` and `QueryBus` from `application/bus/` and dispatch by input type:

```kotlin
class BookletController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) {
    fun save(...) = commandBus.dispatch(SaveBookletCommand(...))
    fun findById(...) = queryBus.dispatch(FindBookletByIdQuery(...))
}
```

**Exception**: use cases with no input parameter (e.g. `AddDefaultTagsUseCase`) or called exclusively from non-controller components (e.g. `DataLoader`) keep their direct injection — they are not routed through the bus.

### 3.4 Foundation Types

The base interfaces live in `domain/port/input/`:

- `Command<R>` — marker interface for command input objects
- `Query<R>` — marker interface for query input objects
- `CommandHandler<C : Command<R>, R>` — declares `val commandClass: KClass<C>` and `fun handle(command: C): Result<R>`
- `QueryHandler<Q : Query<R>, R>` — declares `val queryClass: KClass<Q>` and `fun handle(query: Q): Result<R>`

The `commandClass`/`queryClass` property (not `GenericTypeResolver`) is the registration key used by the bus — this is intentional: Kotlin erases generic type information for interfaces parameterised with `Nothing`, making reflection-based resolution unreliable.

---

---

## 2. Domain Modelling and Invariants

### 6.1 Explicit Business Model

- Domain code expresses the business language of `FEATURES.md`, not a technical vocabulary.
- Business behaviour lives in domain objects, not in generic utility services.
- Every important business rule must be visible in a method named by intent.

### 6.2 Entities and Value Objects

- Use an entity when identity and lifecycle are central.
- Use a value object to represent an immutable concept, valid by construction.
- Avoid primitive obsession:
  - prefer dedicated business types over raw `String`/`Int` for identifiers and critical quantities
  - encapsulate validation rules in these types

### 6.3 Business Invariants: Fail Fast

- A domain object must never exist in an invalid state.
- Validate invariants on creation and at each state transition.
- Immediately refuse an invalid transition via an explicit business error.
- Never delegate a fundamental business rule to infrastructure.

### 6.4 State Transitions and Domain API

- Model transitions explicitly.
- Each transition:
  - verifies its preconditions
  - applies only the authorised changes
  - returns a result usable by the application layer
- Avoid public setters that allow arbitrary state modifications.

### 6.5 Time, Calculations and Determinism

- Use `java.time` (`Instant`, `LocalDate`, `LocalTime`, `Duration`) for all temporal logic.
- Inject a `Clock` into use cases or domain services that depend on the current time.
- Keep calculations deterministic and testable: no direct access to system time within business rules.

---

## 7. Error Handling and Result Strategy

### 7.1 General Principles

- A business error is expected domain information, not a technical surprise.
- A technical exception is reserved for non-recoverable cases or infrastructure failures.
- Do not use generic `Exception` or `RuntimeException` to carry business rules.
- Error messages must be actionable and aligned with the business vocabulary.

### 7.2 Business Errors in Domain

- Model business errors with explicit types (sealed interface/class recommended).
- Each critical invariant must have a dedicated error type.
- Avoid opaque numeric codes in the domain.
- The domain does not depend on transport details (HTTP status, JSON format, etc.).

### 7.3 Use Case Results in Application

- Use cases return explicit results (success or business failure), not an ambiguous boolean.
- Prefer a structured return type:
  - `sealed class UseCaseResult`
  - `kotlin.Result` only for simple cases without loss of business semantics
- A use case method must make explicit what the caller can handle as both the nominal case and failure cases.

### 7.4 Technical Boundaries in Infrastructure

- Capture technical errors as close to their source as possible (DB, HTTP client, messaging).
- Map technical errors to application/business errors understandable by the upper layer.
- Never let an external library exception bubble up into domain.
- Retries, timeouts, and circuit-breakers remain in infrastructure, not in domain.

### 7.5 Mapping and Observability

- Error-to-external-response mapping happens at the application layer (or input adapter), never in domain.
- Log technical errors with context without exposing sensitive data.
- For expected business errors, log at the appropriate level (often `info`/`warn`), not systematically as errors.
- Keep clear user-facing messages and detailed technical messages separate.

---

## 8. Layer-Specific Reminders

### 8.1 Domain

- Pure Kotlin, no framework dependency.
- Business invariants validated on construction and transitions.
- Errors modelled as explicit sealed types.

### 8.2 Infrastructure

- Implements domain ports.
- Technical exceptions caught and mapped to domain error types at the adapter boundary.
- No business logic — only technical translation and delegation.

### 8.3 Application

- Orchestrates use cases by wiring ports to adapters.
- Maps API DTOs to/from domain objects.
- Handles HTTP concerns (status codes, error responses) — never leaks them into domain.
