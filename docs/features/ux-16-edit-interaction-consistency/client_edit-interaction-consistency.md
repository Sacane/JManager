# Client Module — Align the edit interaction between desktop and mobile

**Context**
Opening a record differs by platform. On desktop, editing a transaction or a regular transaction
requires a double click on the row. On mobile, a single tap on the card opens the edit dialog, and
the handler is still named `handleRowDoubleClick`. On the booklet detail page a tap on a mobile row
selects the transaction instead, so there is no obvious way to open it. The interaction model must be
the same everywhere and must not rely on an undiscoverable gesture.

**Acceptance Criteria**
Feature: Consistent edit interaction
  In order to know how to open a record
  As an authenticated user
  I want the same interaction on desktop and on mobile

Scenario: 1. An explicit action opens the editor on both platforms
  Given I am on a list of transactions
  When I activate the edit action of a row
  Then the edit dialog opens on desktop and on mobile alike

Scenario: 2. Selecting and opening are distinct on mobile
  Given I am on the booklet detail page on mobile
  When I tap the row body
  Then the row selection is toggled without opening the edit dialog

Scenario: 3. The double click shortcut remains available on desktop
  Given I am on a list of transactions on desktop
  When I double click a row
  Then the edit dialog opens

**Notes**
- Depends on UX-15.
- Files: `pages/regular-transaction/index.vue`, `pages/booklet/[id].vue`.
- Rename `handleRowDoubleClick` to reflect the real intent.
- Priority P1 - Effort S - Frontend only.
