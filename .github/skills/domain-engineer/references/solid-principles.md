# SOLID Principles — Domain Engineering Reference

Read this file during every design and review phase of domain development.
Apply these principles **strictly inside the domain layer** — infrastructure and framework concerns must never bleed in.

---

## S — Single Responsibility Principle

> A class should have **one, and only one, reason to change**.

### Domain signals

- A domain service that loads an entity, validates a business rule, calculates a value, AND persists the result → **violation**.
- An entity that both represents state and orchestrates complex workflows → **violation**.
- A use case that mixes multiple distinct business concerns in a single `handle()` → **violation**.

### Fixes

| Smell | Fix |
|---|---|
| Service with mixed concerns | Extract each concern into a focused collaborator (`*DomainHelper`, dedicated value object, or a separate use case) |
| Entity with computation logic | Move computation to a domain value object or a pure function in a `*DomainHelper` |
| Use case orchestrating too many steps | Split into finer-grained use cases chained by a higher-order command |

### Good shape in the domain

```kotlin
// ✅ One responsibility: validate business rule
class BudgetGuard(private val budget: Money) {
    fun check(amount: Money): Result<Unit> =
        if (amount <= budget) Result.success(Unit)
        else Result.failure(BudgetExceededError(budget, amount))
}

// ✅ One responsibility: orchestrate the use case
@DomainService
class BookExpenseService(
    private val bookletRepository: BookletRepository,
    private val guard: BudgetGuard
) : BookExpenseUseCase { ... }
```

---

## O — Open/Closed Principle

> A class should be **open for extension** but **closed for modification**.

### Domain signals

- A `when` or `if` branching on a type tag to select behavior → violation if new types require touching this class.
- A service that must be modified every time a new business variant is introduced → violation.

### Fixes

| Smell | Fix |
|---|---|
| `when (type)` selecting algorithm | **Strategy Pattern**: extract an interface per variant, inject the right one |
| Conditional creation of objects by type | **Factory / Abstract Factory**: delegate creation |
| Decorator-like enrichment (validation chains, rule sets) | **Decorator / Chain of Responsibility** |

```kotlin
// ❌ Closed to extension
fun calculate(type: String, amount: Money): Money =
    when (type) {
        "flat" -> amount
        "percent" -> amount * rate
        else -> throw IllegalArgumentException()
    }

// ✅ Open for extension
interface FeeStrategy {
    fun apply(amount: Money): Money
}
class FlatFee(private val fee: Money) : FeeStrategy { override fun apply(amount: Money) = fee }
class PercentFee(private val rate: BigDecimal) : FeeStrategy { override fun apply(amount: Money) = amount * rate }
```

---

## L — Liskov Substitution Principle

> Subtypes must be **fully substitutable** for their base types without altering correctness.

### Domain signals

- An implementation that throws `UnsupportedOperationException` for some inherited method → violation.
- An implementation that narrows preconditions or widens postconditions → violation.
- A `when (impl is XxxImpl)` in client code to compensate for inconsistent behaviour → violation.

### Fixes

| Smell | Fix |
|---|---|
| Override that throws "not supported" | **Interface Segregation** — split the interface so unsupported operations are simply absent |
| Type-checking at call site | Ensure the abstraction's contract is **complete and coherent** for all implementations |
| Different error strategies per impl | Unify the error type in the port contract; each impl maps to it |

---

## I — Interface Segregation Principle

> Clients should not depend on interfaces they **do not use**.

### Domain signals

- A repository port with 10+ methods when a use case only needs 2 → ISP violation.
- A use case port extending a generic interface that forces implementing unused methods → violation.

### Fixes

| Smell | Fix |
|---|---|
| Fat repository with all CRUD methods | Narrow the repository to the operations actually required by the domain (e.g. `BookletReader`, `BookletWriter`) |
| Use case interface with helpers | Keep the port to a **single `handle()` method** only; move helpers to the implementation |

```kotlin
// ❌ Fat port
interface TransactionRepository {
    fun save(t: Transaction): Transaction
    fun findById(id: UUID): Transaction?
    fun findAll(): List<Transaction>
    fun deleteAll()
    fun countByBooklet(bookletId: UUID): Int
    fun sumByTag(tagId: UUID): Money
}

// ✅ Focused ports — each use case gets what it needs
interface TransactionWriter { fun save(t: Transaction): Transaction }
interface TransactionReader { fun findById(id: UUID): Transaction? }
interface TransactionStats { fun sumByTag(tagId: UUID): Money }
```

---

## D — Dependency Inversion Principle

> High-level modules must not depend on low-level modules. Both must depend on **abstractions**.

### Domain signals

- A domain service constructing its own repositories or collaborators (`val repo = JpaTransactionRepository()`) → hard violation.
- A domain service importing anything from `infrastructure.*` or `application.*` → hard violation.
- A domain object with Spring annotations (`@Component`, `@Autowired`) → hard violation.

### Fixes

- Always inject **port interfaces** (defined in `domain/port/`) into domain services.
- Domain services receive their collaborators via **constructor injection** only.
- All framework wiring lives exclusively in the infrastructure or application layer.

```kotlin
// ✅ Domain depends only on its own port
@DomainService
class CreateBookletService(
    private val bookletRepository: BookletRepository,  // domain port, not JPA impl
    private val session: SessionManager
) : CreateBookletUseCase { ... }
```
