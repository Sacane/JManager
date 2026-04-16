# Kotlin Coding Guidelines

Guidelines specific to the JManager project.

## 1. Scope, Goals and Architecture

These guidelines define the Kotlin coding rules to apply in this repository to ensure:
- consistency across modules
- clarity of business code
- system evolvability
- high testability

The project follows a hexagonal architecture with three main modules:
- domain: business core, pure Kotlin, no framework dependency
- infrastructure: technical adapters (persistence, integration, external consumers), depends on domain
- application: entry point and backend composition, uses the domain via port interfaces, depends on domain and infrastructure

Structural constraints:
- Target JDK: 21
- Build: Gradle Kotlin DSL
- Tests: Kotlin test + execution on JUnit Platform

Guiding principles:
- Business logic lives in domain and never depends on any technical choice.
- Technical details remain confined to infrastructure.
- application orchestrates and assembles components without containing complex business logic.
- Code favours readability, simplicity, and explicit invariants over magic.

## 2. Kotlin Style and Naming Conventions

### 2.1 General Style Rules

- Indentation: 4 spaces, no tabs.
- Encoding: UTF-8.
- One primary type (class, interface, object) per file.
- Prefer `val` over `var`. Any mutability must be justified.
- Short functions with a single clear responsibility.
- Avoid "magic" boolean parameters. Prefer an explicit type (enum, value object).
- Prefer Kotlin null-safety features (`?.`, `?:`, `requireNotNull`) over scattered null checks.
- Raise explicit business errors (see errors section) rather than generic `IllegalStateException` everywhere.

### 2.2 Kotlin Naming Conventions

- Packages: lowercase, no underscores, aligned with the business context.
- Classes, interfaces, objects, enums: PascalCase.
- Functions and properties: camelCase.
- Compile-time constants: UPPER_SNAKE_CASE.
- Explicit names, no ambiguous abbreviations.

### 2.3 Naming per Layer

- domain:
	- Entities and value objects named after business concepts.
	- Business services named by intent.
	- No technical suffix tied to a framework.

- application:
	- Use cases suffixed with `UseCase`.
	- Incoming ports named by business role.
	- Command/Query objects named with intent.
	- Explicit technical DTOs that do not leak into domain.

- infrastructure:
	- Technical adapters suffixed by their nature (`JpaXxxRepository`, `HttpXxxClient`).
	- Explicit technical DTOs that do not leak into domain.

### 2.4 Readability and Intent

- A name must explain "why" and "what", not "how".
- Avoid vague names: `data`, `info`, `manager`, `helper`, `util`.
- Prefer explicit APIs over over-parameterised generic functions.
- When two formulations are possible, choose the one closest to the vocabulary of `FEATURES.md`.

## 3. Domain Modelling and Invariants

### 3.1 Explicit Business Model

- Domain code expresses the business language of `FEATURES.md`, not a technical vocabulary.
- Business behaviour lives in domain objects, not in generic utility services.
- Every important business rule must be visible in a method named by intent.

### 3.2 Entities and Value Objects

- Use an entity when identity and lifecycle are central.
- Use a value object to represent an immutable concept, valid by construction.
- Avoid primitive obsession:
	- prefer dedicated business types over raw `String`/`Int` for identifiers and critical quantities
	- encapsulate validation rules in these types

### 3.3 Business Invariants: Fail Fast

- A domain object must never exist in an invalid state.
- Validate invariants on creation and at each state transition.
- Immediately refuse an invalid transition via an explicit business error.
- Never delegate a fundamental business rule to infrastructure.

### 3.4 State Transitions and Domain API

- Model transitions explicitly.
- Each transition:
	- verifies its preconditions
	- applies only the authorised changes
	- returns a result usable by the application layer
- Avoid public setters that allow arbitrary state modifications.

### 3.5 Time, Calculations and Determinism

- Use `java.time` (`Instant`, `LocalDate`, `LocalTime`, `Duration`) for all temporal logic.
- Inject a `Clock` into use cases or domain services that depend on the current time.
- Keep calculations deterministic and testable: no direct access to system time within business rules.

## 4. Error Handling and Result Strategy

### 4.1 General Principles

- A business error is expected domain information, not a technical surprise.
- A technical exception is reserved for non-recoverable cases or infrastructure failures.
- Do not use generic `Exception` or `RuntimeException` to carry business rules.
- Error messages must be actionable and aligned with the business vocabulary.

### 4.2 Business Errors in domain

- Model business errors with explicit types (sealed interface/class recommended).
- Each critical invariant must have a dedicated error type.
- Avoid opaque numeric codes in the domain.
- The domain does not depend on transport details (HTTP status, JSON format, etc.).

### 4.3 Use Case Results in application

- Use cases return explicit results (success or business failure), not an ambiguous boolean.
- Prefer a structured return type:
	- `sealed class UseCaseResult`
	- `kotlin.Result` only for simple cases without loss of business semantics
- A use case method must make explicit what the caller can handle as both the nominal case and failure cases.

### 4.4 Technical Boundaries in infrastructure

- Capture technical errors as close to their source as possible (DB, HTTP client, messaging).
- Map technical errors to application/business errors understandable by the upper layer.
- Never let an external library exception bubble up into domain.
- Retries, timeouts, and circuit-breakers remain in infrastructure, not in domain.

### 4.5 Mapping and Observability

- Error-to-external-response mapping happens at the application layer (or input adapter), never in domain.
- Log technical errors with context without exposing sensitive data.
- For expected business errors, log at the appropriate level (often `info`/`warn`), not systematically as errors.
- Keep clear user-facing messages and detailed technical messages separate.
