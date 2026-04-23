# Daily Financial Evolution Chart: Client Module

**Contexte**
The dashboard "Évolution des finances" chart currently displays a multi-month overview (6 months for monthly view) using the `TrendStats` endpoint which returns monthly granularity data. This makes it impossible to see how finances evolve day by day within a single month.

The chart must be reworked so that, when the selected period is "month", it fetches and displays **daily** trend data from the new `GET /api/stats/daily-trends` endpoint instead of the multi-month `GET /api/stats/trends` view. The chart must pass the correct `startDate` and `endDate` derived from the user's custom monthly cycle configuration (e.g., start day 26 to end day 25), so the daily evolution matches the user's configured period boundaries.

For "quarter" and "year" periods, the existing monthly granularity view should be preserved unchanged.

**Critères d'acceptation**
Feature: Daily financial evolution chart on dashboard
    In order to clearly track my day-by-day financial evolution within a month
    As an authenticated user
    I want the evolution chart to display daily data points when viewing a monthly period

Scenario: 1 - Chart shows daily evolution for the current month
    Given a user viewing the dashboard with period set to "month"
    When the chart data loads
    Then the "Évolution des finances" chart displays one data point per day
    And the X axis labels show day numbers or short date labels (e.g. "1", "2", ... "31" or "01 Jan", "02 Jan")
    And two datasets are shown: "Dépenses" (expenses) and "Revenus" (income)

Scenario: 2 - Chart respects custom monthly cycle boundaries
    Given a user whose booklet has a monthly cycle configured as startDay=26 and endDay=25
    And the user is viewing the dashboard with period "month" for January 2025
    When the chart data loads
    Then the API is called with startDate=2024-12-26 and endDate=2025-01-25
    And the chart displays daily data points from December 26 to January 25

Scenario: 3 - Chart shows daily evolution for a past month
    Given a user who navigated to a previous month using the period navigation arrows
    When the chart data loads
    Then the chart displays daily evolution for the selected past month's custom cycle range

Scenario: 4 - Chart falls back to monthly granularity for quarter period
    Given a user viewing the dashboard with period set to "quarter"
    When the chart data loads
    Then the chart displays monthly data points (existing behavior, unchanged)

Scenario: 5 - Chart falls back to monthly granularity for year period
    Given a user viewing the dashboard with period set to "year"
    When the chart data loads
    Then the chart displays monthly data points (existing behavior, unchanged)

Scenario: 6 - Chart handles a month with no transactions
    Given a user viewing a month with zero transactions
    When the chart data loads
    Then the chart displays a flat line at zero for both datasets
    And no error is shown

Scenario: 7 - Chart shows cumulative balance line
    Given a user viewing the dashboard with period "month" and transactions in the period
    When the chart data loads
    Then an additional "Solde cumulé" dataset is shown as a line
    And it represents the running cumulative balance day by day

Scenario: 8 - Chart displays cross-month custom cycle (25th to 24th) for June
    Given a user whose booklet has a monthly cycle configured as startDay=25 and endDay=24
    And the user is viewing the dashboard with period "month" anchored in June 2025
    When the chart data loads
    Then the API is called with startDate=2025-05-25 and endDate=2025-06-24
    And the chart displays daily data points from May 25 to June 24
    And the X axis labels span across both months (e.g. "25 Mai", "26 Mai", ... "23 Juin", "24 Juin")

**Notes**
- A new `getDailyTrendStats` function must be added to `useStats.ts` composable.
- A new `DailyTrendStatsDTO` and `DailyTrendDTO` type must be added to `types/index.d.ts`.
- The `loadStatsData` function must call the daily endpoint when `selectedPeriod === 'month'` and the monthly endpoint otherwise.
- The `expensesTrendData` computed must branch on the selected period: daily data for "month", monthly data for "quarter"/"year".
- The existing `evolutionTrendStats` ref can be kept for quarter/year; a new `dailyTrendStats` ref is needed for the monthly daily view.
- The `currentDateRange` computed already resolves custom cycle boundaries — use `currentDateRange.start` and `currentDateRange.end` as the API parameters.
