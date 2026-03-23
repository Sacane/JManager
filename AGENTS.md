# Agent Instructions

## Architecture

This project follows **Hexagonal Architecture** (also known as Ports & Adapters).

- The **domain** layer is the core and must remain completely isolated — it must never depend on infrastructure concerns.
- The **infra** layer contains adapters that implement the ports defined in the domain.
- Always enforce the **closed rule**: no infrastructure dependency may leak into the domain.

## Testing Guidelines

### Domain Tests
- Tests in the domain layer must identify and validate **business rules**, not the internal behaviour of a specific class.
- No infrastructure or adapter dependencies are allowed in domain tests.

### Infrastructure Tests
Infrastructure tests are split into three scopes:

1. **API → Domain**: Verify that the API returns the correct HTTP response for each scenario (status codes, response bodies, error handling).
2. **Domain → Database**: Verify that persistence works correctly (read/write operations, data integrity).
3. **End-to-End (API → Domain → Database)**: Verify that a specific API call produces the expected outcome from entry point to database, covering the full stack.

Keep these scopes clearly separated — do not mix concerns across layers in a single test.

### General Rules
- Always add or update tests for any code you change, even if not explicitly requested.
- Fix all test and type errors until the entire test suite is green before considering a task complete.

## Documentation

- Only **public methods** and **port/interface contracts** must be documented.
- Do **not** document the internals of methods.
- Private functions may be documented only when strictly necessary for clarity.
- All documentation must be written in **English**.

## Changelog

- Always update `Changelog.md` whenever a feature or fix is implemented.
- Entries must clearly describe what changed and why.
- During a plan, you must update changelog only when I mention that the plan is fully complete and the time i want to push. You must do an effort to synthesis the maximum you can.