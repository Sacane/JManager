# Use Case Interface Segregation – Domain: Transaction Category

**Context**
The `TransactionFeature` sealed interface in `domain/port/api` currently exposes 6 methods as a single contract.
Following the use-case interface segregation refactor, each method must be extracted into its own single-method interface
located in `domain/port/input`, and each implementation class must implement exactly one interface.

Methods to decompose:
- `bookTransaction(token, bookletLabel, transaction): Result<TransactionResumeResult>`
- `retrieveTransactionsByMonthAndYear(token, month, year, bookletLabel): Result<List<Transaction>>`
- `editTransaction(bookletID, transaction, token): Result<TransactionResumeResult>`
- `findById(id, token): Result<Transaction>`
- `deleteTransactionsByIds(bookletID, transactionIds, token): Result<TransactionDeletionResult>`
- `confirmPreviewTransaction(token, bookletID, transactionId, newAmount, newDate): Result<TransactionResumeResult>`

**Acceptance Criteria**
Feature: Use case interface segregation for Transaction domain operations

    In order to enforce single-responsibility and explicit domain boundaries
    As a domain maintainer
    I want each transaction operation to be represented by a dedicated single-method interface

    Scenario: 1 - Extract BookTransactionUseCase
    Given the `bookTransaction` method on TransactionFeature
    When the interface segregation is applied
    Then a `BookTransactionUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a `BookTransactionService` class implements `BookTransactionUseCase`
    And the original `TransactionFeatureImpl` no longer implements this concern

    Scenario: 2 - Extract RetrieveTransactionsByMonthAndYearUseCase
    Given the `retrieveTransactionsByMonthAndYear` method on TransactionFeature
    When the interface segregation is applied
    Then a `RetrieveTransactionsByMonthAndYearUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 3 - Extract EditTransactionUseCase
    Given the `editTransaction` method on TransactionFeature
    When the interface segregation is applied
    Then an `EditTransactionUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 4 - Extract FindTransactionByIdUseCase
    Given the `findById` method on TransactionFeature
    When the interface segregation is applied
    Then a `FindTransactionByIdUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 5 - Extract DeleteTransactionsByIdsUseCase
    Given the `deleteTransactionsByIds` method on TransactionFeature
    When the interface segregation is applied
    Then a `DeleteTransactionsByIdsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 6 - Extract ConfirmPreviewTransactionUseCase
    Given the `confirmPreviewTransaction` method on TransactionFeature
    When the interface segregation is applied
    Then a `ConfirmPreviewTransactionUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 7 - Preserve transaction domain behavior
    Given existing transaction domain business rules and tests
    When all 6 use case interfaces have been created and implemented
    Then all existing domain tests for transactions remain green
    And no transaction domain model or output-port signature is modified
    And the application layer can resolve each use case independently via dependency injection

**Notes**
- All new interfaces must be located in `domain/port/input`, under a `transaction` sub-package for clarity.
- Strict signature parity is required: no parameter reorder, no type change, no return type change.
- The existing `TransactionFeature` sealed interface may be kept temporarily for migration or removed once all consumers are updated.
- This issue covers domain layer only; application layer wiring is tracked in a separate issue.
