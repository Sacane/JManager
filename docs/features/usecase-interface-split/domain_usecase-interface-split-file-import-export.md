# Use Case Interface Segregation – Domain: File Import/Export Category

**Context**
The `FileImportExportFeature` sealed interface in `domain/port/api` currently exposes 3 methods as a single contract.
Following the use-case interface segregation refactor, each method must be extracted into its own single-method interface
located in `domain/port/input`, and each implementation class must implement exactly one interface.

The use case classes in `domain/usecase/csv` (`CsvTransactionValidator`, `CsvFileValidator`, `CsvTransactionExporter`)
also lack dedicated input interfaces and must be given one as part of this issue.

Methods to decompose:
- `validateCsvFile(token, bookletId, csvContent, month, year): Result<CsvValidationReport>`
- `importTransactionsFromCsv(token, bookletId, csvContent, skipValidation, month, year): Result<CsvImportResult>`
- `exportTransactionsToCsv(token, transactions): Result<String>`

Use case classes requiring new interfaces:
- `CsvTransactionValidator` → `ValidateCsvTransactionUseCase`
- `CsvFileValidator` → `ValidateCsvFileContentUseCase`
- `CsvTransactionExporter` → `ExportCsvTransactionUseCase`

**Acceptance Criteria**
Feature: Use case interface segregation for File Import/Export domain operations

    In order to enforce single-responsibility and explicit domain boundaries
    As a domain maintainer
    I want each file import/export operation to be represented by a dedicated single-method interface

    Scenario: 1 - Extract ValidateCsvFileUseCase
    Given the `validateCsvFile` method on FileImportExportFeature
    When the interface segregation is applied
    Then a `ValidateCsvFileUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original (including optional month/year parameters)
    And a dedicated implementation class implements this interface

    Scenario: 2 - Extract ImportTransactionsFromCsvUseCase
    Given the `importTransactionsFromCsv` method on FileImportExportFeature
    When the interface segregation is applied
    Then an `ImportTransactionsFromCsvUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original (including `skipValidation: Boolean = false`)
    And a dedicated implementation class implements this interface

    Scenario: 3 - Extract ExportTransactionsToCsvUseCase
    Given the `exportTransactionsToCsv` method on FileImportExportFeature
    When the interface segregation is applied
    Then an `ExportTransactionsToCsvUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 4 - Introduce interfaces for internal CSV use case classes
    Given the use case classes CsvTransactionValidator, CsvFileValidator, CsvTransactionExporter in `domain/usecase/csv`
    Currently lacking dedicated input interfaces
    When the interface segregation is applied
    Then a dedicated single-method interface is introduced for each class
    And each class is updated to implement its matching interface
    And the interfaces are located consistently with the other input interfaces

    Scenario: 5 - Preserve file import/export domain behavior
    Given existing domain business rules for CSV import (validation, line parsing, error handling)
    When all use case interfaces have been created and implemented
    Then all existing domain tests for file import/export remain green
    And no domain model, CSV validation logic, or output-port signature is modified
    And the application layer can resolve each use case independently via dependency injection

**Notes**
- All new interfaces must be located in `domain/port/input`, under a `csv` or `fileimportexport` sub-package for clarity.
- Strict signature parity is required: no parameter reorder, no type change, no return type change.
- Default parameter values (`skipValidation = false`, optional `month` and `year`) must be preserved in the interfaces.
- The existing `FileImportExportFeature` sealed interface may be kept temporarily for migration or removed once all consumers are updated.
- This issue covers domain layer only; application layer wiring is tracked in a separate issue.
