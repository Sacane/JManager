# Client Module — Selective Regeneration of Deleted Provisional Transactions

**Context**
On the booklet page, the "Regenerate" action (`BookletActionButtons.vue` → `BookletFilterActionBar.vue` → `regenerate()` in `client/pages/booklet/[id].vue`) currently calls `regenerateDeletedPrevisionalTransactions` directly and restores everything in one shot, with no visibility into what will come back. This must be replaced by a dialog: clicking "Regenerate" opens it, it lists every deleted provisional/virtual transaction candidate for the current month (label, amount, date), each with its own checkbox, plus a "select all" checkbox in the header. Confirming regenerates only the checked transactions.

The `hasRegenerableTransactions` flag (already returned by the transactions endpoint) keeps controlling whether the "Regenerate" action is shown/enabled at all.

**Acceptance Criteria**
```gherkin
Feature: Dialog to select which deleted provisional transactions to regenerate
  In order to control exactly which deleted forecast transactions come back
  As a user viewing a booklet
  I want to open a dialog listing the deleted transactions and choose which ones to restore

Scenario: 1. Opening the dialog shows the deleted transaction candidates
  Given a booklet with deleted provisional transactions for the displayed month
  When the user clicks the "Regenerate" action
  Then a dialog opens listing each deleted transaction with its label, amount and date
  And each row has its own unchecked checkbox

Scenario: 2. Select-all toggles every candidate
  Given the regenerate dialog is open with several candidates listed
  When the user checks the "select all" checkbox
  Then every candidate row becomes checked
  And unchecking "select all" clears every candidate row

Scenario: 3. Confirming restores only the selected transactions
  Given the regenerate dialog is open with several candidates and only some are checked
  When the user confirms the selection
  Then only the checked transactions are regenerated
  And the dialog closes
  And a success toast is shown reflecting whether previsional or virtual transactions were restored
  And the booklet transaction list is refreshed

Scenario: 4. Confirm is disabled with no selection
  Given the regenerate dialog is open
  When no candidate is checked
  Then the confirm action is disabled

Scenario: 5. Regeneration failure keeps the dialog usable
  Given the regenerate dialog is open with a valid selection
  When the confirmation request fails
  Then an error toast is shown
  And the dialog remains open with the user's selection preserved

Scenario: 6. No action offered when there is nothing to regenerate
  Given a booklet with no deleted provisional transactions for the displayed month
  When the user views the booklet's action bar
  Then the "Regenerate" action is not shown or is disabled, per the existing hasRegenerableTransactions flag
```

**Notes**
- New dialog component, e.g. `client/components/booklet/BookletRegenerateTransactionsDialog.vue`, following the existing `BookletConfirmPreviewDialog.vue` pattern.
- `useBooklet.ts` needs a new call to fetch candidates (backing the new GET endpoint) and an updated `regenerateDeletedPrevisionalTransactions` signature that accepts the selected regular transaction identifiers (per `docs/features/restore-deleted-provisional-transactions/application_*.md`); update the corresponding `vi.stubGlobal` stub in `tests/setup.ts`.
- Reuse existing amount/date formatting utilities used elsewhere on the booklet page rather than reformatting inline.
- For Weekly/Daily regular transactions, restoring one candidate restores the whole excluded month for that regular transaction (see the domain-module issue) — the dialog copy should not imply per-occurrence precision for those rows.
