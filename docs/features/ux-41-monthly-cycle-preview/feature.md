# UX-41 — Knowing which period each account's cycle covers

**Context**
Functional acceptance for UX-41, independent of any layer. Merged with the Trello card "cycle mensuel
du compte", which reported that the settings page does not make clear which cycle belongs to which
account. On that page, each account's name can be cut off, the settings of every account look alike,
and nothing states the dates a cycle actually covers. The help text also states a rule that is false
for half the possible start days.

**Acceptance Criteria**
Feature: Knowing which period each account's cycle covers
  In order to configure the monthly cycle of the right account with confidence
  As an authenticated user
  I want each account's cycle clearly attached to that account and shown as real dates

Scenario: Each cycle is unmistakably attached to its account
  Given I have several accounts with monthly cycles
  When I open the monthly cycle settings
  Then each account's name is shown in full next to its own cycle settings

Scenario: The period a cycle covers is shown as dates
  Given I look at the cycle of an account
  When the settings are displayed
  Then I see the exact first and last day that the cycle covers for the current month

Scenario: The preview follows my changes before I save
  Given I am editing the start day of an account's cycle
  When I pick a different day
  Then the displayed period updates immediately, without saving

Scenario: The explanation matches what the application computes
  Given I read the explanation of how cycles work
  When I compare it with the periods the application displays
  Then the explanation agrees with them for every start day

**Notes**
- Layer-agnostic functional acceptance for UX-41. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
