# UX-14 — Analysing an arbitrary period

**Context**
Functional acceptance for UX-14, independent of any layer. Booklet navigation is locked to a single month even though the API already accepts a date range.

**Acceptance Criteria**
Feature: Analysing an arbitrary period
  In order to analyse a period that is not a calendar month
  As an authenticated user
  I want to choose the date range shown for a booklet

Scenario: I choose my own period
  Given I am looking at a booklet
  When I select a start date and an end date
  Then the transactions and the balances shown cover exactly that period

Scenario: I return to the monthly view
  Given a custom date range is applied
  When I clear the range
  Then the booklet returns to the selected month

Scenario: An impossible range is refused
  Given I am choosing a date range
  When I pick an end date earlier than the start date
  Then I am told the range is invalid and nothing is loaded

**Notes**
- Layer-agnostic functional acceptance for UX-14. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
