# Use Case Interface Segregation – Domain: Tag Category

**Context**
The `TagFeature` sealed interface in `domain/port/api` currently exposes 6 methods as a single contract.
Following the use-case interface segregation refactor, each method must be extracted into its own single-method interface
located in `domain/port/input`, and each implementation class must implement exactly one interface.

Methods to decompose:
- `addTag(token, tag): Result<Tag>`
- `getAllTags(token): Result<List<Tag>>`
- `addDefaultTags()`
- `deleteTag(token, tagId, force): Result<Nothing>`
- `defaultTag(token): Result<Tag>`
- `editTag(token, tag): Result<Tag>`

**Acceptance Criteria**
Feature: Use case interface segregation for Tag domain operations

    In order to enforce single-responsibility and explicit domain boundaries
    As a domain maintainer
    I want each tag operation to be represented by a dedicated single-method interface

    Scenario: 1 - Extract AddTagUseCase
    Given the `addTag` method on TagFeature
    When the interface segregation is applied
    Then an `AddTagUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 2 - Extract GetAllTagsUseCase
    Given the `getAllTags` method on TagFeature
    When the interface segregation is applied
    Then a `GetAllTagsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 3 - Extract AddDefaultTagsUseCase
    Given the `addDefaultTags` method on TagFeature
    When the interface segregation is applied
    Then an `AddDefaultTagsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original (no parameters, no return value)
    And a dedicated implementation class implements this interface

    Scenario: 4 - Extract DeleteTagUseCase
    Given the `deleteTag` method on TagFeature (with optional `force` parameter)
    When the interface segregation is applied
    Then a `DeleteTagUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original (including `force: Boolean = false`)
    And a dedicated implementation class implements this interface

    Scenario: 5 - Extract GetDefaultTagUseCase
    Given the `defaultTag` method on TagFeature
    When the interface segregation is applied
    Then a `GetDefaultTagUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 6 - Extract EditTagUseCase
    Given the `editTag` method on TagFeature
    When the interface segregation is applied
    Then an `EditTagUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 7 - Preserve tag domain behavior
    Given existing tag domain business rules and tests
    When all 6 use case interfaces have been created and implemented
    Then all existing domain tests for tags remain green
    And no tag domain model or output-port signature is modified
    And the application layer can resolve each use case independently via dependency injection

**Notes**
- All new interfaces must be located in `domain/port/input`, under a `tag` sub-package for clarity.
- Strict signature parity is required: no parameter reorder, no type change, no return type change.
- The `deleteTag` interface must preserve the default value `force: Boolean = false`.
- The existing `TagFeature` sealed interface may be kept temporarily for migration or removed once all consumers are updated.
- This issue covers domain layer only; application layer wiring is tracked in a separate issue.
