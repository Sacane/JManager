# UX-20 — Knowing what to correct

**Context**
Functional acceptance for UX-20, independent of any layer. Validation failures are only reported through a toast, sometimes with a message that does not match the actual problem.

**Acceptance Criteria**
Feature: Knowing what to correct
  In order to fix a form quickly
  As a user filling a form
  I want the error shown next to the field that caused it

Scenario: An invalid field is pointed out
  Given I fill a form incorrectly
  When I submit it
  Then the error is displayed next to the field that caused it

Scenario: The message matches the actual problem
  Given a form with several possible errors
  When I submit it with one invalid field
  Then the message describes that field and no other

Scenario: Correcting clears the error
  Given an error is displayed on a field
  When I enter a valid value in it
  Then the error disappears without submitting again

**Notes**
- Layer-agnostic functional acceptance for UX-20. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
