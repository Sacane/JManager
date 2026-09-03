# UX-12 — Stable income and expense colours

**Context**
Functional acceptance for UX-12, independent of any layer. Income is rendered blue on the booklet detail and green everywhere else.

**Acceptance Criteria**
Feature: Stable income and expense colours
  In order not to relearn the colour code on every page
  As an authenticated user
  I want income and expenses to keep the same colours everywhere

Scenario: The colour code does not change between pages
  Given an income amount and an expense amount
  When I compare them on the dashboard, on a booklet and on the regular transactions
  Then income always uses the same colour and expense always uses the same colour

Scenario: An empty amount is neutral
  Given a transaction that has no income value
  When the income column is displayed for it
  Then the empty cell is neutral rather than coloured

**Notes**
- Layer-agnostic functional acceptance for UX-12. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
