# Use Case Interface Segregation – Domain: Stats Category

**Context**
The `StatsFeature` sealed interface in `domain/port/api` currently exposes 5 methods as a single contract.
Following the use-case interface segregation refactor, each method must be extracted into its own single-method interface
located in `domain/port/input`, and each implementation class must implement exactly one interface.

The following use case classes in `domain/usecase` also lack dedicated input interfaces and must be given one as part of this issue:
- `CategoryDistributionCalculator` → `CalculateCategoryDistributionUseCase`
- `DailyTrendCalculator` → `CalculateDailyTrendUseCase`
- `MonthlyStatsCalculator` → `CalculateMonthlyStatsUseCase`
- `TrendCalculator` → `CalculateTrendUseCase`

Methods to decompose:
- `getMonthlyBookletStats(bookletId, year, token): Result<MonthlyBookletStatsOutput>`
- `getCategoryDistribution(token, bookletId, startDate, endDate): Result<CategoryDistributionOutput>`
- `getTrendStats(token, bookletId, startDate, endDate): Result<TrendStatsOutput>`
- `getPrevisionalTransactions(token, startDate, endDate, bookletId): Result<PrevisionalTransactionsOutput>`
- `getDailyTrendStats(token, startDate, endDate, bookletId): Result<DailyTrendStatsOutput>`

**Acceptance Criteria**
Feature: Use case interface segregation for Stats domain operations

    In order to enforce single-responsibility and explicit domain boundaries
    As a domain maintainer
    I want each statistics operation to be represented by a dedicated single-method interface

    Scenario: 1 - Extract GetMonthlyBookletStatsUseCase
    Given the `getMonthlyBookletStats` method on StatsFeature
    When the interface segregation is applied
    Then a `GetMonthlyBookletStatsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 2 - Extract GetCategoryDistributionUseCase
    Given the `getCategoryDistribution` method on StatsFeature
    When the interface segregation is applied
    Then a `GetCategoryDistributionUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original (including optional parameters)
    And a dedicated implementation class implements this interface

    Scenario: 3 - Extract GetTrendStatsUseCase
    Given the `getTrendStats` method on StatsFeature
    When the interface segregation is applied
    Then a `GetTrendStatsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original (including optional parameters)
    And a dedicated implementation class implements this interface

    Scenario: 4 - Extract GetPrevisionalTransactionsUseCase
    Given the `getPrevisionalTransactions` method on StatsFeature
    When the interface segregation is applied
    Then a `GetPrevisionalTransactionsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 5 - Extract GetDailyTrendStatsUseCase
    Given the `getDailyTrendStats` method on StatsFeature
    When the interface segregation is applied
    Then a `GetDailyTrendStatsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 6 - Introduce interfaces for internal stats calculator use cases
    Given the use case classes CategoryDistributionCalculator, DailyTrendCalculator, MonthlyStatsCalculator, TrendCalculator
    Currently lacking dedicated input interfaces
    When the interface segregation is applied
    Then a dedicated single-method interface is introduced for each calculator class
    And each class is updated to implement its matching interface
    And the interfaces are located consistently with the other input interfaces

    Scenario: 7 - Preserve stats domain behavior
    Given existing stats domain business rules and tests
    When all use case interfaces have been created and implemented
    Then all existing domain tests for statistics remain green
    And no stats domain model or output-port signature is modified
    And the application layer can resolve each use case independently via dependency injection

**Notes**
- All new interfaces must be located in `domain/port/input`, under a `stats` sub-package for clarity.
- Strict signature parity is required: no parameter reorder, no type change, no return type change.
- Optional parameters (`bookletId`, `startDate`, `endDate`) must be preserved with their default values.
- The existing `StatsFeature` sealed interface may be kept temporarily for migration or removed once all consumers are updated.
- This issue covers domain layer only; application layer wiring is tracked in a separate issue.
