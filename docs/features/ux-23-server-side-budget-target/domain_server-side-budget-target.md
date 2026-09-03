# Domain Module — Budget target as a user setting

**Context**
The dashboard budget target is stored in the browser under
`dashboard.budgetTargetsByBooklet.v1`, so it is lost when the user changes device or browser and the
budget alerts effectively become per device. This is inconsistent with the rest of the user settings,
which are persisted server side. The domain must own a budget target per booklet as part of the user
settings.

**Acceptance Criteria**
Feature: Budget target per booklet
  In order to keep my budget across devices
  As an authenticated user
  I want my budget target stored with my settings

Scenario: 1. A budget target is recorded for a booklet
  Given an authenticated user owning a booklet
  When a budget target is set for that booklet
  Then the target is stored in the user settings

Scenario: 2. A negative target is rejected
  Given an authenticated user owning a booklet
  When a negative budget target is set for that booklet
  Then the update is rejected and the previous target is unchanged

Scenario: 3. A target on a foreign booklet is rejected
  Given a booklet that belongs to another user
  When a budget target is set for that booklet
  Then the update is rejected

Scenario: 4. Clearing a target removes it
  Given a booklet with a budget target
  When the target is cleared
  Then the user settings no longer hold a target for that booklet

**Notes**
- Monetary values use `BigDecimal`, never `Double` or `Float`.
- Extend the existing user settings aggregate rather than creating a parallel one.
- Priority P1 - Effort L (full stack) - Part 1 of 4.
