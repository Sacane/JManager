# UX-16 — Same interaction on every device

**Context**
Functional acceptance for UX-16, independent of any layer. Desktop requires a double click while mobile opens on a single tap, and on the booklet page a tap selects instead.

**Acceptance Criteria**
Feature: Same interaction on every device
  In order to know how to open a record
  As an authenticated user
  I want the same interaction on desktop and on mobile

Scenario: Opening a record works the same way
  Given I am looking at a list of transactions
  When I use the open action on a phone and on a desktop
  Then the same editor opens in both cases

Scenario: Selecting and opening are distinct
  Given I am looking at a list on a phone
  When I tap the body of a row
  Then the row is selected without opening the editor

**Notes**
- Layer-agnostic functional acceptance for UX-16. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
