# Refactor Controller Dependencies to Dedicated Use Case Interfaces: Application Module Impact

**Context**
After splitting domain input contracts into one interface per use case, application entry points (controllers and related orchestration) must depend on dedicated use case interfaces instead of large feature interfaces. This change must preserve API behavior and response semantics while aligning the application layer with the use case driven architecture.

**Acceptance Criteria**
Feature: Application wiring aligned with one-use-case-per-interface contracts
    In order to keep the application layer aligned with clean hexagonal architecture
    As an API consumer
    I want application endpoints to keep the same behavior while delegating to dedicated use case interfaces

    Scenario: 1 - Controllers use dedicated one-method input interfaces
    Given a controller that currently depends on a multi-method feature interface
    When application dependencies are refactored
    Then the controller depends only on the dedicated use case interfaces required by each endpoint
    And each delegated interface exposes exactly one execute(...) or invoke(...) method

    Scenario: 2 - Endpoint behavior remains unchanged
    Given existing endpoint contracts and expected behavior
    When the controller delegates to decomposed use case interfaces
    Then each endpoint returns the same HTTP status codes and payload structure as before
    And no endpoint path or request contract is modified by this refactor

    Scenario: 3 - Use case implementations remain discoverable by dependency injection
    Given one implementation class per use case in the domain module
    When the application module starts
    Then dependency injection resolves all required use case interfaces
    And all controllers start without missing-bean errors

    Scenario: 4 - Application tests validate non-regression after wiring change
    Given existing application tests for impacted controllers
    When the refactor is completed
    Then all existing tests pass
    And additional tests are added or updated for endpoint-level non-regression where needed

**Notes**
- The application layer must not re-aggregate decomposed use cases into new large adapter interfaces.
- Constructor dependencies should include only use cases actually used by each controller endpoint.
- This issue focuses on API-to-use-case wiring and endpoint non-regression; domain behavior changes are out of scope.
