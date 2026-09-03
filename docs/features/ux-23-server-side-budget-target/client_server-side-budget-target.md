# Client Module — Read the budget target from the server

**Context**
`pages/index.vue` reads and writes the budget target through `useLocalStorage` on the key
`dashboard.budgetTargetsByBooklet.v1`. It must instead use the user settings API so the target and its
alerts follow the user across devices. Existing local values should be migrated once, then the local
key dropped.

**Acceptance Criteria**
Feature: Budget target shared across devices
  In order to see the same budget everywhere
  As an authenticated user
  I want my budget target read from and written to my account

Scenario: 1. The target is loaded from the settings
  Given a budget target stored on my account for a booklet
  When I open the dashboard on any device
  Then the budget card displays that target

Scenario: 2. Saving the target persists it on the account
  Given I am on the dashboard
  When I set a budget target and save it
  Then the target is sent to the settings endpoint and confirmed

Scenario: 3. A locally stored target is migrated once
  Given a budget target stored in the browser from a previous version
  When I open the dashboard while no target exists on my account
  Then the local value is sent to my account and the local key is removed

Scenario: 4. A failed save is reported
  Given I am on the dashboard
  When saving the budget target fails
  Then an error is reported and the previous target is still displayed

**Notes**
- Depends on the application part of UX-23.
- Files: `pages/index.vue`, `composables/useUserSettings.ts`.
- Priority P1 - Effort L (full stack) - Part 4 of 4.
