# Client Module — Remove dead components and the unimplemented user route

**Context**
Five frontend files are never imported. `components/delete.vue` is also broken: its `visible` ref is
never set to `true` and `onActionValid` is an empty `ref()` used as a click handler.
`components/TitleCard.vue` and `components/card/BalanceCard.vue` hardcode `bg-white`, so they would
break in dark mode if they were ever used. `pages/user/[id].vue` is a reachable production route that
renders `User {id}`.

**Acceptance Criteria**
Feature: No dead frontend code
  In order to keep the codebase readable
  As a developer
  I want unused and broken components removed

Scenario: 1. The unused components are deleted
  Given the client codebase
  When I search for imports of the unused components
  Then monthPicker, TitleCard, BalanceCard and delete no longer exist in the repository

Scenario: 2. The unimplemented user route is removed
  Given the client codebase
  When I navigate to a user detail URL
  Then the not-found page is displayed instead of an empty placeholder

Scenario: 3. The test suite stays green
  Given the components were removed
  When I run the frontend test suite
  Then every test passes

**Notes**
- Files to delete: `components/monthPicker.vue`, `components/TitleCard.vue`, `components/card/BalanceCard.vue`, `components/delete.vue`, `pages/user/[id].vue`.
- Check `tests/setup.ts` for any stub referencing them.
- Priority P0 - Effort XS - Frontend only.
