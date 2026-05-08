# Daily Balance Curve — Domain: Seed cumulative balance from actual booklet balance at start date

**Context**
The dashboard displays a "Solde cumulé" curve on the "Évolution des finances" chart, built from the
`cumulativeBalance` field returned by `GET /stats/daily-trends`. Currently, `DailyTrendCalculatorImpl`
initialises the running total at `BigDecimal.ZERO`, so the published `cumulativeBalance` represents
the **net delta over the selected period from zero** rather than the booklet's true absolute balance.
For an account holding 5 000 € on day 1 of the period, the curve incorrectly starts at 0.

The fix is entirely contained within the domain layer. `Booklet` already carries `initialSold` and
all its transactions are loaded in memory by the existing `findBookletByIdWithTransactions` /
`findBookletsForUser` calls. The starting balance can therefore be computed as a pure in-memory fold:

```
balanceAtStartDate = initialSold
  + sum of all non-preview transactions with date < startDate
    (income transactions added, expense transactions subtracted)
```

`DailyTrendCalculator.calculateDailyTrend()` must accept a `startingBalance: Amount` parameter.
`GetDailyTrendStatsService` must compute this value before delegating to the calculator.
No new entity, port, infrastructure adapter, or database query is required.

**Acceptance Criteria**

Feature: Seed daily trend cumulative balance from actual booklet balance at start date

Scenario: Single scoped booklet with transactions before the start date
    Given an authenticated user who owns a booklet with an initialSold of 1 000 €
    And the booklet has confirmed income transactions of 500 € and expense transactions of 200 € before the start date
    And the booklet has no transactions on or after the start date
    When the user requests daily trend statistics for a given date range scoped to that booklet
    Then the first DailyTrend entry has a cumulativeBalance of 1 300 € (1 000 + 500 - 200)
    And each subsequent entry's cumulativeBalance reflects the day-by-day accumulation from that baseline

Scenario: Single scoped booklet with no transactions before the start date
    Given an authenticated user who owns a booklet with an initialSold of 2 000 €
    And the booklet has no transactions before the start date
    When the user requests daily trend statistics for a given date range scoped to that booklet
    Then the first DailyTrend entry has a cumulativeBalance equal to the booklet's initialSold (2 000 €)

Scenario: All booklets scope — no bookletId filter
    Given an authenticated user who owns two booklets
    And booklet A has a computed balance before start date of 1 000 €
    And booklet B has a computed balance before start date of 500 €
    When the user requests daily trend statistics with no bookletId filter
    Then the first DailyTrend entry has a cumulativeBalance equal to the sum of both starting balances (1 500 €)
    And each subsequent entry's cumulativeBalance accumulates income and expenses from all booklets from that baseline

Scenario: Preview transactions before the start date are excluded from the starting balance
    Given an authenticated user who owns a booklet with an initialSold of 1 000 €
    And the booklet has a confirmed income transaction of 500 € before the start date
    And the booklet has a preview income transaction of 300 € before the start date
    When the user requests daily trend statistics for a given date range scoped to that booklet
    Then the starting balance used is 1 500 € (1 000 + 500, excluding the 300 € preview)
    And the first DailyTrend entry has a cumulativeBalance of 1 500 €

Scenario: Empty period with no transactions in the requested range
    Given an authenticated user who owns a booklet with a computed balance before start date of 3 000 €
    And the booklet has no transactions in the requested date range
    When the user requests daily trend statistics for that range scoped to that booklet
    Then every DailyTrend entry has a cumulativeBalance of 3 000 €
    And income and expenses for each entry are 0

**Notes**
- Infrastructure, Application, and Client layers are not impacted by this change.
- The `DailyTrend` domain model (`cumulativeBalance` field) does not change — only the seed value changes.
- The `GET /stats/daily-trends` HTTP contract and `DailyTrendStatsDTO` response shape are unchanged.
- See investigation report: `docs/investigations/daily-balance-curve/REPORT.md` — Approach A.
- If the booklet loading contract changes in the future to load transactions per-period rather than all at once, Approach B (dedicated SPI port) should be reconsidered.
