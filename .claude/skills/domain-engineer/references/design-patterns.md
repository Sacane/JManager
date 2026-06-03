# Design Patterns — Domain Engineering Reference

This catalogue lists the patterns most relevant to **domain-layer** work in a hexagonal / DDD context.
For each pattern: the signal that indicates it is needed, the domain-specific application, and a Kotlin example.

> **Rule**: propose a pattern only when a **concrete structural problem** exists. Never apply speculatively.  
> Always describe the problem, name the pattern, and **wait for user confirmation** before implementing.

> **Kotlin-first rule**: before introducing a full class hierarchy to implement a classic GoF pattern, always check  
> whether a **Kotlin language feature** already expresses the same intent more concisely with less ceremony.  
> Do not reinvent what the language already provides.

---

## Kotlin-Native Pattern Implementations

Kotlin is a **pattern language**. Many classical GoF patterns have a direct, built-in Kotlin equivalent that is
simpler, more expressive, and idiomatic. Prefer these over their ceremonious Java-style counterparts.

### Strategy → Higher-Order Function

When the strategy has a single method and no shared state, a lambda is strictly superior to an interface.

```kotlin
// ❌ Java-style strategy — unnecessary interface for a single behaviour
interface FeeCalculator {
    fun calculate(amount: Money): Money
}
class FlatFeeCalculator(private val fee: Money) : FeeCalculator {
    override fun calculate(amount: Money) = fee
}

// ✅ Kotlin-native — the function IS the strategy
typealias FeeCalculator = (Money) -> Money

val flatFee: FeeCalculator = { _ -> Money.of(5.toBigDecimal()) }
val percentFee: FeeCalculator = { amount -> amount * BigDecimal("0.02") }

// Caller is clean and composable
fun applyFee(amount: Money, calculator: FeeCalculator): Money = calculator(amount)
```

Use a **named interface** only when the strategy carries state, multiple methods, or needs to be injected as a Spring bean.

---

### State → `sealed class` with Behaviour per Variant

`sealed class` + `when` eliminates the classic State pattern class explosion. Each variant owns its behaviour.

```kotlin
// ✅ State machine as a sealed hierarchy — exhaustive, no state manager class needed
sealed class BookletStatus {
    abstract fun canReceiveTransaction(): Boolean
    abstract fun transition(): BookletStatus

    data object Open : BookletStatus() {
        override fun canReceiveTransaction() = true
        override fun transition() = Locked
    }

    data object Locked : BookletStatus() {
        override fun canReceiveTransaction() = false
        override fun transition() = Archived
    }

    data object Archived : BookletStatus() {
        override fun canReceiveTransaction() = false
        override fun transition() = throw IllegalStateException("Archived booklets cannot transition")
    }
}

// Exhaustive dispatch — the compiler enforces completeness
fun describe(status: BookletStatus): String = when (status) {
    is BookletStatus.Open   -> "Accepting transactions"
    is BookletStatus.Locked -> "Read-only"
    is BookletStatus.Archived -> "Permanently closed"
}
```

---

### Visitor / Exhaustive Dispatch → `when` on `sealed class`

No need for a Visitor interface hierarchy. `when` on a `sealed class` is the Kotlin Visitor.

```kotlin
sealed class DomainEvent
data class TransactionCreated(val id: TransactionId, val amount: Money) : DomainEvent()
data class BookletClosed(val bookletId: BookletId, val closedAt: Instant) : DomainEvent()
data class TagRenamed(val tagId: TagId, val newName: String) : DomainEvent()

// The "visit" — compiler guarantees all cases are handled
fun handle(event: DomainEvent) = when (event) {
    is TransactionCreated -> updateBalance(event.id, event.amount)
    is BookletClosed      -> archiveBooklet(event.bookletId)
    is TagRenamed         -> reindexTag(event.tagId, event.newName)
}
```

---

### Value Object → `@JvmInline value class`

Single-field value objects with zero runtime overhead. The compiler unwraps them at the call site.

```kotlin
// ✅ Strongly-typed domain identifiers — no accidental ID mix-up, zero allocation overhead
@JvmInline value class BookletId(val value: UUID) {
    companion object { fun generate() = BookletId(UUID.randomUUID()) }
}

@JvmInline value class UserId(val value: UUID) {
    companion object { fun generate() = UserId(UUID.randomUUID()) }
}

// These are now different types — the compiler prevents passing a UserId where a BookletId is expected
fun findBooklet(id: BookletId): Booklet? = ...
```

---

### Singleton → `object`

No singleton pattern boilerplate. `object` declarations are thread-safe singletons by definition.

```kotlin
// ✅ Stateless domain service as a singleton
object MoneyFormatter {
    fun format(money: Money, locale: Locale): String =
        NumberFormat.getCurrencyInstance(locale).format(money.amount)
}
```

---

### Factory Method → `companion object`

Private constructor + `companion object` factory enforces invariants at construction time.

```kotlin
data class DateRange private constructor(
    val from: LocalDate,
    val toExclusive: LocalDate
) {
    companion object {
        fun of(from: LocalDate, toExclusive: LocalDate): DateRange {
            require(from < toExclusive) { "Range must be positive: [$from, $toExclusive)" }
            return DateRange(from, toExclusive)
        }

        fun month(month: YearMonth): DateRange =
            DateRange(month.atDay(1), month.atEndOfMonth().plusDays(1))
    }
}
```

---

### Decorator / Extension → Extension Functions + `by` Delegation

Add behaviour to a type without subclassing or wrapping boilerplate.

```kotlin
// ✅ Extension function as a zero-ceremony Decorator
fun Money.applyDiscount(percent: BigDecimal): Money =
    Money.of(this.amount * (BigDecimal.ONE - percent / BigDecimal("100")))

// ✅ `by` delegation — forward all methods, override only what changes
class AuditedRepository(
    private val delegate: BookletRepository,
    private val auditLog: AuditLog
) : BookletRepository by delegate {
    override fun save(booklet: Booklet): Booklet {
        val saved = delegate.save(booklet)
        auditLog.record("booklet.saved", saved.id)
        return saved
    }
}
```

---

### Immutable Update → `data class` `copy()`

No Builder class needed for simple immutable state transitions. `copy()` is the builder.

```kotlin
data class Booklet(
    val id: BookletId,
    val name: String,
    val currency: Currency,
    val status: BookletStatus
) {
    fun rename(newName: String): Booklet = copy(name = newName)
    fun lock(): Booklet = copy(status = BookletStatus.Locked)
    fun archive(): Booklet = copy(status = BookletStatus.Archived)
}
```

---

### Domain DSL → `infix` + Type-Safe Builders

`infix` functions produce expressive, English-like domain expressions without a DSL framework.

```kotlin
// ✅ Infix for readable business predicates
infix fun Money.isLessThan(other: Money): Boolean = this.amount < other.amount
infix fun Money.exceeds(budget: Money): Boolean = this.amount > budget.amount
infix fun LocalDate.isBefore(other: LocalDate): Boolean = this.isBefore(other)

// Usage reads like a business rule
if (totalSpent exceeds monthlyBudget) return Result.failure(BudgetExceededError(...))
```

---

### Lazy Computation → `Sequence` + `by lazy`

Prefer `Sequence` over `List` for large domain pipelines to avoid creating intermediate collections.

```kotlin
// ✅ Lazy pipeline — intermediate lists are never materialised
fun overdueTransactions(transactions: Sequence<Transaction>, today: LocalDate): Sequence<Transaction> =
    transactions
        .filter { it.dueDate != null }
        .filter { it.dueDate!! isBefore today }
        .filter { !it.isPaid }

// ✅ Lazy property — computed only on first access, never recomputed
class BookletSummary(private val transactions: List<Transaction>) {
    val totalDebits: Money by lazy { transactions.filter { it.isDebit() }.fold(Money.ZERO) { acc, t -> acc + t.amount } }
    val totalCredits: Money by lazy { transactions.filter { it.isCredit() }.fold(Money.ZERO) { acc, t -> acc + t.amount } }
    val netBalance: Money by lazy { totalCredits - totalDebits }
}
```

---

### Decision Matrix: Kotlin Feature vs. Classic Pattern

Use this table to decide the right tool before reaching for a full class hierarchy:

| Problem | Kotlin-first | Full pattern (when Kotlin-first is not enough) |
|---|---|---|
| Single-method algorithm variant | Lambda / `typealias` | `Strategy` interface (multiple methods or Spring bean) |
| Exhaustive state with behaviour | `sealed class` + `when` | Classic State pattern |
| Exhaustive case dispatch | `when` on `sealed` | Visitor pattern |
| Strongly-typed single-field concept | `@JvmInline value class` | `data class` (multiple fields or operations) |
| Stateless service | `object` | `@DomainService` class (stateful, injectable) |
| Invariant-enforced construction | `companion object` factory | Builder (many optional fields) |
| Adding behaviour without modification | Extension function | Decorator class (stateful decoration) |
| Forwarding with one override | `by` delegation | Full Decorator class |
| Immutable state transition | `data class` `copy()` | Builder (when transitions are complex) |
| Readable business predicate | `infix` function | Specification (composable predicates) |
| Large collection pipeline | `Sequence` | Custom iterator or cursor |
| Deferred / expensive property | `by lazy` | Explicit cache field |

---

## Creational Patterns

### Factory Method / Abstract Factory

**Signal**: Object creation logic is complex, conditional, or must remain extensible without touching call sites.

**Domain use cases**:
- Creating a domain aggregate from a raw command (e.g. `Transaction.from(command)`).
- Building variants of a value object without exposing internal conditionals.

```kotlin
// Companion object factory on the aggregate — creation logic lives in the domain
data class Transaction private constructor(
    val id: TransactionId,
    val amount: Money,
    val type: TransactionType
) {
    companion object {
        fun credit(amount: Money): Transaction =
            Transaction(TransactionId.generate(), amount, TransactionType.CREDIT)

        fun debit(amount: Money): Transaction =
            Transaction(TransactionId.generate(), amount.negate(), TransactionType.DEBIT)
    }
}
```

### Builder (value-object construction)

**Signal**: A value object or aggregate has many optional fields and complex validation at construction time.

**Domain use cases**: Building a `SearchCriteria` query object, constructing a complex `Report` value object.

```kotlin
data class SearchCriteria private constructor(
    val bookletId: BookletId?,
    val tagIds: Set<TagId>,
    val dateRange: DateRange?,
    val amountRange: AmountRange?
) {
    class Builder {
        private var bookletId: BookletId? = null
        private var tagIds: Set<TagId> = emptySet()
        private var dateRange: DateRange? = null
        private var amountRange: AmountRange? = null

        fun booklet(id: BookletId) = apply { bookletId = id }
        fun tags(ids: Set<TagId>) = apply { tagIds = ids }
        fun dateRange(range: DateRange) = apply { dateRange = range }
        fun amountRange(range: AmountRange) = apply { amountRange = range }

        fun build(): SearchCriteria = SearchCriteria(bookletId, tagIds, dateRange, amountRange)
    }
}
```

---

## Structural Patterns

### Decorator

**Signal**: A behaviour needs to be augmented (logging, validation, caching) without modifying the decorated class.

**Domain use cases**:
- Wrapping a repository port with validation before persistence.
- Adding audit trail capability to a use case without altering its logic.

> In hexagonal architecture, decorators typically live in **infrastructure** or **application** — not the domain core.  
> The domain defines the port; the decoration wraps the adapter.

### Composite

**Signal**: Individual objects and compositions of those objects must be treated uniformly.

**Domain use cases**: Hierarchical tags (parent/child), tree-structured budget categories, nested rule sets.

```kotlin
sealed class BudgetRule {
    abstract fun evaluate(transaction: Transaction): Boolean

    data class Simple(val predicate: (Transaction) -> Boolean) : BudgetRule() {
        override fun evaluate(transaction: Transaction) = predicate(transaction)
    }

    data class All(val rules: List<BudgetRule>) : BudgetRule() {
        override fun evaluate(transaction: Transaction) = rules.all { it.evaluate(transaction) }
    }

    data class Any(val rules: List<BudgetRule>) : BudgetRule() {
        override fun evaluate(transaction: Transaction) = rules.any { it.evaluate(transaction) }
    }
}
```

---

## Behavioural Patterns

### Strategy

**Signal**: An algorithm must vary independently from the clients that use it; selection of behaviour depends on a runtime condition.

**Domain use cases**:
- Different fee calculation modes (flat, percentage, tiered).
- Different transaction classification algorithms.
- Different recurrence rules (weekly, monthly, custom).

```kotlin
// Port in domain — closed to modification, open to new strategies
interface RecurrenceStrategy {
    fun nextOccurrence(from: LocalDate): LocalDate
    fun occurrencesBetween(from: LocalDate, to: LocalDate): List<LocalDate>
}

class WeeklyRecurrence(private val dayOfWeek: DayOfWeek) : RecurrenceStrategy { ... }
class MonthlyRecurrence(private val dayOfMonth: Int) : RecurrenceStrategy { ... }
class CustomIntervalRecurrence(private val intervalDays: Int) : RecurrenceStrategy { ... }
```

### Template Method

**Signal**: Multiple algorithms share the same skeleton but differ at specific steps.

**Domain use cases**: Import processing pipeline (validate → transform → persist), report generation.

```kotlin
abstract class TransactionImportProcessor {
    // Template method — the skeleton is fixed
    fun process(raw: List<RawRow>): Result<ImportSummary> {
        val validated = validate(raw)
        val transformed = transform(validated)
        return persist(transformed)
    }

    // Steps left for subclasses to define
    protected abstract fun validate(rows: List<RawRow>): List<ValidatedRow>
    protected abstract fun transform(rows: List<ValidatedRow>): List<Transaction>
    protected abstract fun persist(transactions: List<Transaction>): Result<ImportSummary>
}
```

### Chain of Responsibility

**Signal**: A request must pass through a sequence of handlers, each able to process or forward it.

**Domain use cases**: Business validation chains, rule engines, permission checks before an operation.

```kotlin
interface ValidationRule<T> {
    fun validate(subject: T): Result<Unit>
}

class CompositeValidator<T>(private val rules: List<ValidationRule<T>>) {
    fun validate(subject: T): Result<Unit> =
        rules.asSequence()
            .map { it.validate(subject) }
            .firstOrNull { it.isFailure }
            ?: Result.success(Unit)
}
```

### Observer / Domain Events

**Signal**: An action in the domain must trigger reactions without the emitter knowing who reacts.

**Domain use cases**: Publishing a `TransactionCreatedEvent` after a booking, notifying that a booklet has been closed.

```kotlin
// Domain event — pure data, no framework dependency
data class TransactionCreatedEvent(
    val transactionId: TransactionId,
    val bookletId: BookletId,
    val amount: Money,
    val occurredAt: Instant
) : DomainEvent

// Domain service emits the event — infrastructure handles the dispatch
@DomainService
class BookTransactionService(
    private val repository: TransactionRepository,
    private val eventPublisher: DomainEventPublisher  // port in domain
) : BookTransactionUseCase {
    override fun handle(command: BookTransactionCommand): Result<Transaction> {
        val transaction = Transaction.from(command)
        val saved = repository.save(transaction)
        eventPublisher.publish(TransactionCreatedEvent.from(saved))
        return Result.success(saved)
    }
}
```

### State

**Signal**: An object changes its behaviour fundamentally based on its internal state.

**Domain use cases**: Booklet lifecycle (open → locked → archived), transaction approval workflow.

```kotlin
sealed class BookletStatus {
    abstract fun canReceiveTransaction(): Boolean
    abstract fun canBeLocked(): Boolean

    object Open : BookletStatus() {
        override fun canReceiveTransaction() = true
        override fun canBeLocked() = true
    }

    object Locked : BookletStatus() {
        override fun canReceiveTransaction() = false
        override fun canBeLocked() = false
    }

    object Archived : BookletStatus() {
        override fun canReceiveTransaction() = false
        override fun canBeLocked() = false
    }
}
```

### Specification

**Signal**: Business rules for selecting or validating domain objects are complex and must be composable.

**Domain use cases**: Eligibility checks, filtering logic encapsulating business predicates.

```kotlin
interface Specification<T> {
    fun isSatisfiedBy(candidate: T): Boolean
    fun and(other: Specification<T>): Specification<T> = And(this, other)
    fun or(other: Specification<T>): Specification<T> = Or(this, other)
    fun not(): Specification<T> = Not(this)
}

class And<T>(private val a: Specification<T>, private val b: Specification<T>) : Specification<T> {
    override fun isSatisfiedBy(candidate: T) = a.isSatisfiedBy(candidate) && b.isSatisfiedBy(candidate)
}

class Or<T>(private val a: Specification<T>, private val b: Specification<T>) : Specification<T> {
    override fun isSatisfiedBy(candidate: T) = a.isSatisfiedBy(candidate) || b.isSatisfiedBy(candidate)
}

class Not<T>(private val spec: Specification<T>) : Specification<T> {
    override fun isSatisfiedBy(candidate: T) = !spec.isSatisfiedBy(candidate)
}
```

---

## DDD-Specific Patterns

### Value Object

**Signal**: A domain concept has no identity — only its value matters. Equality is structural.

**Rules**:
- Always immutable (`val` only, no setters).
- Validate invariants in the constructor or factory function.
- Rich behaviour lives on the value object itself.

```kotlin
@JvmInline
value class Money private constructor(val amount: BigDecimal) {
    companion object {
        fun of(amount: BigDecimal): Money {
            require(amount >= BigDecimal.ZERO) { "Money cannot be negative" }
            return Money(amount)
        }
    }

    operator fun plus(other: Money): Money = Money(amount + other.amount)
    operator fun minus(other: Money): Money = Money.of(amount - other.amount)
    operator fun compareTo(other: Money): Int = amount.compareTo(other.amount)
    fun negate(): Money = Money(amount.negate())
}
```

### Aggregate Root

**Signal**: A cluster of domain objects must be treated as a single unit for data changes; all changes go through one entry point.

**Rules**:
- Only the root is accessible from outside the aggregate.
- External references hold only the root's ID, not inner objects.
- Business invariants that span multiple child entities are enforced by the root.

### Repository (Port)

**Signal**: The domain must store and retrieve aggregates without knowing the persistence technology.

**Rules**:
- Repository ports live in the `domain` layer.
- Methods are expressed in **domain terms**, not SQL terms.
- Return domain objects, never persistence entities or raw results.

```kotlin
// domain/port/output/
interface BookletRepository {
    fun save(booklet: Booklet): Booklet
    fun findById(id: BookletId): Booklet?
    fun findAllByOwner(userId: UserId): List<Booklet>
    fun delete(id: BookletId)
}
```
