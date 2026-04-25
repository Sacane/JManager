# Use Case Interface Segregation – Domain: Admin Category

**Context**
The `AdminFeature` interface in `domain/port/api` currently exposes 1 method.
Following the use-case interface segregation refactor, this method must be extracted into its own single-method interface
located in `domain/port/input`, and the implementation class must implement exactly that interface.

Methods to decompose:
- `getUsers(token, pageNumber, pageSize): Result<Page<User>>`

**Acceptance Criteria**
Feature: Use case interface segregation for Admin domain operations

    In order to enforce single-responsibility and explicit domain boundaries
    As a domain maintainer
    I want each admin operation to be represented by a dedicated single-method interface

    Scenario: 1 - Extract GetUsersUseCase
    Given the `getUsers` method on AdminFeature
    When the interface segregation is applied
    Then a `GetUsersUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original (including default pagination parameters)
    And `AdminFeatureImpl` is renamed or replaced by a dedicated `GetUsersService` class
    And this class implements `GetUsersUseCase`

    Scenario: 2 - Preserve admin domain behavior
    Given the existing admin authorization rule (caller must have admin role)
    When the GetUsersUseCase is implemented
    Then the role check behavior is preserved in the new implementation
    And all existing domain tests for admin operations remain green
    And no admin domain model or output-port signature is modified

**Notes**
- The new interface must be located in `domain/port/input`, under an `admin` sub-package for clarity.
- Strict signature parity is required: no parameter reorder, no type change, no return type change.
- Default parameter values (`pageNumber = 0`, `pageSize = 20`) must be preserved in the interface.
- The existing `AdminFeature` interface may be removed once all consumers are updated.
- This issue covers domain layer only; application layer wiring is tracked in a separate issue.
