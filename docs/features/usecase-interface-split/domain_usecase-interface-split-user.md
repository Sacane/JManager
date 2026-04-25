# Use Case Interface Segregation – Domain: User Category

**Context**
The `UserFeature` sealed interface in `domain/port/api` currently exposes 7 methods as a single contract.
Following the use-case interface segregation refactor, each method must be extracted into its own single-method interface
located in `domain/port/input`, and each implementation class must implement exactly one interface.

Methods to decompose:
- `login(pseudonym, userPassword): Result<UserToken>`
- `logout(token): Result<Nothing>`
- `refresh(refreshToken): Result<UserToken>`
- `register(username, password, confirmPassword): Result<User>`
- `createAdminIfNotExists(username, password): Result<User>`
- `getSettings(token): Result<UserSettings>`
- `updateSettings(token, projectionWindowDays, bookletCycles): Result<UserSettings>`

**Acceptance Criteria**
Feature: Use case interface segregation for User domain operations

    In order to enforce single-responsibility and explicit domain boundaries
    As a domain maintainer
    I want each user operation to be represented by a dedicated single-method interface

    Scenario: 1 - Extract LoginUseCase
    Given the `login` method on UserFeature
    When the interface segregation is applied
    Then a `LoginUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 2 - Extract LogoutUseCase
    Given the `logout` method on UserFeature
    When the interface segregation is applied
    Then a `LogoutUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 3 - Extract RefreshSessionUseCase
    Given the `refresh` method on UserFeature
    When the interface segregation is applied
    Then a `RefreshSessionUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 4 - Extract RegisterUserUseCase
    Given the `register` method on UserFeature
    When the interface segregation is applied
    Then a `RegisterUserUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 5 - Extract CreateAdminIfNotExistsUseCase
    Given the `createAdminIfNotExists` method on UserFeature
    When the interface segregation is applied
    Then a `CreateAdminIfNotExistsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 6 - Extract GetUserSettingsUseCase
    Given the `getSettings` method on UserFeature
    When the interface segregation is applied
    Then a `GetUserSettingsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 7 - Extract UpdateUserSettingsUseCase
    Given the `updateSettings` method on UserFeature
    When the interface segregation is applied
    Then an `UpdateUserSettingsUseCase` interface is created in `domain/port/input`
    And it contains exactly one method with the same signature as the original
    And a dedicated implementation class implements this interface

    Scenario: 8 - Preserve user domain behavior
    Given existing user domain business rules and tests
    When all 7 use case interfaces have been created and implemented
    Then all existing domain tests for users remain green
    And no user domain model or output-port signature is modified
    And the application layer can resolve each use case independently via dependency injection

**Notes**
- All new interfaces must be located in `domain/port/input`, under a `user` sub-package for clarity.
- Strict signature parity is required: no parameter reorder, no type change, no return type change.
- The existing `UserFeature` sealed interface may be kept temporarily for migration or removed once all consumers are updated.
- This issue covers domain layer only; application layer wiring is tracked in a separate issue.
