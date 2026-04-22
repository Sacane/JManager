# Daily Financial Trend Calculation: Domain Module

**Contexte**
The current `TrendCalculator` aggregates financial data at **monthly** granularity only (`MonthlyTrend`). The dashboard "Évolution des finances" chart therefore shows a multi-month view, making it impossible to visualize the day-by-day financial evolution within a single period (month).

Users need to track how their balance, income and expenses evolve **day by day** inside a given period. Additionally, the period boundaries must respect the user's custom monthly cycle settings (e.g. a cycle starting on day 26 and ending on day 25 of the next month), so the daily evolution must work with arbitrary `startDate`/`endDate` ranges, not just calendar months.

A new `DailyTrendCalculator` use case and a new `DailyTrend` domain model are required. The existing `StatsFeature` port must expose a new method to retrieve daily trend data.

**Critères d'acceptation**
Feature: Daily financial trend calculation
    In order to visualize day-by-day financial evolution within a period
    As an authenticated user
    I want the system to compute daily trend data for a given date range and optional booklet

Scenario: 1 - Calculate daily trends for a standard calendar month
    Given an authenticated user with transactions on January 5, January 12, and January 20
    When requesting daily trends from 2025-01-01 to 2025-01-31
    Then the system returns a DailyTrend entry for each day of the range (31 entries)
    And each entry contains the day's income, expenses, balance, and cumulative balance
    And days without transactions have zero income and zero expenses

Scenario: 2 - Calculate daily trends for a custom monthly cycle
    Given an authenticated user with transactions on December 28 and January 15
    When requesting daily trends from 2024-12-26 to 2025-01-25
    Then the system returns a DailyTrend entry for each day of the custom cycle
    And transactions on December 28 and January 15 are included in the result
    And cumulative balance is computed sequentially across the full custom range

Scenario: 3 - Calculate daily trends scoped to a specific booklet
    Given an authenticated user with two booklets, each having transactions
    When requesting daily trends for a date range filtered by one booklet ID
    Then only transactions belonging to the specified booklet are included
    And totalBooklets equals 1

Scenario: 4 - Cumulative balance builds correctly over the period
    Given an authenticated user with an income of 1000 on day 1 and an expense of 300 on day 10
    When requesting daily trends from day 1 to day 30
    Then the cumulative balance on day 1 is 1000
    And the cumulative balance on day 10 is 700
    And the cumulative balance on day 30 is 700

Scenario: 5 - Daily trends exclude preview transactions
    Given an authenticated user with a real transaction and a preview transaction on the same day
    When requesting daily trends for that day's range
    Then only the real transaction is counted in the daily trend

Scenario: 6 - Requesting daily trends with a partial date range fails
    Given an authenticated user
    When requesting daily trends with a startDate but no endDate
    Then the system returns a validation failure

Scenario: 7 - Requesting daily trends with no transactions returns zero-valued entries
    Given an authenticated user with no transactions in the requested range
    When requesting daily trends from 2025-03-01 to 2025-03-31
    Then the system returns 31 entries, all with zero income, expenses, and balance

Scenario: 8 - Calculate daily trends for a cross-month custom cycle (25th to 24th)
    Given an authenticated user with a booklet configured with startDay=25 and endDay=24
    And the user has transactions on May 27, June 3, and June 18
    When requesting daily trends from 2025-05-25 to 2025-06-24
    Then the system returns a DailyTrend entry for each day from May 25 to June 24 (31 entries)
    And the transactions on May 27, June 3, and June 18 are included in the result
    And days outside this range are excluded
    And cumulative balance is computed sequentially starting from May 25

**Notes**
- The new `DailyTrend` model should contain: `date: LocalDate`, `income: Amount`, `expenses: Amount`, `balance: Amount`, `cumulativeBalance: Amount`, `totalBooklets: Int`.
- The new `DailyTrendCalculator` use case must accept `booklets: List<Booklet>`, `startDate: LocalDate`, `endDate: LocalDate` (both required).
- The `StatsFeature` port must expose a new `getDailyTrendStats(token, bookletId?, startDate, endDate): Result<DailyTrendStatsOutput>` method.
- Preview transactions (`isPreview = true`) must be excluded from the calculation, consistent with `TrendCalculator`.
