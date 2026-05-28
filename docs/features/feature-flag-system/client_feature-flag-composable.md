# Client Module — Feature Flag Composable

**Context**
The Nuxt frontend must gate UI behaviour on the backend flag state. It fetches the flag map once from the
dedicated `GET /feature-flags` endpoint and exposes a reactive `isEnabled(key)` helper used across pages and
components. The aim is a clean, ergonomic API mirroring the backend service so UI gating is one call away.

**Scope (client)**
- A store/composable (e.g. `useFeatureFlags`) holding the flag map.
- A plugin or app-init hook that loads `/feature-flags` once at startup (and exposes a refresh).
- `isEnabled(key: string): ComputedRef<boolean>` (or equivalent) returning `false` for unknown keys.
- Typed flag keys mirroring the backend registry to avoid magic strings.

**Acceptance Criteria**
Feature: Feature flag gating in the client
  In order to show or hide features per environment
  As a frontend
  I want a composable that reflects backend flag state

Scenario: Flags are loaded at app start
  Given the backend returns { "email-verification": true }
  When the app initialises
  Then the feature-flag store contains "email-verification" = true

Scenario: Gate a UI element on an enabled flag
  Given the store reports "email-verification" = true
  When a component checks isEnabled("email-verification")
  Then it receives true and renders the gated UI

Scenario: Gate a UI element on a disabled flag
  Given the store reports "email-verification" = false
  When a component checks isEnabled("email-verification")
  Then it receives false and the gated UI is not rendered

Scenario: Unknown flag key resolves to disabled
  Given the store has no entry for "unknown-flag"
  When a component checks isEnabled("unknown-flag")
  Then it receives false

Scenario: Backend unavailable falls back to disabled
  Given the /feature-flags request fails at startup
  When components check any flag
  Then all flags resolve to false
  And the app does not crash
