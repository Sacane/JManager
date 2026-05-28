# Infrastructure Module — Feature Flag Persistence

**Context**
Feature flags are runtime-controlled and therefore persisted in the database. This module provides the JPA
adapter implementing the domain `FeatureFlagRepository` port, plus idempotent seeding so every known flag from
the domain registry exists in the database at startup (defaulting to **disabled**) without overwriting an
administrator's existing choice.

**Scope (infrastructure)**
- `FeatureFlagEntity` mapped to a `feature_flag` table: `key` (unique), `enabled`, `description`.
- JPA `FeatureFlagRepositoryJpaAdapter` implementing `findByKey`, `findAll`, `upsert`.
- Startup seeding: for each known `FeatureKey`, insert a row with `enabled = false` if absent; never modify an
  existing row's enabled value.

**Acceptance Criteria**
Feature: Feature flag persistence
  In order to control flags at runtime
  As the system
  I want feature flags stored and retrievable from the database

Scenario: Persist and retrieve a flag
  Given the feature flag adapter
  When a flag "email-verification" is upserted with enabled = true
  Then querying the flag by key returns enabled = true

Scenario: Upsert updates an existing flag in place
  Given a persisted flag "email-verification" with enabled = false
  When the flag is upserted with enabled = true
  Then no duplicate row is created
  And the stored flag has enabled = true

Scenario: Seeding creates missing flags as disabled
  Given the feature_flag table has no row for a newly added known key
  When the application starts and seeding runs
  Then a row is created for that key with enabled = false

Scenario: Seeding is idempotent and preserves admin choices
  Given a persisted flag "email-verification" with enabled = true set by an administrator
  When the application restarts and seeding runs again
  Then the flag remains enabled = true
  And no duplicate row is created
