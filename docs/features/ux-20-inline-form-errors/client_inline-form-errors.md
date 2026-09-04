# Client Module — Attach form errors to the fields they concern

**Context**
Except for the password confirmation, no validation error is attached to the field it concerns: every
failure is reported through a toast that disappears after a few seconds. `TransactionCreationDialog`
even shows "Veuillez saisir un montant superieur a 0" for a condition that also covers an empty
label, so the message can be plainly wrong. The user is left without knowing which field to fix.

**Acceptance Criteria**
Feature: Inline form validation
  In order to know what to correct
  As an authenticated user
  I want errors displayed next to the field that caused them

Scenario: 1. An empty label is reported on the label field
  Given the transaction creation dialog is open with an empty label
  When I submit the form
  Then an error is displayed under the label field

Scenario: 2. An invalid amount is reported on the amount field
  Given the transaction creation dialog is open with an amount of zero
  When I submit the form
  Then an error is displayed under the amount field

Scenario: 3. Errors clear as the user corrects them
  Given an inline error is displayed on a field
  When I enter a valid value in that field
  Then the error disappears without submitting again

Scenario: 4. Server errors remain reported
  Given the form is valid
  When the server rejects the submission
  Then the failure is still reported to the user

**Notes**
- Files: `components/dialog/TransactionCreationDialog.vue`, `components/dialog/RegularTransactionCreationDialog.vue`, `components/dialog/TagFormDialog.vue`, `pages/login.vue`, `pages/settings/index.vue`.
- Toasts stay for server and network failures; field-level validation moves inline.
- Priority P1 - Effort M - Frontend only.
