# UX-27 — Choosing a valid password on the first try

**Context**
Functional acceptance for UX-27, independent of any layer. Three screens ask for a password without stating any rule, and none offers a reveal control.

**Acceptance Criteria**
Feature: Choosing a valid password on the first try
  In order not to be rejected after submitting
  As a user setting a password
  I want the rules and my progress shown as I type

Scenario: I know the rules before typing
  Given I am asked for a new password
  When the form is displayed
  Then the rules my password must satisfy are shown

Scenario: I get feedback while typing
  Given I am typing a new password
  When I type
  Then satisfied rules are marked and a strength indicator updates

Scenario: I can check what I typed
  Given I typed a password
  When I use the reveal control
  Then I can read it in clear text

Scenario: A non compliant password is refused early
  Given I typed a password that does not satisfy the rules
  When I try to submit
  Then the submission is blocked and the unmet rules are highlighted

**Notes**
- Layer-agnostic functional acceptance for UX-27. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
