# UX-44 — Seeing all my accounts at once

**Context**
Functional acceptance for UX-44, independent of any layer. The dashboard header could display "Tous
les comptes", yet no such view existed: the account selector only listed individual booklets and
always selected one. The label only appeared when loading had failed.

**Acceptance Criteria**
Feature: Seeing all my accounts at once
  In order to know my overall situation without adding figures up myself
  As an authenticated user
  I want a dashboard view that aggregates every account

Scenario: I can choose to see all my accounts
  Given I have several accounts
  When I open the account selector on the dashboard
  Then an option lets me see all my accounts together

Scenario: The aggregated view covers every account
  Given I chose to see all my accounts
  When the dashboard is displayed
  Then its balance, income, expenses and upcoming entries cover every account

Scenario: The aggregated view uses calendar months
  Given my accounts have different monthly cycles
  When I look at a month with all my accounts selected
  Then the period runs from the first to the last day of that calendar month

Scenario: Account-specific features are not offered for all accounts at once
  Given I chose to see all my accounts
  When the dashboard is displayed
  Then nothing asks me to act on "all accounts" where a single account is required

Scenario: My choice is remembered
  Given I chose to see all my accounts
  When I come back to the dashboard later
  Then it still shows all my accounts

**Notes**
- Layer-agnostic functional acceptance for UX-44. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
