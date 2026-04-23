# Testing Guidelines

Testing guidelines for the JManager project.

## 1. Goal and Scope

This document defines the Kotlin testing strategy to ensure:
- reliability of business rules
- safe evolution of the codebase
- fast feedback during development

The strategy follows the project architecture:
- `domain`: pure business unit tests
- `application`: use case and orchestration tests
- `infrastructure`: integration tests for technical adapters

## 2. Guiding Principles

- Prioritise the validation of business rules defined in `FEATURES.md`.
- Write deterministic tests (no uncontrolled system time, no implicit randomness).
- Test observable behaviour, not internal implementation.
- Keep tests readable, concise, and focused on a single intent.
- Every fixed bug must add at least one non-regression test.

## 3. Test Pyramid Adapted to the Project

- Majority base: unit tests in `domain`.
- Intermediate level: application tests in `application` with test doubles (fakes/stubs).
- Target apex: integration tests in `infrastructure` at technical boundaries.

Practical rule:
- a business change starts with a test in `domain`
- a flow change adds tests in `application`
- a technical connector change adds tests in `infrastructure`

## 4. Conventions per Module

### 4.1 Domain Tests

- Target: entities, value objects, business services, state transitions.
- No framework dependency.
- No I/O access (DB, HTTP, filesystem).
- Explicitly verify invariants.

### 4.2 Application Tests

- Target: use cases, port orchestration, result mapping, REST connectors.
- Use fakes/stubs for outgoing ports (repository, notification, etc.).
- Verify:
  - REST API inputs and outputs (HTTP status codes, response bodies)
  - use case result
  - expected side effects (port call, event, persistence)
  - absence of side effects on business failure

### 4.3 Infrastructure Tests

- Target: concrete adapters (persistence, external clients, messaging).
- Prefer realistic integration tests over heavy mocks.
- Verify mappings:
  - technical → business
  - business → technical
- Resilience details (timeout, retry) are tested here.

## 5. Test Structure and Naming

- One test file per main type under test.
- Explicit, behaviour-oriented test names.
- Recommended format: `shouldExpectedBehavior_whenContext`.

Examples:
- `shouldRefuseOrder_whenTokensAreInsufficient`
- `shouldCancelOrder_whenOrderIsNotAcknowledged`
- `shouldReturnFailure_whenRepositoryIsUnavailable`

## 6. Test Data and Fixtures

- Centralise shared fixtures to avoid duplication.
- Use test builders/factories with explicit default values.
- Avoid overly rich fixtures when only a subset is needed.
- Any temporal data must be fixed (test `Clock` or constant timestamp).

## 7. Assertions and Test Doubles

- Verify business outcomes before interactions.
- Avoid over-verification of calls (test the essential, not every internal detail).
- Use strict mocks only when the interaction is a functional requirement.
- Prefer simple fakes when behaviour is easy to simulate.

## 8. Flakiness and Stability

- Forbid `Thread.sleep` in application/business tests.
- Forbid implicit dependencies on test execution order.
- Each test must be independent and executable in isolation.
- Tests must pass identically locally and in CI.

## 9. Validation Commands

- Full suite:
  - `./gradlew test`
- Targeted module:
  - `./gradlew :domain:test`
  - `./gradlew :application:test`
  - `./gradlew :infrastructure:test`

## 10. Definition of Done (Testing)

An evolution is considered complete when:
- impacted business rules are covered by tests
- important business failure cases are tested
- tests pass on the concerned module and on the full suite
- no flaky test is introduced
- a non-regression test is added for every fixed bug

## 11. TDD Approach (Red, Green, Refactor)

> The complete development workflow (TDD cycle, mandatory final analysis, SOLID, duplication, design patterns) is defined in  
> [`development-workflow.md`](development-workflow.md) and applies to **all layers**.  
> This section summarises the testing-specific aspects of TDD.

TDD is the recommended method for implementing business rules and limiting regressions.

### 11.1 Red: Write a Failing Test

- Write a test expressing a business rule or a concrete bug first.
- Verify that the test fails for the right reason.
- The Red test must be small, focused, and understandable at a glance.
- Start at the lowest possible level:
  - `domain` for a business rule
  - `application` for a use case orchestration
  - `infrastructure` for a technical behaviour

### 11.2 Green: Make the Test Pass with Minimum Code

- Write the smallest implementation that makes the test pass.
- Avoid anticipating cases not required by the current test.
- If multiple tests fail, address one behaviour at a time.
- Validate locally with a minimal scope before running the full suite.

### 11.3 Refactor: Improve Without Changing Behaviour

- Refactor only when all tests are green.
- Simplify names, extract useful abstractions, eliminate duplication.
- Verify that no business rule is moved out of `domain` without justification.
- Re-run the module tests then the full suite after refactoring.

### 11.4 Practical Application Rules

- A business evolution commit must contain at least:
  - an initial Red test
  - the associated Green implementation
  - the necessary Refactor adjustments
- In case of a production bug:
  - reproduce it with a failing test
  - fix it afterwards
  - keep the test as a non-regression
- If a test is difficult to write, treat it as a design signal to improve.

### 11.5 TDD Anti-Patterns to Avoid

- Writing production code first and then "adding tests" afterwards.
- Writing tests that are too broad and validate multiple rules at once.
- Mocking excessively to the point of testing internal implementation rather than behaviour.
- Ignoring an unstable test instead of fixing its root cause (time, order, shared state, implicit I/O).
