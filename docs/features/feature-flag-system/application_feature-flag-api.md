# Application Module — Feature Flag API

**Context**
The frontend needs to know which flags are active, and administrators need to toggle them at runtime. This
module exposes a **dedicated** read endpoint the client calls at boot, plus an admin-only toggle endpoint, and
wires both through the existing command/query bus to the domain use cases.

**Scope (application)**
- `GET /feature-flags` — public (or authenticated, matching existing session conventions) endpoint returning
  the full set of known flags as a map of `key -> enabled`. Returns all known flags, not only enabled ones, so
  the client can reason about both states.
- `PATCH /admin/feature-flags/{key}` — ADMIN-only endpoint to set a flag's enabled state.
- Response/request DTOs; mapping from domain `FeatureFlag` to DTO.
- Wiring through the query bus (`GetAllFeatureFlagsUseCase`) and command bus (`ToggleFeatureFlagUseCase`).

**Acceptance Criteria**
Feature: Feature flag API
  In order to drive UI and operations from flag state
  As a client or administrator
  I want REST endpoints to read and toggle feature flags

Scenario: Read all flags
  Given flags "email-verification" = true and "email-verification-simple-user-registration" = false
  When a client calls GET /feature-flags
  Then the response is 200 with a map containing both keys and their boolean state

Scenario: Admin toggles a flag
  Given an authenticated admin user
  And a flag "email-verification" currently disabled
  When the admin calls PATCH /admin/feature-flags/email-verification with enabled = true
  Then the response is 200
  And a subsequent GET /feature-flags reports "email-verification" = true

Scenario: Non-admin cannot toggle a flag
  Given an authenticated user without the ADMIN role
  When the user calls PATCH /admin/feature-flags/email-verification
  Then the response is 403 Forbidden
  And the flag state is unchanged

Scenario: Toggling an unknown flag returns a client error
  Given an authenticated admin user
  When the admin calls PATCH /admin/feature-flags/does-not-exist
  Then the response is a 400/404 error
  And no flag is created or modified
