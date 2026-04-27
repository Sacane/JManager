# Use Case Interface Segregation – Domain: Regular Transaction Category

**Context**
The `RegularTransactionFeature` sealed interface in `domain/port/api` currently exposes 8 methods as a single contract.
Following the use-case interface segregation refactor, each method must be extracted into its own single-method interface
located in `domain/port/input`, and each implementation class must implement exactly one interface.

The `RegularTransactionGenerator` use case class in `domain/usecase` also lacks a dedicated input interface and must
be given one as part of this issue.

Methods to decompose:
- `getAllRegularTransactions(token, pageNumber, pageSize): Result<Page<RegularTransaction>>`
- `bookRegularTransaction(token, regularTransaction, bookletIds): Result<RegularTransaction>`
- `getRegularTransactionById(token, transactionId): Result<RegularTransaction>`
- `updateRegularTransaction(token, regularTransaction, bookletIds): Result<RegularTransaction>`
- `deleteRegularTransaction(token, transactionId): Result<Boolean>`
- `deleteRegularTransactions(token, transactionIds): Result<List<String>>`
- `linkRegularTransactionToBooklet(token, transactionId, bookletId): Result<RegularTransaction>`
- `unlinkRegularTransactionFromBooklet(token, transactionId, bookletId): Result<RegularTransaction>`

Use case class requiring a new interface:
- `RegularTransactionGenerator` → `GenerateRegularTransactionsUseCase`

**Acceptance Criteria**
Feature: Use case interface segregation for Regular Transaction domain operations

    In order to enforce single-responsibility and explicit domain boundaries
    As a domain maintainer
    I want each regular transaction operation to be represented by a dedicated single-method interface

    Scenario: 1 - Extract GetAllRegularTransactionsUseCase
    Given the `getAllRegularTransactions` method on RegularTransactionFeature
    When the interface segregation is applied
    Then a `GetAllRegularTransactionsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original (including default pagination parameters)
    And a dedicated implementation class implements this interface

    Scenario: 2 - Extract BookRegularTransactionUseCase
    Given the `bookRegularTransaction` method on RegularTransactionFeature
    When the interface segregation is applied
    Then a `BookRegularTransactionUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 3 - Extract GetRegularTransactionByIdUseCase
    Given the `getRegularTransactionById` method on RegularTransactionFeature
    When the interface segregation is applied
    Then a `GetRegularTransactionByIdUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 4 - Extract UpdateRegularTransactionUseCase
    Given the `updateRegularTransaction` method on RegularTransactionFeature
    When the interface segregation is applied
    Then an `UpdateRegularTransactionUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 5 - Extract DeleteRegularTransactionUseCase
    Given the `deleteRegularTransaction` method on RegularTransactionFeature (single id)
    When the interface segregation is applied
    Then a `DeleteRegularTransactionUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 6 - Extract DeleteRegularTransactionsUseCase
    Given the `deleteRegularTransactions` method on RegularTransactionFeature (bulk)
    When the interface segregation is applied
    Then a `DeleteRegularTransactionsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 7 - Extract LinkRegularTransactionToBookletUseCase
    Given the `linkRegularTransactionToBooklet` method on RegularTransactionFeature
    When the interface segregation is applied
    Then a `LinkRegularTransactionToBookletUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 8 - Extract UnlinkRegularTransactionFromBookletUseCase
    Given the `unlinkRegularTransactionFromBooklet` method on RegularTransactionFeature
    When the interface segregation is applied
    Then an `UnlinkRegularTransactionFromBookletUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 9 - Introduce GenerateRegularTransactionsUseCase interface for RegularTransactionGenerator
    Given the `RegularTransactionGenerator` class in `domain/usecase` currently lacking a dedicated interface
    When the interface segregation is applied
    Then a `GenerateRegularTransactionsUseCase` interface is introduced
    And `RegularTransactionGenerator` implements it
    And the interface is located consistently with the other input interfaces

    Scenario: 10 - Preserve regular transaction domain behavior
    Given existing regular transaction domain business rules and tests
    When all use case interfaces have been created and implemented
    Then all existing domain tests for regular transactions remain green
    And no regular transaction domain model or output-port signature is modified
    And the application layer can resolve each use case independently via dependency injection

**Notes**
- All new interfaces must be located in `domain/port/input`, under a `regulartransaction` sub-package for clarity.
- Strict signature parity is required: no parameter reorder, no type change, no return type change.
- Default parameter values on `getAllRegularTransactions` (pageNumber, pageSize) must be preserved.
- The existing `RegularTransactionFeature` sealed interface may be kept temporarily for migration or removed once all consumers are updated.
- This issue covers domain layer only; application layer wiring is tracked in a separate issue.
