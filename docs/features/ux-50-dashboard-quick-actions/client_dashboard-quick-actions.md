# Client Module — Replace the dashboard shortcuts with real actions

**Context**
The "Actions rapides" block of `pages/index.vue` holds three buttons — "Voir mes comptes", "Ajuster les
régulières", "Revoir mes tags" — that call `navigateTo` on pages the sidebar already links to. They
take space on the most visited page and do nothing the navigation does not already do.

The actions missing from the dashboard already exist as dialogs elsewhere, and are reused as they are:

- `TransactionCreationDialog`, saved with `useTransaction().saveTransaction(bookletLabel, dto)` — the
  API identifies the booklet by its label;
- `CsvImportDialog`, which takes a `bookletId` and is opened through its exposed `openDialog()`;
- `RegularTransactionCreationDialog`, which takes the booklet list and lets the user pick targets,
  saved with `useRegularTransaction().saveMonthlyTransaction`.

The dashboard always resolves a selected booklet when at least one exists, and shows the onboarding
state otherwise (UX-43), so the single-booklet actions always have a target.

**Acceptance Criteria**
Feature: Real quick actions on the dashboard
  In order to record what happened without leaving the overview
  As an authenticated user
  I want the quick actions to open the right dialog on the right account

Scenario: 1. Adding a transaction opens the creation dialog
  Given the dashboard shows the booklet "Livret A"
  When I activate the add transaction quick action
  Then the transaction creation dialog opens

Scenario: 2. The transaction is recorded on the selected booklet
  Given the transaction creation dialog was opened from the dashboard on "Livret A"
  When I submit a valid transaction
  Then it is saved against "Livret A"

Scenario: 3. The single-booklet actions name their target
  Given the dashboard shows the booklet "Livret A"
  When the quick actions are rendered
  Then the add and import actions name "Livret A"

Scenario: 4. Importing a statement targets the selected booklet
  Given the dashboard shows the booklet "Livret A"
  When I activate the import quick action
  Then the CSV import dialog opens for "Livret A"

Scenario: 5. A recurring entry can be created from the dashboard
  Given the dashboard shows at least one booklet
  When I activate the recurring entry quick action and submit a valid entry
  Then the recurring entry is saved

Scenario: 6. The dashboard figures are refreshed after a successful action
  Given a quick action was used from the dashboard
  When the transaction, the import or the recurring entry succeeds
  Then the dashboard data is reloaded

Scenario: 7. A failed save is reported and leaves the dialog open
  Given the transaction creation dialog is open on the dashboard
  When the server rejects the transaction
  Then the failure is reported
  And the dialog stays open with the user's input

Scenario: 8. No quick action duplicates the sidebar
  Given the dashboard is rendered
  When I inspect the quick actions
  Then none of them navigates to the booklets, regular transactions or tags pages

**Notes**
- Files: `pages/index.vue`. No new dialog; the three above are reused.
- Backlog estimate was S; three dialogs, their saves, the refresh and their tests make it closer to M.
- Priority P2 - Effort M - Frontend only.
