# UX-27 — Choosing a valid password on the first try

**Context**
Functional acceptance for UX-27, independent of any layer. Four screens ask for a password without
stating any rule — and behind them, the server accepts a password of a single character.

Rescoped on 21 September 2026 from frontend only to **full-stack**: rules shown by the client over an API
that enforces none would be decoration. The policy now lives in the domain and applies to every way of
setting a password; the client reads it from the API. UX-27 ships with UX-21, whose reset form uses it.
The reveal control this issue first asked for was delivered by UX-47.

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

Scenario: The server refuses what the screen refuses
  Given I send a password that does not satisfy the rules without going through the screen
  When the server receives it
  Then it is refused, whatever the screen it was meant for

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
