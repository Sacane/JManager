# Performance Improvements Report — JManager

> Written on February 21, 2026  
> Scope: backend (Spring Boot / Kotlin) + frontend (Nuxt 3 / Vue 3)

---

## Context

Users with a large number of recorded transactions were experiencing noticeable latency when
browsing a booklet (account). The initial analysis revealed several sources of degradation,
primarily in the infrastructure layer on the backend side, and in the network calls made by the
frontend.

---

## 1. Initial Diagnosis — Identified Issues

### 1.1 Loading All Transactions into Memory

**Affected file (before):**
`infra/…/adapters/transaction/TransactionRepositoryJpaAdapter.kt`

```kotlin
// BEFORE — all booklet transactions loaded into memory,
// then filtered on the JVM side
override fun findTransactionsByBookletYearAndMonth(bookletId: UUID, year: Int, month: Month) =
    bookletJpaRepository.findTransactionsById(bookletId)
        ?.sheets
        ?.filter { it.date.year == year && it.date.month == month }
        ?.map { it.toModel() }
```

The JPA repository was loading **all** transactions for the booklet (`LEFT JOIN FETCH account.sheets`),
then filtering results in JVM memory. For a user with 500+ transactions, this represented an
unnecessarily large volume of data transferred from the database and an O(n) mapping cost.

---

### 1.2 Full Aggregate Loaded to Compute Balances Only

**Affected file (before):**
`domain/…/port/api/BookletFeature.kt` — `loadTransactionsForBookletForAMonth`

The single controller entry point (`GET /api/account/report/{id}`) loaded:
1. The `Booklet` aggregate **with all its transactions** via `findAccountByIdWithTransactions`,
2. The associated regular transactions,
3. The regular transaction trackers **one by one** (N+1 calls),

…even when the frontend only needed the balances (`realSold` / `previewSold`) to display the
sidebar account cards.

---

### 1.3 N+1 Calls on Regular Transaction Trackers

**Affected file (before):**
`domain/…/port/api/BookletFeature.kt` — provisional transaction generation section

The code called `trackerRepository.findTracker(regularTransactionId, bookletId)` inside a loop
over each transaction for the month, generating as many SQL queries as there were active regular
transactions.

---

### 1.4 A Single Endpoint Serving Two Different Needs

The frontend consumed `GET /api/account/report/{id}?month=X&year=Y` for two distinct use cases:
- Displaying **balances** in the sidebar / summary card.
- Displaying the **transaction list** in the booklet detail view.

A single call returned all the data even though each view only needed part of it.

---

## 2. Improvements Applied

### 2.1 Database-side Transaction Filtering

**New files:**
- `domain/…/port/spi/repository/TransactionQueryRepository.kt` *(new SPI port)*
- `infra/…/repositories/TransactionQueryJpaRepository.kt` *(new read-only JPA repository)*
- `infra/…/adapters/transaction/TransactionQueryRepositoryJpaAdapter.kt` *(new adapter)*

**Introduced JPQL query:**

```kotlin
@Query("""
    SELECT s
    FROM TransactionResource s
    LEFT JOIN FETCH s.personalTag
    LEFT JOIN FETCH s.tag
    WHERE s.account.idAccount = :bookletId
      AND s.date >= :from
      AND s.date <= :to
    ORDER BY s.date, s.lastModified
""")
fun findByBookletIdAndDateBetween(bookletId: UUID, from: LocalDate, to: LocalDate): List<TransactionResource>
```

Date range filtering is now delegated to the SQL engine. Only the transactions for the requested
month travel over the application network and are mapped to domain objects.

**Impact:** linear reduction in processed volume — O(n total) → O(n month).

---

### 2.2 "Balances Only" Projection Without Loading the Aggregate

**New files:**
- `domain/…/port/spi/repository/BookletBalanceQueryRepository.kt` *(new SPI port)*
- `infra/…/repositories/BookletBalanceJpaRepository.kt` *(read-only JPA repository, Spring Data projection)*
- `infra/…/adapters/BookletBalanceQueryRepositoryJpaAdapter.kt` *(adapter)*

**Introduced JPQL query:**

```kotlin
@Query("""
    SELECT acc.label AS label, acc.amount AS amount, acc.previewAmount AS previewAmount
    FROM BookletResource acc
    WHERE acc.idAccount = :id
""")
fun findPersistedBalances(id: UUID): PersistedBalancesRow?
```

Instead of loading the entire `Booklet` aggregate (label + all transactions + regularTransactions),
a minimal projection selects only the three columns needed to compute the balances.

**Impact:** from a `LEFT JOIN FETCH` query over hundreds of rows to an O(1) query on a single row
of the `account` table.

---

### 2.3 Eliminating N+1 Calls on Trackers

**Modified file:**
`domain/…/port/api/BookletFeature.kt` — `loadTransactionsForBookletForAMonth`

```kotlin
// BEFORE — one call per regular transaction
regularTransactions.forEach { rt ->
    val tracker = trackerRepository.findTracker(rt.id, bookletId) // N queries
    ...
}

// AFTER — bulk load, then O(1) map lookup
val trackersByRegularId = trackerRepository
    .findAllTrackersForBooklet(bookletId)       // single query
    .associateBy { it.regularTransactionId }

filteredTransactions.filter { transaction ->
    val tracker = trackersByRegularId[transaction.regularTransactionId] // O(1)
    ...
}
```

**Method added to the port:**
`RegularTransactionTrackerRepository.findAllTrackersForBooklet(bookletId: UUID)`  
**JPA implementation:**
`JpaRegularTransactionTrackerRepository.findAllByBookletId(bookletId: UUID)`

**Impact:** N SQL queries → 1 SQL query, regardless of the number of regular transactions.

---

### 2.4 Endpoint Decoupling — Balances vs Transactions

**Modified file:**
`infra/…/api/booklet/Controller.kt`

Two new REST endpoints were added:

| Endpoint | Returned data | Cost |
|---|---|---|
| `GET /api/account/{id}/balances?month=X&year=Y` | `label`, `realSold`, `previewSold` | Lightweight — DB projection |
| `GET /api/account/{id}/transactions?month=X&year=Y` | Transaction list for the month | Bounded to the month |

The old `GET /api/account/report/{id}` remains available for backwards compatibility but is no
longer used by the main views.

**New DTOs:**
- `BookletBalancesResponse(label, realSold, previewSold)`
- `BookletTransactionsResponse(transactions)`

**New domain use-case:**
`BookletFeature.loadBalancesForBookletForAMonth(...)` → `Result<BookletBalances>`

This use-case builds a **minimal** aggregate (`Booklet` with no transactions) relying exclusively
on the balance projection, then computes `previsionalSold` by loading only the provisional
transactions within the current→target time window.

---

### 2.5 Frontend Adaptation — Parallel and Targeted Requests

**Modified file:**
`client/composables/useBooklet.ts`

Two new functions are now exposed:

```typescript
async function findBalancesByIdMonthAndYear(
  accountId: string, month: number, year: number
): Promise<BookletBalancesDTO> {
  return get(`account/${accountId}/balances`, { month, year })
}

async function findTransactionsByIdMonthAndYear(
  accountId: string, month: number, year: number
): Promise<BookletTransactionsDTO> {
  return get(`account/${accountId}/transactions`, { month, year })
}
```

**Modified file:**
`client/pages/account/[id].vue` — `loadBookletData` function

```typescript
// BEFORE — a single call returning everything
const result: BookletReport = await findByIdMonthAndYear(accountId, month, year)

// AFTER — two parallel targeted calls
const [balances, transactionsRes] = await Promise.all([
  findBalancesByIdMonthAndYear(accountId, month, year),
  findTransactionsByIdMonthAndYear(accountId, month, year),
])
```

**Impact:**
- Both requests execute in parallel (network gain).
- The view can now display balances independently from the list (progressive rendering possible).
- Fixed a JavaScript bug: `accounts is undefined` caused by consuming the old response whose
  structure had changed.

---

## 3. Summary of Created / Modified Files

### New files

| File | Role |
|---|---|
| `domain/…/repository/TransactionQueryRepository.kt` | Read-optimized SPI port for transactions |
| `domain/…/repository/BookletBalanceQueryRepository.kt` | Balance projection SPI port |
| `domain/…/models/BookletBalances.kt` | Lightweight domain model |
| `infra/…/repositories/TransactionQueryJpaRepository.kt` | Read-only JPA repository with filtered JPQL |
| `infra/…/repositories/BookletBalanceJpaRepository.kt` | Read-only JPA repository — 3-column projection |
| `infra/…/adapters/transaction/TransactionQueryRepositoryJpaAdapter.kt` | Hexagonal adapter |
| `infra/…/adapters/BookletBalanceQueryRepositoryJpaAdapter.kt` | Hexagonal adapter |

### Modified files

| File | Change |
|---|---|
| `domain/…/port/api/BookletFeature.kt` | Added `loadBalancesForBookletForAMonth`, injected new ports, bulk tracker loading, timing logs |
| `infra/…/api/booklet/Controller.kt` | Added `/balances` and `/transactions` endpoints, `BookletBalancesResponse` / `BookletTransactionsResponse` DTOs |
| `client/composables/useBooklet.ts` | Added `findBalancesByIdMonthAndYear` and `findTransactionsByIdMonthAndYear` |
| `client/pages/account/[id].vue` | Replaced single call with `Promise.all`, updated response consumption |

---

## 4. Gains Summary Table

| Issue | Before | After | Technique |
|---|---|---|---|
| Transaction filtering | All TX loaded into JVM, application-side filtering | SQL `WHERE date BETWEEN` filtering | JPQL read-only bounded query |
| Loading for balances | Full aggregate (TX + regular TX) | 3-column projection | Spring Data projection interface |
| N+1 trackers | 1 SQL query per regular transaction | 1 query + `Map` lookup | Bulk load + `associateBy` |
| Overloaded single endpoint | 1 call returning all data | 2 specialised endpoints | CQRS-like read-side split |
| Sequential frontend calls | 1 blocking call | 2 parallel calls (`Promise.all`) | HTTP parallelisation |

---

## 5. Architecture and Hexagonal Compliance

All improvements comply with the project's hexagonal architecture:

- The new ports (`TransactionQueryRepository`, `BookletBalanceQueryRepository`) are defined in the
  **domain** module and annotated `@Port(Side.INFRASTRUCTURE)`.
- Concrete implementations (JPA) reside exclusively in the **infra** module and do not leak into
  the business logic.
- The domain has no dependency on any JPA/SQL detail.
- The `previsionalSold` calculation logic remains entirely within the domain service
  `BookletFeatureImpl`.

> **Note:** The query ports (`TransactionQueryRepository`, `BookletBalanceQueryRepository`) were
> introduced as dedicated read-side ports (*read-side / query port* pattern), allowing optimised
> queries to be exposed without altering the existing write ports
> (`BookletRepository`, `TransactionRepository`).

