# Investigation Report — Daily Account Balance Curve

**Date:** 2026-05-08
**Status:** Draft

---

## 1. Problem Statement

The user wants to display a curve on the dashboard showing the account balance evolving day by day
over a selected period. The goal is to track financial health over time, understanding at any given
day whether the account is growing, stagnating or declining — with the balance grounded in the
actual account state, not an arbitrary zero baseline.

---

## 2. Context

### What already exists

The infrastructure for this feature is **90% in place**. A detailed audit of the codebase reveals:

**Domain layer** (`domain/`)
- `DailyTrend` model: carries `date`, `income`, `expenses`, `balance`, `cumulativeBalance`, `totalBooklets`.
- `DailyTrendCalculatorImpl`: iterates every day in the requested range and accumulates income/expenses
  into a running `cumulativeBalance`. However, `cumulativeBalance` is initialised at `BigDecimal.ZERO`,
  not at the booklet's actual balance before the period.
- `GetDailyTrendStatsUseCase` / `GetDailyTrendStatsService`: full port + service implementation;
  accepts `userId`, `startDate`, `endDate`, and an optional `bookletId`.
- `withScopedBooklets` helper: fetches either a single booklet (with all its transactions) or all
  booklets owned by the user when no booklet is scoped.

**Application layer** (`application/`)
- `GET /stats/daily-trends` REST endpoint is implemented and wired.

**Frontend** (`client/`)
- `useStats.getDailyTrendStats()` composable wraps the endpoint.
- `dailyTrendStats` reactive ref and `DailyTrendStatsDTO` type are declared.
- `expensesTrendData` computed property in `index.vue` already builds a three-series Chart.js line
  chart for the monthly period view:
  - "Dépenses" (red, filled)
  - "Revenus" (green, filled)
  - "Solde cumulé" (purple, dashed) — mapped from `cumulativeBalance`
- The chart is rendered under the "Évolution des finances" section and is already visible in the UI.

### The gap

The `DailyTrendCalculatorImpl` initialises `cumulativeBalance = BigDecimal.ZERO`. The published
`cumulativeBalance` therefore represents the **net delta over the selected period starting from
zero** — not the booklet's actual running balance. If the account holds 5 000 € on day 1, the
chart starts at 0, not at 5 000 €.

To display the **true account balance per day**, the cumulative sum must be seeded with the
booklet's balance at the moment the period begins.

### Balance at `startDate`: how to compute it

`Booklet` carries two relevant fields:
- `amount` — the current running balance, maintained by the domain as transactions are applied.
- `initialSold` — the balance at creation time.

`findBookletByIdWithTransactions` and `findBookletsForUser` both load booklets with **all**
transactions in memory. This means the domain can compute the balance at any past date without a
new query:

```
balanceAtStartDate = initialSold
  + sum of all non-preview transactions with date < startDate
    where income transactions are added and expense transactions are subtracted
```

This computation is pure domain logic (no extra SPI call needed), provided all transactions are
already in memory — which they are, given the current loading contract.

---

## 3. Impact Analysis

### Domain Layer

The change is localised to `DailyTrendCalculatorImpl` and its interface `DailyTrendCalculator`.
The signature of `calculateDailyTrend` needs a `startingBalance: Amount` parameter.
The calling service `GetDailyTrendStatsService` must compute this starting balance before
delegating to the calculator. No new entity, value object, or port is required.

The `DailyTrend` domain model does **not** need to change: `cumulativeBalance` already carries the
right semantic once seeded correctly.

### Infrastructure Layer

> Not applicable — the proposed change is entirely within the domain. No new table, column, index,
> or SPI port is needed. The existing `findBookletByIdWithTransactions` and `findBookletsForUser`
> already load all transactions; no extra query is required.

### Application Layer

> Not applicable — the controller, DTO, and HTTP contract are unchanged. The endpoint contract
> (`startDate`, `endDate`, `bookletId`) remains identical. The response shape (`DailyTrendStatsDTO`)
> is unchanged; only the values of `cumulativeBalance` entries become correct.

### Client Layer

> Not applicable — the frontend already maps `cumulativeBalance` into the "Solde cumulé" series.
> Once the backend returns the correctly seeded values, the chart will display the true account
> balance without any frontend change.

### Cross-Cutting Concerns

**Performance**: All transactions are already loaded in memory by the existing repository call.
The starting balance computation is a simple in-memory fold — O(n) on the total transaction count,
which is negligible.

**Security**: No new surface is exposed. The existing booklet ownership check in `withScopedBooklets`
already enforces user isolation.

**Backward compatibility**: The response schema does not change. Consumers of `cumulativeBalance`
will receive different values (the corrected ones), but no contract is broken. This is a semantic
fix, not a schema change.

**Testability**: The domain change is fully unit-testable. `DailyTrendCalculator` is an interface
with a pure implementation; the test simply seeds a non-zero `startingBalance` and verifies that
the output `cumulativeBalance` values are offset accordingly.

---

## 4. Solution Approaches

### Approach A — In-domain starting balance computation

Add a `startingBalance: Amount` parameter to `DailyTrendCalculator.calculateDailyTrend()`.
In `GetDailyTrendStatsService.handle()`, compute the starting balance from the loaded booklets
before calling the calculator:

- For a scoped booklet: `initialSold + sum(non-preview transactions with date < startDate)`
- For all booklets: sum the above for each booklet and aggregate

The balance computation is pure domain logic; no new port or infrastructure adapter is involved.

| Attribute | Detail |
|---|---|
| **Layers touched** | Domain only |
| **Pros** | Minimal blast radius. No infrastructure change. No new port. Fully testable in isolation. The existing chart, DTO, and endpoint are unchanged. |
| **Cons / Risks** | Loads all transactions in memory to compute the starting balance. For accounts with thousands of transactions this is acceptable but will not scale infinitely. The contract between `DailyTrendCalculator` and its callers changes (signature), requiring update of all call sites and tests. |
| **Fit for this project** | Excellent. The project already loads all transactions eagerly. The change is small and contained. |

---

### Approach B — New SPI port for pre-period balance

Add a dedicated method to `BookletRepository`:
`computeBalanceBeforeDate(bookletId: UUID, date: LocalDate): Amount`

The infrastructure adapter computes the balance in a single SQL aggregate query, avoiding the need
to load historical transactions into memory. `GetDailyTrendStatsService` calls this port to obtain
the starting balance, then passes it to the calculator.

| Attribute | Detail |
|---|---|
| **Layers touched** | Domain (new port method), Infrastructure (new SQL query), Domain tests (fake update) |
| **Pros** | More efficient for accounts with large transaction histories. Keeps transaction loading focused on the period only if the repository contract is later refined. Explicit domain intent — the port name communicates the concept. |
| **Cons / Risks** | More work: new port signature, new Fake implementation for domain tests, new SQL query, new infrastructure test. Over-engineered for the current transaction volumes. Transactions are already loaded in memory anyway, so the SQL call is redundant today. |
| **Fit for this project** | Premature at current scale. Worth reconsidering if transaction counts per booklet grow significantly (> 10k rows). |

---

### Approach C — Frontend-only offset

Compute the balance offset client-side by reading the booklet's current `amount` from the already-
available booklet data, then adding it back-shifted by the period's net delta.

| Attribute | Detail |
|---|---|
| **Layers touched** | Frontend only |
| **Pros** | No backend change. Fastest to ship. |
| **Cons / Risks** | Incorrect by design. The booklet `amount` is the CURRENT balance, not the balance at `startDate`. The offset would only be accurate if the period is the most recent and no future transactions exist. Produces misleading charts for historical periods. |
| **Fit for this project** | Not recommended. Trading correctness for speed is not acceptable for financial data. |

---

## 5. Recommended Approach

**Approach A** is the right choice.

The root cause is a single line in `DailyTrendCalculatorImpl` (`var cumulativeBalance = BigDecimal.ZERO`).
The fix is to receive the starting balance as a parameter and seed the accumulator with it. The
entire change stays within the domain layer. No new infrastructure adapter, no new SQL query, no
frontend change.

The key trade-off is in-memory transaction loading — but this is already the case today, and
Approach B's SQL query would be redundant on top of the existing full load. Approach B should be
reconsidered only if the booklet loading contract changes to load transactions per-period rather
than all at once.

The chart on the dashboard will display the correct absolute account balance per day as soon as the
domain fix is in place, since the frontend already correctly renders `cumulativeBalance`.

---

## 6. Open Questions

1. **Multi-booklet mode**: when no `bookletId` is scoped and all booklets are aggregated, should
   `cumulativeBalance` represent the sum of all booklet balances, or should the chart be split per
   booklet? Currently both input and output are aggregated. This question is pre-existing and
   independent of the fix proposed here, but worth clarifying before implementation.

2. **Preview transactions**: `DailyTrendCalculatorImpl` already filters `!transaction.isPreview`
   when computing daily values. Should the starting balance computation also exclude preview
   transactions? The consistent answer is yes, but this should be confirmed.

---

## 7. Next Steps

- The scope is well-defined: create a feature issue scoped to the **domain module** only (→ `create-issue`).
- No technical report is needed: the approach involves no cross-cutting infrastructure concern.
- No refactoring plan is needed: the change is additive and localised.
