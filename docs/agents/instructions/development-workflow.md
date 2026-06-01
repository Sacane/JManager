---
description: 'Cross-cutting development workflow: TDD, clean code principles, and quality analysis — applies to every layer'
applyTo: '{application,domain,infrastructure,client}/**'
---

# Development Workflow

This document defines the mandatory development workflow for **every layer** of the project: `domain`, `infrastructure`, `application`, and `client`.  
Each layer is treated **individually** — apply the full workflow within one layer before moving to the next.

---

## 1. TDD: Red → Green → Refactor

Every change — feature, fix, or evolution — must follow the TDD cycle strictly.

### 1.1 Red: Write a Failing Test

- Write a test expressing the expected behaviour **before** touching production code.
- The test must fail for the **right reason** (missing implementation, not a syntax error).
- Keep the test small, focused, and readable at a glance.
- Start at the lowest possible level within the current layer:
  - `domain`: business rule, entity invariant, value object
  - `infrastructure`: adapter behaviour, persistence mapping
  - `application`: use case orchestration, REST endpoint contract
  - `client`: component rendering, composable logic, utility function

### 1.2 Green: Make It Work — No Matter What

- Write the **minimum code** required to make the test pass.
- At this stage, **making the code work is paramount**, even if the code is not clean.
- Quick, direct, and ugly code is perfectly acceptable — the only goal is a **green test suite**.
- Do not anticipate future cases, do not refactor, do not beautify.
- If multiple tests fail, address **one behaviour at a time**.

### 1.3 Refactor: Improve Without Changing Behaviour

- Refactor **only when all tests are green**.
- Simplify names, extract useful abstractions, eliminate obvious duplication.
- Re-run the layer tests after every refactoring step to ensure no regression.
- Do not introduce new behaviour during this phase.

---

## 2. Mandatory Final Analysis (Post-Green, Post-Refactor)

Once the feature is fully implemented, all tests pass, and the initial refactoring is done, **always** perform a final quality analysis before considering the task complete. This step is **never optional** — skipping it is a defect in the process.

Review the new code **and its surrounding context** against the following checklist:

### 2.1 SOLID Principles

- **Single Responsibility (SRP)**: Does each class/component/composable have one reason to change?
- **Open/Closed (OCP)**: Can behaviour be extended without modifying existing code?
- **Liskov Substitution (LSP)**: Are implementations fully substitutable for their abstractions?
- **Interface Segregation (ISP)**: Are interfaces/ports/contracts small and focused?
- **Dependency Inversion (DIP)**: Does the code depend on abstractions, not concretions?

### 2.2 Design Patterns

- Identify opportunities for patterns (Strategy, Factory, Template Method, Composable extraction, etc.) **only when they solve a concrete structural problem**.
- Do not apply patterns speculatively.
- When a pattern is identified, **describe the problem, name the pattern, and wait for user confirmation** before applying it.

### 2.3 Duplication Removal

Actively search for duplication at three levels:

1. **Method/function extraction** — same logic in two or more places → extract with an intent-revealing name.
2. **Class/composable extraction** — several methods sharing the same data or concept → extract into a dedicated unit. Respect the layer where the unit belongs.
3. **Pattern-level duplication** — recurring structural problem → consider a design pattern (with user confirmation).

### 2.4 Naming and Readability

- Verify that names express **intent**, not mechanics.
- Check that responsibilities are correctly distributed across the layer's units.

### 2.5 Out-of-Scope Findings — Backlog

During any task, observations that are real problems but **outside the current scope** must be recorded so they are not lost. **Do not silently discard them.**

- Create a file in `docs/backlog/{short-slug}.md` describing the finding.
- Include: what was observed, where exactly (file + line), what the correct behaviour should be, and estimated impact.
- Do **not** fix the issue inline — stay focused on the current task.
- Mention the backlog entry in the chat so the developer is aware.

> This applies to any layer and any type of finding: incorrect HTTP mapping, naming inconsistency, missing constraint, potential security gap, stale comment, etc.

---

## 3. Layer-by-Layer Application

The workflow above must be executed **individually for each layer** involved in a change. The recommended order for backend features is:

1. `domain` — business rules and ports
2. `infrastructure` — adapters implementing the ports
3. `application` — API controllers, DTOs, wiring
4. `client` — frontend components, composables, pages

For each layer:
- Complete the full Red → Green → Refactor cycle.
- Run the final analysis (Section 2) **within that layer** before moving to the next.
- Only proceed to the next layer when the current one is fully green and reviewed.

For changes scoped to a single layer (e.g. a frontend-only fix), the same workflow applies — no layer is exempt.

---

## 4. Feature Flag Integration — Code First, Gate Second

When a feature must be gated behind a feature flag, apply the following rule without exception:

**Code the feature in full first.** Complete the Red → Green → Refactor cycle for every layer
as if the flag did not exist. All tests must be green and the final quality analysis (Section 2)
must be done before touching the flag integration.

**Only then add the flag check.** The check is a separate, atomic commit on top of a working
feature — not mixed into the business logic.

### 4.1 Domain: flag check position

- The flag check must be the **first statement** in the use case `handle()` method — before any
  input validation or business invariant check.
- Read the flag via the `FeatureFlagRepository` output port (already available in domain).
- A missing flag row (`findByKey()` returns `null`) must be treated as **disabled** (safe-by-default,
  consistent with the seeder).
- Return a dedicated failure state (e.g. `ResultState.FEATURE_DISABLED`) — never reuse
  `FORBIDDEN` or `INVALID`, which carry different business semantics.

```kotlin
override fun handle(command: MyCommand): Result<MyResult> {
    val flag = featureFlagRepository.findByKey(FeatureKey.MY_FEATURE)
    if (flag == null || !flag.enabled) {
        return failure(
            ResultState.FEATURE_DISABLED,
            DomainError(ResultState.FEATURE_DISABLED.code, "domain.my_feature.disabled", "Feature is disabled")
        )
    }
    // existing business logic unchanged below
}
```

### 4.2 Application: HTTP mapping

- Map `ResultState.FEATURE_DISABLED` in `ProblemDetailHandler` to **HTTP 404** (hides the
  endpoint's existence when the feature is off — safer than 403 for public-facing registration
  endpoints). Discuss with the team if a different status is required.

### 4.3 Client: conditional rendering

- Gate the UI section with `v-if="isEnabled(FEATURE_KEYS.MY_FEATURE)"` using the existing
  `useFeatureFlags()` composable.
- The feature key must be declared in `client/constants/featureKeys.ts` before use.
- Default visual state (flag disabled) must be clean — no empty containers, no broken layouts.

### 4.4 Tests

- Tests for the feature's own business logic must not depend on the flag state (they were written
  before the gate was added).
- Add separate tests for the flag-disabled path in each layer:
  - Domain: use case returns `FEATURE_DISABLED` when flag is off.
  - Application: endpoint returns the expected HTTP status when flag is off.
  - Client: UI element is absent when flag is off.
