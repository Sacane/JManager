# SPI package — Domain outbound ports (infrastructure contracts)

This README explains the responsibilities, conventions and architectural rationale for the `fr.sacane.jmanager.domain.port.output` package. It is written for architects and engineers implementing adapters (infrastructure) or contributing to domain ports.

## Purpose

The `spi` package contains the *outbound* ports of the domain: the interfaces the domain uses to interact with external systems (persistence, cryptography, token generation, transaction management, session store, etc.). These are infrastructure contracts — abstractions that isolate domain logic from concrete adapters.

Put simply:
- `api` = inbound ports (what external callers ask the domain to do).
- `spi` = outbound ports (what the domain asks external systems to do).

The domain implements use-cases by depending on SPI interfaces; adapters implement those interfaces in the `infra` module.

## Responsibilities of SPI interfaces

Each SPI interface must:
- Express an infrastructure capability in domain terms (domain models and value objects).
- Be small, focused and stable — changing an SPI is a backward-incompatible change for adapters.
- Return domain-friendly types (domain models, Optionals / nullable, `Result` when appropriate) rather than infra types (e.g., JDBC ResultSet).
- Avoid prescribing transport or technology details (no SQL, HTTP, or framework annotations in SPI).

Examples in this codebase:
- `BookletRepository` — persistence contract for Booklet aggregates.
- `TransactionRepository` — contract for transaction persistence and queries.
- `TokenGenerator` — encapsulates token signing/parsing.
- `Hasher` — cryptographic hashing abstraction for passwords.
- `SessionManager` — session store and authentication helper.

## Separation of concerns: SPI vs API

Strict rules to keep the architecture clean:
- Domain services (API implementations) call SPI interfaces. They must not depend on adapter implementations.
- Adapters implement SPI interfaces in the `infra` module and depend on external frameworks (JPA, JDBC, Redis, JWT libraries, etc.).
- No SPI interface should import or reference adapter-specific types (for example, Spring/Hibernate types).

Dependency direction:
controllers -> api (ports) -> domain services (impl) -> spi (interfaces) -> infra (adapters)

This ensures testability (mock SPI in unit tests) and portability (swap adapters without changing domain logic).

## Conventions and design guidance for SPI definitions

- Use domain value objects and aggregates for signatures (e.g. `UserId`, `Booklet`, `Transaction`).
- Prefer nullable return types or domain `Result` to indicate not-found vs error cases. Keep the semantic explicit in the method name and KDoc.
- Keep method names intent-driven: `findAccountByIdWithTransactions`, `saveRegularTransaction`, `deleteAllTransactionsById`.
- When an operation may fail due to infra issues, return `null` or an optional and let the domain translate it to `ResultState.INFRASTRUCTURE_ERROR`.
- For multi-repository atomic operations, provide or use a `UnitOfWorkTransactionProvider` SPI — do not mix transaction management into domain logic or adapters implicitly.

## Transaction boundaries

- SPI `UnitOfWorkTransactionProvider` exposes `executeInTransaction(...)` to run a block inside a transactional boundary. Implementations must start/commit/rollback appropriately.
- Domain use-cases should wrap necessary repository calls in a unit-of-work when they must be atomic.

## Error handling and mapping

- SPI adapters may throw infrastructure exceptions (SQL exception, network error). Adapters should either:
  - translate errors into domain-friendly signals (return `null` / `false` / specific error objects), or
  - let exceptions bubble and rely on the domain boundary to map them to domain `ResultState`.
- Prefer explicit mapping in adapters for predictable domain error mapping.

## Implementing adapters

When implementing a SPI adapter:
- Keep all infra-specific code inside the adapter module (e.g. `infra`), not in `domain`.
- Convert infra models to domain models at the adapter boundary.
- Keep SQL/JPA queries, transaction demarcation and persistence code inside the adapter.
- Respect the contract: do not change method semantics or return types declared in the SPI.

Example adapter responsibilities for `BookletRepository`:
- Map JPA entities to `Booklet` domain objects.
- Implement `findAccountByIdWithTransactions` to eagerly fetch related transaction rows and build domain `Transaction` objects.
- Implement `upsert` semantics consistently (e.g. merge or save depending on the ORM).

## Testing guidance

- Unit tests for domain use-cases should mock SPI interfaces to assert business rules and interactions.
- Adapter tests should run against real infra (embedded DB, test containers) to verify mapping and queries.
- Integration tests should validate the end-to-end behaviour with adapters wired in an application context.

## Documentation and maintenance

- Document each SPI interface with precise KDoc describing the method semantics, expected nullability, failure modes and transactional expectations.
- Treat SPI changes as breaking: prefer adding new methods over changing existing signatures.

## Security considerations

- Cryptographic operations (e.g. `Hasher`) must be implemented using vetted libraries and safe defaults (salted hashing, appropriate strength).
- Token generation and verification must protect secret keys and follow expiration/rotation policies.
- Session stores must avoid leaking secrets in logs.

## Summary

The `spi` package defines the outward-facing contractual surface of the domain. It is intentionally minimal and expressive in domain terms. Adapters implement these interfaces in a separate `infra` module, keeping the domain pure, testable and infrastructure-agnostic.

If you want, I can:
- generate ADRs for token and hashing choices;
- create KDoc templates for new SPI interfaces;
- run a completeness check to ensure every SPI interface has KDoc and every method has param/return docs.

