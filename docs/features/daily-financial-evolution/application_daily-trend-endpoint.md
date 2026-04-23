# Daily Financial Trend API Endpoint: Application Module

**Contexte**
The domain module introduces a new `getDailyTrendStats` method on the `StatsFeature` port, returning day-by-day financial evolution data (`DailyTrendStatsOutput`). The application layer must expose this through a new REST endpoint so the frontend dashboard can fetch daily granularity data for the "Évolution des finances" chart.

A new `DailyTrendDTO` and corresponding mapping functions are required, along with a new `GET /api/stats/daily-trends` endpoint accepting `startDate`, `endDate`, and an optional `bookletId`.

**Critères d'acceptation**
Feature: Daily trend stats REST endpoint
    In order to display a day-by-day financial evolution chart
    As an authenticated API consumer
    I want to call an endpoint that returns daily trend data for a given period

Scenario: 1 - Successfully retrieve daily trend stats
    Given an authenticated user with transactions in January 2025
    When calling GET /api/stats/daily-trends?startDate=2025-01-01&endDate=2025-01-31
    Then the response status is 200
    And the body contains a dailyTrends array with one entry per day

Scenario: 2 - Retrieve daily trend stats scoped to a booklet
    Given an authenticated user with two booklets
    When calling GET /api/stats/daily-trends?startDate=2025-01-01&endDate=2025-01-31&bookletId={uuid}
    Then the response status is 200
    And the dailyTrends data reflects only the specified booklet's transactions

Scenario: 3 - Retrieve daily trends for a custom cycle period
    Given an authenticated user with transactions in a custom cycle (26th to 25th)
    When calling GET /api/stats/daily-trends?startDate=2024-12-26&endDate=2025-01-25
    Then the response status is 200
    And the dailyTrends array covers the full custom cycle range

Scenario: 4 - Missing required date parameters returns 400
    Given an authenticated user
    When calling GET /api/stats/daily-trends without startDate or endDate
    Then the response status is 400

Scenario: 5 - Unauthenticated user returns 401
    Given a user with an invalid or expired session token
    When calling GET /api/stats/daily-trends?startDate=2025-01-01&endDate=2025-01-31
    Then the response status is 401

Scenario: 6 - No transactions in range returns 200 with empty trends
    Given an authenticated user with no transactions in the specified range
    When calling GET /api/stats/daily-trends?startDate=2025-06-01&endDate=2025-06-30
    Then the response status is 200
    And the dailyTrends array contains zero-valued entries for each day

**Notes**
- The `DailyTrendDTO` should contain: `date: LocalDate`, `income: String`, `expenses: String`, `balance: String`, `cumulativeBalance: String`, `totalBooklets: Int`.
- The `DailyTrendStatsDTO` wraps a `dailyTrends: List<DailyTrendDTO>`.
- Both `startDate` and `endDate` are required query parameters (unlike the existing `/trends` endpoint where they are optional).
- Mapping function: `DailyTrendStatsOutput.toDTO()` → `DailyTrendStatsDTO`.
