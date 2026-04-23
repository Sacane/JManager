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

## 2. Incremental Development Workflow

Every change — feature, fix, or refactoring — must follow this strict sequence:

### Step 1: Tests First

- Write or update failing tests **before** touching production code.
- Start from the layer closest to the business rule being changed (usually `domain`).
- The test must clearly express the expected behaviour, not the implementation.

### Step 2: Make It Work

- Implement the minimum code required to make the tests pass.
- At this stage, favour a **working solution** over a clean one.
- Quick and direct code is acceptable here — the goal is a green test suite, not perfection.

### Step 3: Refactoring Analysis (Mandatory)

- Once all tests are green, **always** perform a refactoring analysis before considering the task done.
- Review the new code and its surrounding context for:
  - duplication that can be extracted
  - naming that can be improved
  - responsibilities that can be redistributed
  - patterns that can be applied
- This step is **never optional**. Skipping it is a defect in the process.

---

## 3. Code Duplication: Zero Tolerance

Duplicated code is never acceptable. After every implementation, actively search for duplication at three levels:

### 3.1 Method Extraction

- If the same logic appears in two or more places, extract it into a dedicated method.
- The method name must describe **intent**, not mechanics.

### 3.2 Class Extraction

- If several methods share the same data or collaborate on the same concept, extract them into a dedicated class (service, value object, helper object).
- Respect the layer where the class belongs: shared business logic goes in `domain`, shared technical logic goes in `infrastructure`.

### 3.3 Pattern Application

- If duplication reveals a recurring structural problem, consider a design pattern (Strategy, Template Method, Factory, etc.).
- Before applying a pattern, **ask the user for confirmation** — explain the duplication observed, the pattern proposed, and the expected benefit.

---

## 4. SOLID Principles

All backend code must respect the SOLID principles:

### 4.1 Single Responsibility (SRP)

- A class has one reason to change.
- A use case handles one business operation. If a use case grows beyond a single flow, split it.

### 4.2 Open/Closed (OCP)

- Extend behaviour through new implementations (new adapters, new strategies), not by modifying existing classes.
- Ports and sealed hierarchies are the primary extension points.

### 4.3 Liskov Substitution (LSP)

- Any implementation of a port must be fully substitutable for another without breaking the domain contract.
- Fakes used in tests must respect the same invariants as real adapters.

### 4.4 Interface Segregation (ISP)

- Ports must be small and focused. A port that serves multiple unrelated use cases must be split.
- Prefer several specific interfaces over one large general-purpose interface.

### 4.5 Dependency Inversion (DIP)

- Domain depends on abstractions (ports), never on concrete adapters.
- Application wires concrete adapters to ports via dependency injection.
- No `import` from `infrastructure` must appear in `domain` source files.

---

## 5. Design Pattern Proposals

- When the agent identifies an opportunity to apply a design pattern during implementation or refactoring, it **must not apply it silently**.
- The agent must:
  1. Describe the problem or smell observed (duplication, branching, coupling, etc.).
  2. Name the pattern proposed and explain why it fits.
  3. Wait for explicit user confirmation before proceeding.
- Once confirmed, implement the pattern fully — including updating tests and all affected references.

---

## 6. Domain Modelling and Invariants

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
