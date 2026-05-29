# Domain Module — Feature Flag Service

**Context**
JManager needs a clean, reusable feature-flag capability so behaviour can be toggled without redeploying.
Flags are **global** (a single ON/OFF state for the whole application — no per-user or per-plan targeting at
flag level) and **runtime-controlled** (persisted, toggleable on the fly by an administrator). The domain owns
the concept of a feature flag, the registry of known flag keys, and the use cases to query and toggle them.
Per-plan behavioural differences (e.g. beta tester vs simple user) are decided in the consuming domain logic
based on the resolved flag state — never encoded in the flag itself.

**Scope (domain)**
- `FeatureFlag` model: a known `FeatureKey`, an `enabled` boolean, an optional human description.
- `FeatureKey` — a typed registry (enum or sealed set) of the application's known flags to avoid magic strings.
- Output port `FeatureFlagRepository`: `findByKey(key)`, `findAll()`, `upsert(flag)`.
- Input ports / use cases:
  - `IsFeatureEnabledUseCase` (query) — resolves a single flag, defaulting to **disabled** when unknown/absent.
  - `GetAllFeatureFlagsUseCase` (query) — returns the full set of known flags with their current state.
  - `ToggleFeatureFlagUseCase` (command) — sets a flag's enabled state (intended for ADMIN callers).
- `FeatureFlagService` implementing the use cases, with no framework dependency.

**Acceptance Criteria**
Feature: Feature flag service
  In order to change application behaviour without redeploying
  As the system
  I want a domain service to query and toggle global feature flags

Scenario: Resolve an enabled flag
  Given a feature flag "email-verification" persisted with enabled = true
  When the system checks whether "email-verification" is enabled
  Then the service returns true

Scenario: Resolve a disabled flag
  Given a feature flag "email-verification" persisted with enabled = false
  When the system checks whether "email-verification" is enabled
  Then the service returns false

Scenario: Unknown or unpersisted flag defaults to disabled
  Given no persisted state exists for flag "email-verification"
  When the system checks whether "email-verification" is enabled
  Then the service returns false

Scenario: List all known flags with their state
  Given the known flag registry contains "email-verification" and "email-verification-simple-user-registration"
  When the system requests all feature flags
  Then the service returns one entry per known flag with its resolved enabled state

Scenario: Toggle a flag on
  Given a feature flag "email-verification" persisted with enabled = false
  When the system toggles "email-verification" to enabled = true
  Then the repository persists the flag with enabled = true
  And subsequent checks for "email-verification" return true

Scenario: Toggling an unknown flag key is rejected
  Given a flag key that is not part of the known registry
  When the system attempts to toggle it
  Then the service returns a validation failure
  And nothing is persisted
