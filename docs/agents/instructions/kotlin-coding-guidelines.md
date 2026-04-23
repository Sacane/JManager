# Kotlin Coding Guidelines

Guidelines specific to the JManager project.

## 1. Scope and Constraints

These guidelines define the **Kotlin language** rules to apply in this repository.
For architecture, layering, domain modelling, and error strategy, see `backend.instructions.md`.

Structural constraints:
- Target JDK: 21
- Build: Gradle Kotlin DSL
- Tests: Kotlin test + execution on JUnit Platform
- Encoding: UTF-8

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
	- DTOs named explicitly (e.g. `XxxDTO`, `XxxRequest`, `XxxResponse`).

- infrastructure:
	- Technical adapters suffixed by their nature (`JpaXxxRepository`, `HttpXxxClient`).
	- Persistence entities named explicitly (e.g. `XxxEntity`).

### 2.4 Readability and Intent

- A name must explain "why" and "what", not "how".
- Avoid vague names: `data`, `info`, `manager`, `helper`, `util`.
- Prefer explicit APIs over over-parameterised generic functions.
- When two formulations are possible, choose the one closest to the vocabulary of `FEATURES.md`.

For domain modelling, invariants, and error handling strategy, see `backend.instructions.md`.
