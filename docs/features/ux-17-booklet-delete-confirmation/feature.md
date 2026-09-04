# UX-17 — Protection against destructive mistakes

**Context**
Functional acceptance for UX-17, independent of any layer. Deleting a booklet destroys all its transactions behind a single click.

**Acceptance Criteria**
Feature: Protection against destructive mistakes
  In order not to destroy my history by accident
  As an authenticated user
  I want an irreversible deletion to require a deliberate confirmation

Scenario: Deleting a booklet is deliberate
  Given I ask to delete a booklet holding transactions
  When the confirmation is displayed
  Then I must retype its name before the deletion can proceed

Scenario: The consequences are stated
  Given the deletion confirmation is displayed
  When I read it
  Then it tells me how many transactions will be permanently deleted

**Notes**
- Layer-agnostic functional acceptance for UX-17. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
