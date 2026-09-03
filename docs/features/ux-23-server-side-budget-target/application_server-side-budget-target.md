# Application Module — Expose the budget target in the user settings API

**Context**
`UserSettingsDTO` currently carries `projectionWindowDays` and `bookletCycles` only. The budget target
per booklet must be added to the settings read and update payloads so the dashboard can stop relying
on browser storage.

**Acceptance Criteria**
Feature: Budget target in the settings API
  In order to read and write my budget from any device
  As an authenticated user
  I want the settings endpoints to carry my budget targets

Scenario: 1. The settings response carries the targets
  Given an authenticated user with a budget target on a booklet
  When the settings endpoint is called
  Then the response carries the target for that booklet

Scenario: 2. Updating the settings stores the target
  Given an authenticated user owning a booklet
  When the settings are updated with a budget target
  Then the response is successful and the target is persisted

Scenario: 3. A negative target is rejected
  Given an authenticated user owning a booklet
  When the settings are updated with a negative budget target
  Then the response is 400 Bad Request

Scenario: 4. A target on a foreign booklet is rejected
  Given a booklet that belongs to another user
  When the settings are updated with a target for that booklet
  Then the response is 403 Forbidden

**Notes**
- Keep the DTO backward compatible: the field is optional so an older client keeps working.
- Priority P1 - Effort L (full stack) - Part 3 of 4.
