# Infrastructure Module — Persist the budget target

**Context**
The budget target per booklet must be persisted alongside the existing monthly cycle settings, which
already carry a per-booklet configuration. The write must stay atomic with the rest of the settings
update.

**Acceptance Criteria**
Feature: Budget target persistence
  In order to keep budget targets across sessions
  As the system
  I want the target stored with the booklet cycle settings

Scenario: 1. A target is persisted and reloaded
  Given a budget target set for a booklet
  When the user settings are reloaded
  Then the stored target is returned with the booklet configuration

Scenario: 2. Settings are written atomically
  Given a settings update carrying cycles and budget targets
  When the update is persisted
  Then every change is committed in the same transaction

Scenario: 3. Deleting a booklet removes its target
  Given a booklet holding a budget target
  When the booklet is deleted
  Then the associated target is removed as well

**Notes**
- Add a Flyway migration extending the booklet settings table with a nullable numeric column.
- Integration tests against a real database.
- Priority P1 - Effort L (full stack) - Part 2 of 4.
