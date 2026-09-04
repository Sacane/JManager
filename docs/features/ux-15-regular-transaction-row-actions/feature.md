# UX-15 — Managing my recurring entries

**Context**
Functional acceptance for UX-15, independent of any layer. Editing requires an unsignalled double click and single deletion is buried inside the edit dialog.

**Acceptance Criteria**
Feature: Managing my recurring entries
  In order to keep my recurring entries up to date
  As an authenticated user
  I want visible edit and delete actions on every entry

Scenario: I can edit and delete without guessing
  Given I am looking at my regular transactions
  When I look at an entry
  Then a visible action lets me edit it and another lets me delete it

Scenario: Deleting is confirmed
  Given I ask to delete a regular transaction
  When the confirmation is displayed
  Then it names the transaction and the deletion happens only after I confirm

Scenario: The actions exist on every device
  Given I am looking at my regular transactions on a phone
  When I look at an entry
  Then the same actions are available as on desktop

**Notes**
- Layer-agnostic functional acceptance for UX-15. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
