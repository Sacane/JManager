# Use Case Interface Segregation – Domain: Booklet Category

**Context**
The `BookletFeature` sealed interface in `domain/port/api` currently exposes 9 methods as a single contract.
Following the use-case interface segregation refactor, each method must be extracted into its own single-method interface
located in `domain/port/input`, and each implementation class must implement exactly one interface.

The `PrevisionalTransactionFilter` use case class in `domain/usecase` also lacks a dedicated input interface and must
be given one as part of this issue.

Methods to decompose:
- `findBookletById(bookletId, token): Result<Booklet>`
- `editBooklet(booklet, token): Result<Booklet>`
- `deleteBookletById(bookletId, token): Result<Nothing>`
- `findByLabelAndUserId(token, label): Result<Booklet>`
- `findAllRegisteredBooklets(token): Result<List<Booklet>>`
- `save(token, booklet): Result<Booklet>`
- `loadTransactionsForBookletForAMonth(token, bookletId, month, year, ...): Result<BookletLoadingResult>`
- `loadBalancesForBookletForAMonth(token, bookletId, month, year, ...): Result<BookletBalances>`
- `regenerateDeletedPrevisionalTransactions(token, bookletId, month, year): Result<List<Transaction>>`

Use case class requiring a new interface:
- `PrevisionalTransactionFilter` → `FilterPrevisionalTransactionsUseCase`

**Acceptance Criteria**
Feature: Use case interface segregation for Booklet domain operations

    In order to enforce single-responsibility and explicit domain boundaries
    As a domain maintainer
    I want each booklet operation to be represented by a dedicated single-method interface

    Scenario: 1 - Extract FindBookletByIdUseCase
    Given the `findBookletById` method on BookletFeature
    When the interface segregation is applied
    Then a `FindBookletByIdUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 2 - Extract EditBookletUseCase
    Given the `editBooklet` method on BookletFeature
    When the interface segregation is applied
    Then an `EditBookletUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 3 - Extract DeleteBookletByIdUseCase
    Given the `deleteBookletById` method on BookletFeature
    When the interface segregation is applied
    Then a `DeleteBookletByIdUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 4 - Extract FindBookletByLabelAndUserIdUseCase
    Given the `findByLabelAndUserId` method on BookletFeature
    When the interface segregation is applied
    Then a `FindBookletByLabelAndUserIdUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 5 - Extract FindAllRegisteredBookletsUseCase
    Given the `findAllRegisteredBooklets` method on BookletFeature
    When the interface segregation is applied
    Then a `FindAllRegisteredBookletsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 6 - Extract SaveBookletUseCase
    Given the `save` method on BookletFeature
    When the interface segregation is applied
    Then a `SaveBookletUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 7 - Extract LoadTransactionsForBookletForAMonthUseCase
    Given the `loadTransactionsForBookletForAMonth` method on BookletFeature
    When the interface segregation is applied
    Then a `LoadTransactionsForBookletForAMonthUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original (all optional parameters included)
    And a dedicated implementation class implements this interface

    Scenario: 8 - Extract LoadBalancesForBookletForAMonthUseCase
    Given the `loadBalancesForBookletForAMonth` method on BookletFeature
    When the interface segregation is applied
    Then a `LoadBalancesForBookletForAMonthUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 9 - Extract RegenerateDeletedPrevisionalTransactionsUseCase
    Given the `regenerateDeletedPrevisionalTransactions` method on BookletFeature
    When the interface segregation is applied
    Then a `RegenerateDeletedPrevisionalTransactionsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 10 - Introduce FilterPrevisionalTransactionsUseCase interface for PrevisionalTransactionFilter
    Given the `PrevisionalTransactionFilter` class in `domain/usecase` currently lacking a dedicated interface
    When the interface segregation is applied
    Then a `FilterPrevisionalTransactionsUseCase` interface is introduced
    And `PrevisionalTransactionFilter` implements it
    And the interface is located consistently with the other input interfaces

    Scenario: 11 - Preserve booklet domain behavior
    Given existing booklet domain business rules and tests
    When all use case interfaces have been created and implemented
    Then all existing domain tests for booklets remain green
    And no booklet domain model or output-port signature is modified
    And the application layer can resolve each use case independently via dependency injection

**Notes**
- All new interfaces must be located in `domain/port/input`, under a `booklet` sub-package for clarity.
- Strict signature parity is required: no parameter reorder, no type change, no return type change.
- The `loadTransactionsForBookletForAMonth` and `loadBalancesForBookletForAMonth` methods carry multiple optional parameters — all defaults must be preserved in the interface.
- The existing `BookletFeature` sealed interface may be kept temporarily for migration or removed once all consumers are updated.
- This issue covers domain layer only; application layer wiring is tracked in a separate issue.
