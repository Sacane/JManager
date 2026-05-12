# Algorithm Strategies — Domain Engineering Reference

This reference covers the algorithmic thinking and problem-solving strategies most applicable to **business domain logic**.
The goal is always the same: find the **simplest, most expressive, and most correct** algorithm for the problem.

> **Principle**: prefer clarity over cleverness. The best domain algorithm reads like the business requirement itself.

---

## 1. Problem Classification

Before writing any code, classify the problem:

| Type | Description | Typical domain examples |
|---|---|---|
| **Transformation** | Map input data to output data without side effects | Converting a list of raw rows to domain objects, calculating a monthly summary |
| **Aggregation** | Reduce a collection to a single value | Summing transactions, computing a balance, counting occurrences |
| **Filtering / Selection** | Select a subset matching a predicate | Finding overdue transactions, selecting active booklets |
| **Sorting / Ranking** | Order elements by a business criterion | Transactions by date, tags by usage frequency |
| **Search** | Locate an element or determine membership | Finding a transaction by ID, checking if a tag exists |
| **Validation** | Check that a set of business invariants hold | Budget guard, recurrence constraint, overlap detection |
| **Scheduling / Recurrence** | Generate a sequence of occurrences from a rule | Regular transaction next dates, billing cycle generation |
| **Partitioning** | Group elements into distinct buckets | Grouping transactions by month, by tag, by type |

---

## 2. Core Algorithmic Principles in the Domain

### 2.1 Immutability First

Prefer **pure transformations** over stateful mutations:

```kotlin
// ❌ Stateful accumulation — hard to test, error-prone
fun computeBalance(transactions: List<Transaction>): Money {
    var balance = Money.ZERO
    for (t in transactions) {
        balance = balance + t.amount
    }
    return balance
}

// ✅ Pure fold — predictable, testable, Kotlin idiomatic
fun computeBalance(transactions: List<Transaction>): Money =
    transactions.fold(Money.ZERO) { acc, t -> acc + t.amount }
```

### 2.2 Collection Pipeline (Map / Filter / Reduce)

Chain Kotlin collection operations to express business intent as a pipeline:

```kotlin
// Business intent: "total credits per tag for the current month"
fun totalCreditsByTag(transactions: List<Transaction>, month: YearMonth): Map<TagId, Money> =
    transactions
        .filter { it.occurredIn(month) && it.isCredit() }
        .groupBy { it.tagId }
        .mapValues { (_, txs) -> txs.fold(Money.ZERO) { acc, t -> acc + t.amount } }
```

### 2.3 Early Return over Deep Nesting

Prefer guard clauses and early returns over deeply nested conditionals:

```kotlin
// ❌ Deeply nested
fun validateTransaction(t: Transaction): Result<Unit> {
    if (t.amount > Money.ZERO) {
        if (t.bookletId != null) {
            if (!t.isExpired()) {
                return Result.success(Unit)
            }
        }
    }
    return Result.failure(ValidationError())
}

// ✅ Guard clauses
fun validateTransaction(t: Transaction): Result<Unit> {
    if (t.amount <= Money.ZERO) return Result.failure(InvalidAmountError(t.amount))
    if (t.bookletId == null)   return Result.failure(MissingBookletError())
    if (t.isExpired())         return Result.failure(ExpiredTransactionError(t.id))
    return Result.success(Unit)
}
```

### 2.4 Result Chaining (Railway-Oriented Programming)

When multiple operations can fail, chain `Result` instead of nesting:

```kotlin
fun processBooking(command: BookTransactionCommand): Result<Transaction> =
    validateAmount(command.amount)
        .flatMap { findBooklet(command.bookletId) }
        .flatMap { booklet -> applyBudgetGuard(booklet, command.amount) }
        .map { booklet -> Transaction.from(command, booklet) }
```

> Kotlin's standard `Result` does not include `flatMap`. Use `mapCatching` or introduce a small extension if chaining is needed.

---

## 3. Complexity Awareness

Always reason about complexity before implementing. The goal is not premature optimisation — it is **avoiding obvious bottlenecks**.

### Complexity Reference

| Operation | Complexity | Notes |
|---|---|---|
| List traversal (single pass) | O(n) | The default; prefer this |
| Nested loops on same collection | O(n²) | Acceptable for n < 1000; consider alternatives above |
| Hash lookup (`Map`, `Set`) | O(1) average | Use when repeated membership checks are needed |
| Sorting | O(n log n) | Fine for domain collections; avoid inside loops |
| Repeated `filter` on same list | O(k·n) | Materialise a partition first |

### Typical domain anti-patterns

```kotlin
// ❌ O(n²) — searching inside a loop
transactions.forEach { t ->
    val tag = allTags.find { it.id == t.tagId }  // linear search per transaction
    ...
}

// ✅ O(n) — build a lookup map first
val tagIndex: Map<TagId, Tag> = allTags.associateBy { it.id }
transactions.forEach { t ->
    val tag = tagIndex[t.tagId]  // O(1) lookup
    ...
}
```

---

## 4. Recurrence and Date Arithmetic

Recurrence calculations are a common source of subtle bugs. Follow these rules:

1. **Use `java.time` types exclusively** — never `Date`, `Calendar`, or raw timestamps.
2. **Represent ranges as closed-open `[from, to)`** — inclusive start, exclusive end — for easy overlap detection.
3. **Be explicit about the time zone context** — store and compare in UTC; convert for display only.
4. **Avoid "current date" in pure domain logic** — inject `Clock` or pass the reference date as a parameter (enables deterministic testing).

```kotlin
// ✅ Inject Clock — never call LocalDate.now() directly in domain logic
class GenerateRecurrenceService(private val clock: Clock) {
    fun nextOccurrence(rule: RecurrenceRule): LocalDate =
        rule.nextAfter(LocalDate.now(clock))
}
```

### Overlap detection (closed-open intervals)

```kotlin
data class DateRange(val from: LocalDate, val toExclusive: LocalDate) {
    init { require(from < toExclusive) { "Range must be positive" } }

    fun overlaps(other: DateRange): Boolean =
        from < other.toExclusive && other.from < toExclusive

    fun contains(date: LocalDate): Boolean =
        date >= from && date < toExclusive
}
```

---

## 5. Aggregation and Balance Calculations

Monetary aggregation is the core of most financial domain logic. Key rules:

1. **Never use `Double` or `Float` for money** — use `BigDecimal` with explicit scale and rounding mode.
2. **Sum lazily when the collection is large** — use `fold` or `sumOf`, not repeated additions in imperative loops.
3. **Track signs explicitly** — a debit is not a negative credit; model the intent, then apply the sign convention once at the boundary.

```kotlin
// ✅ Explicit sign convention — model intent, apply sign at aggregation
fun netBalance(transactions: List<Transaction>): Money =
    transactions.fold(Money.ZERO) { acc, t ->
        when (t.type) {
            TransactionType.CREDIT -> acc + t.amount
            TransactionType.DEBIT  -> acc - t.amount
        }
    }
```

---

## 6. Partitioning and Grouping

When grouping domain objects, always consider:
- **Partition by a natural key** (`groupBy`) for uniform bucket sizes.
- **Use `associateBy` over `groupBy`** when the key is unique (avoids unnecessary `List` wrapping).
- **Consider `toMap` with merge function** when duplicates must be resolved.

```kotlin
// Group transactions by month
val byMonth: Map<YearMonth, List<Transaction>> =
    transactions.groupBy { YearMonth.from(it.date) }

// Index unique entities
val bookletById: Map<BookletId, Booklet> =
    booklets.associateBy { it.id }

// Merge amounts per tag (sum duplicates)
val amountPerTag: Map<TagId, Money> =
    transactions.groupingBy { it.tagId }
        .fold(Money.ZERO) { acc, t -> acc + t.amount }
```

---

## 7. Elegance Checklist

Before finalising any algorithm, verify:

- [ ] **Reads like the business rule** — a domain expert could understand it without knowing Kotlin.
- [ ] **No incidental state** — no mutable local variables unless strictly necessary.
- [ ] **Single pass where possible** — avoid iterating the same collection twice.
- [ ] **Errors are explicit** — no silent swallowing, no magic defaults.
- [ ] **Complexity is justified** — if O(n²) or worse, document why it is acceptable or propose an alternative.
- [ ] **Testable in isolation** — no hidden dependency on `LocalDate.now()`, `UUID.randomUUID()`, or external I/O.
