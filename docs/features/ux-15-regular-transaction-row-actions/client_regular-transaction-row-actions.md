# Client Module — Make edit and delete discoverable on regular transactions

**Context**
On `pages/regular-transaction/index.vue` the actions column only contains "Lier" and "Delier".
Editing is only reachable through an unsignalled double click on the row, and deleting a single
regular transaction is only possible from inside the edit dialog. Bulk deletion exists but its button
is hidden on mobile. A user can reasonably conclude that a regular transaction cannot be edited.

**Acceptance Criteria**
Feature: Discoverable actions on regular transactions
  In order to manage my recurring entries
  As an authenticated user
  I want visible edit and delete actions on every row

Scenario: 1. Edit and delete are visible on desktop
  Given I am on the regular transactions page on desktop
  When the table is rendered
  Then every row shows an edit action and a delete action

Scenario: 2. Edit and delete are visible on mobile
  Given I am on the regular transactions page on mobile
  When the list is rendered
  Then every card shows an edit action and a delete action

Scenario: 3. Deleting asks for confirmation
  Given I am on the regular transactions page
  When I activate the delete action of a row
  Then a confirmation naming the transaction is displayed
  And the transaction is removed only after I confirm

Scenario: 4. Bulk deletion is available on mobile
  Given I selected several regular transactions on mobile
  When the selection is not empty
  Then a bulk delete action is available

**Notes**
- Backend already exposes `DELETE /api/transaction/regular/{id}` and `DELETE /api/transaction/regular`.
- Files: `pages/regular-transaction/index.vue`, `components/dialog/RegularTransactionDialogCard.vue`.
- Priority P1 - Effort M - Frontend only.
