# API package — Domain ports (application-facing use-cases)

This document describes the responsibilities, conventions and architectural rules for the `fr.sacane.jmanager.domain.port.api` package. It is written for architects and developers who implement the domain's application-facing ports (use-case boundaries) in a Hexagonal (Ports & Adapters) architecture.

## Purpose

The `api` package contains the "inbound" ports of the domain — the interfaces that define how the application layer and external callers (controllers, CLI, scheduled jobs, tests) invoke domain use-cases. These interfaces describe business operations (use-cases) in terms of domain models and domain-level results. Implementations of these ports orchestrate business rules, validation and coordination of domain services.

Key points:
- This package hosts _interfaces_ (sealed interfaces in Kotlin) that express domain use-cases (what the system does), not how these use-cases are technically implemented.
- Implementations live in the domain module (sometimes alongside the interfaces) as domain services; they may coordinate repositories, other domain services and domain utilities.
- These interfaces are part of the domain's public contract with the application layer — keep them stable and focused on business intent.

## Responsibilities

An implementation of an API port must:
- Authenticate/authorize the request at the use-case boundary (via a `SessionManager` or equivalent) if required.
- Validate inputs against pure business rules (domain validation).
- Coordinate persistence through repository *interfaces* (see below) — the domain must depend on repository *abstractions* (SPI), never on adapters.
- Execute business rules and return domain-friendly results (e.g. `Result<T>`, domain error enums) — avoid leaking infra or framework exceptions.
- Keep side-effects (I/O, messaging) behind SPI adapters and unit-of-work boundaries.

## Relation with SPI (outbound ports)

In hexagonal architecture terms:
- `api` = inbound ports (what the application asks the domain to do).
- `spi` = outbound ports (what the domain asks external systems to do).

Strict separation rules:
1. Classes inside `api` must only reference domain models and SPI interfaces (repository contracts, token generators, hasher, etc.) — they must never directly reference concrete infrastructure implementations.
2. The `spi` package contains repository interfaces, gateways and other outbound contracts. Adapters (JPA, JDBC, HTTP clients, filesystems) live in the infrastructure module and implement SPI interfaces.
3. The dependency direction must always be: application/controllers -> api (ports) -> domain services (impl) -> spi (interfaces) -> infra adapters (implementations). No cyclic dependency.

Why this matters: this separation ensures testability (you can mock SPI interfaces when testing API implementations), portability (implementation of a repository can be replaced without changing business logic) and clear responsibility boundaries.

## Conventions and design guidance

- Interface signatures should use domain-level types (Aggregate roots, Value Objects, domain DTOs). Avoid passing raw infra types (ResultSet, JSON nodes, ORM entities).
- Return type: use a domain `Result<T>` or sealed error strategy that encodes well-known domain failures (notFound, invalid, forbidden, conflict, etc.). This centralises error handling and avoids throwing unchecked exceptions.
- Use `token: String` or a domain `SessionToken` object for authentication at the port level. The implementation authenticates via `SessionManager` SPI.
- Prefer `sealed interface` for ports when you provide multiple implementations (it is common to mark ports sealed for future extension or DSL safety).
- Keep methods coarse-grained and use-case oriented. Each method should represent a complete business action (e.g. `bookTransaction`, `addTag`, `getMonthlyAccountStats`).
- Document the contract in KDoc at interface level: describe purpose, parameters, outputs, failure modes and preconditions.

## How to use repositories (SPI) from API implementations

- Inject repository interfaces (from `domain.port.spi.repository`) into domain service implementations that live next to the API ports. These repositories are abstractions only — they do not contain infra code.
- Repositories must expose domain-friendly operations (e.g. `findByIdWithTransactions`, `save`, `deleteById`) and return domain types or `null` / `Result` for not-found cases.
- Do not perform transaction management inside repository implementations unless they provide explicit unit-of-work helpers. Prefer explicit `UnitOfWorkTransactionProvider` SPI to wrap multiple repository operations in a single transaction scope inside the API implementation.

Example pattern (pseudocode):

- Port (this package):
  fun save(token: String, booklet: Booklet): Result<Booklet>

- Implementation (domain service):
  class BookletFeatureImpl(private val bookletRepo: BookletRepository, private val session: SessionManager): BookletFeature {
      override fun save(token, booklet) = session.authenticate(token) { userId ->
          if (bookletRepo.existsByLabel(userId, booklet.label)) return failure(CONFLICT)
          val saved = bookletRepo.save(userId, booklet) ?: return invalid("infrastructure")
          success(saved)
      }
  }

- Repository (SPI, `domain.port.spi.repository`):
  interface BookletRepository {
      fun findAccountByIdWithTransactions(id: UUID): Booklet?
      fun save(userId: UserId, booklet: Booklet): Booklet?
      fun existsByLabel(userId: UserId, label: String): Boolean
  }

- Adapter (infra): implements `BookletRepository` using JPA, JDBC, etc.; lives in the `infra` module.

## Error handling and results

- Ports should not throw infra exceptions. Map infra errors to domain `ResultState` types.
- Use clear, well-documented failure codes (for example: `NOT_FOUND`, `INVALID`, `FORBIDDEN`, `INFRASTRUCTURE_ERROR`, `TAG_LABEL_ALREADY_TAKEN`) and provide human-readable messages only at the boundary layer (the messages should be localised or formatted by the presentation layer if needed).

## Transactional boundaries

- When a use-case requires atomic changes across multiple repositories, wrap the coordination in a unit-of-work transaction (use `UnitOfWorkTransactionProvider` SPI). Keep transactions scoped to a single application use-case; do not span user-interactive long periods.

## Testing guidance

- Unit tests for API implementations should mock the SPI interfaces (`SessionManager`, repositories, unit-of-work), assert business logic and verify interactions.
- Integration tests should use real adapters (in-memory DB, test-containers) to validate cross-module contracts and schema mappings.
- Cover these scenarios explicitly: authentication failure, not-found, concurrent modification (label already exists), invalid input, successful path.

## Documentation and expectations for contributors

- Document each port method using KDoc directly above the sealed interface method: intention, parameters, return type, and known failure modes.
- Keep the API package clean of framework annotations and infrastructure-specific code.
- If a new outbound capability is needed (e.g. remote notification), add an SPI interface under `domain.port.spi` and call it from your API implementation; implement the adapter in `infra`.

## Summary (architectural mantra)

The `api` package embodies the language the application uses to talk to the domain. It defines intent (use-cases) and contracts, not implementation. The domain implementation (services) uses repository abstractions (SPI) to express dependencies outward. Keep boundaries strict, document contracts precisely, and prefer small, well-named methods that represent complete business use-cases.

