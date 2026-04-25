# Refactor Feature Ports to One Use Case Interface per Method: Domain Module Impact

**Context**
The current domain API contracts expose large feature interfaces with many methods (e.g., transaction, booklet, tag, user features). This makes boundaries less explicit and increases coupling between unrelated use cases. The target architecture is one input interface per use case, with a single method (`execute(...)` or `invoke(...)`), while preserving business behavior and existing method signatures.

This refactor must also cover existing use case classes that currently do not have dedicated use case interfaces.

**Acceptance Criteria**
Feature: Domain use case interface decomposition (CQRS / use-case style)
    In order to enforce clean hexagonal boundaries and explicit use cases
    As a domain consumer
    I want each domain input operation to be represented by one dedicated interface with one method

    Scenario: 1 - Split each feature method into its own input use case interface
    Given a domain feature interface that currently exposes multiple methods
    When the refactor is applied in the domain input ports
    Then each method is extracted into a dedicated interface in domain/port/input
    And each new interface contains exactly one method named execute(...) or invoke(...)
    And the method parameters and return type are identical to the original method signature

    Scenario: 2 - Split feature implementation classes into one implementation per use case
    Given a domain implementation class that currently implements a multi-method feature interface
    When the refactor is applied
    Then one implementation class is created per use case interface
    And each implementation class implements exactly one use case interface
    And each implementation class is colocated with its corresponding interface file

    Scenario: 3 - Preserve domain behavior and output-port contracts
    Given existing domain business rules and output ports
    When the use case decomposition refactor is completed
    Then domain behavior remains unchanged
    And no domain model structure is modified
    And no output port signature is changed

    Scenario: 4 - Add missing interfaces for existing use case classes
    Given domain classes currently used as use cases without dedicated input interfaces
    When the refactor is applied
    Then dedicated one-method use case interfaces are introduced for these classes
    And these classes are updated to implement their matching interfaces

    Scenario: 5 - Keep domain tests green with use case-level coverage
    Given existing domain test suites
    When the domain refactor is completed
    Then all existing domain tests pass
    And tests are added or updated to verify one-method-per-interface mapping for impacted use cases

**Notes**
- Scope includes decomposition of current multi-method domain input interfaces (for example current feature interfaces under domain/port/api) into one interface per operation.
- Preferred target location for input interfaces is domain/port/input (or application/usecase if agreed by architecture rules), but the same convention must be applied consistently.
- Keep operation names explicit (for example GetTransactionByIdUseCase, SaveBookletUseCase, DeleteTagUseCase).
- Signature parity is strict: no parameter reorder, no type change, no return type change.
- This issue does not include output-port decomposition.
