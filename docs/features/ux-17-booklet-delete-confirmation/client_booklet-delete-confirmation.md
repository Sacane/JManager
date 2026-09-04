# Client Module — Require a strong confirmation before deleting a booklet

**Context**
Deleting a booklet destroys every transaction it holds. `pages/booklet/index.vue` announces this in
the confirmation message but a single click on "Supprimer" is enough to proceed. For an irreversible
destruction of that scope the user must be asked to retype the booklet label, as is done for the
account deletion.

**Acceptance Criteria**
Feature: Strong confirmation before deleting a booklet
  In order not to destroy my history by accident
  As an authenticated user
  I want to retype the booklet name before it is deleted

Scenario: 1. Deletion requires retyping the label
  Given I ask to delete a booklet named Livret A
  When the confirmation dialog is displayed
  Then the confirm action stays disabled until I type Livret A

Scenario: 2. A wrong label does not delete anything
  Given the deletion confirmation of a booklet is displayed
  When I type a label that does not match
  Then the confirm action stays disabled and no request is sent

Scenario: 3. The consequences are stated
  Given the deletion confirmation of a booklet is displayed
  When I read the message
  Then it states the number of transactions that will be permanently deleted

**Notes**
- Files: `pages/booklet/index.vue`.
- The current message also carries two typos, fixed by UX-07.
- Priority P1 - Effort S - Frontend only.
