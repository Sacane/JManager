---
applyTo: "client/**"
description: "Frontend-specific agent guidelines for Nuxt/Vue code"
---

# Frontend Agent Guidelines

These rules apply to all frontend work in this repository.

## Testing Discipline

- After every code change, add or update tests to match the new behavior.
- **Always re-run the full frontend test suite (`pnpm test`) at the end of every implementation session**, before considering the work done.
- Iterate on implementation and tests until the frontend test suite is fully green.
- Do not consider a task done if tests are failing.
- When adding a new composable or utility that Nuxt auto-imports, ensure it is also stubbed via `vi.stubGlobal` in `tests/setup.ts` so all existing page and component tests continue to pass without changes.

## Reuse and Code Quality

- Follow frontend best practices for readability, maintainability, and consistency.
- Avoid code duplication.
- Extract repeated UI/logic into reusable components or composables.
- Prefer reusable building blocks that can be consumed by parent components.

## API Simulation in Tests

- Mock API-facing dependencies in tests.
- Simulate realistic success and failure scenarios.
- Ensure component and page tests validate behavior through mocks rather than real network calls.
- Keep mocks explicit and close to the tested behavior to maintain test clarity.
