# UX-06 — No silent loss of settings

**Context**
Functional acceptance for UX-06, independent of any layer. The settings page has two save buttons of different scope, and using one discards the pending changes of the other.

**Acceptance Criteria**
Feature: No silent loss of settings
  In order not to lose what I just configured
  As an authenticated user
  I want each save action to state its scope and preserve the rest

Scenario: Saving one section preserves the others
  Given I edited a setting without saving it
  When I use another action on the same page
  Then my pending edit is still there afterwards

Scenario: Leaving the page warns me
  Given I edited a setting without saving it
  When I navigate away from the page
  Then I am warned before my change is lost

Scenario: Pending changes are visible
  Given I edited a setting without saving it
  When I look at the section
  Then it indicates that changes are not saved yet

**Notes**
- Layer-agnostic functional acceptance for UX-06. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
