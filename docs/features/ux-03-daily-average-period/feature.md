# UX-03 — Trustworthy daily expense average

**Context**
Functional acceptance for UX-03, independent of any layer. The daily average is currently divided by a hardcoded 30 days whatever the selected period.

**Acceptance Criteria**
Feature: Trustworthy daily expense average
  In order to trust the figures on my dashboard
  As an authenticated user
  I want the daily average to match the period I selected

Scenario: The average matches the selected period
  Given I selected a period on my dashboard
  When I read the daily expense average
  Then it equals my expenses divided by the real number of days of that period

Scenario: Changing the period updates the average
  Given I am reading the daily average for one period
  When I switch to a longer period
  Then the average is recomputed against the new period length

**Notes**
- Layer-agnostic functional acceptance for UX-03. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
