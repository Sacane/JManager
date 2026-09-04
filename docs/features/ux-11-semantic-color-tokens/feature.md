# UX-11 — One colour per meaning

**Context**
Functional acceptance for UX-11, independent of any layer. No semantic colour token exists, so each screen invents its own shade of success, danger, income and expense.

**Acceptance Criteria**
Feature: One colour per meaning
  In order to read any screen the same way
  As a user
  I want a given meaning to always use the same colour

Scenario: A meaning maps to a single colour
  Given any screen of the application
  When an amount, a success or an error is displayed
  Then it uses the one colour defined for that meaning

Scenario: Colours stay readable in both themes
  Given a coloured amount or status
  When I switch between the light and the dark theme
  Then it stays legible against its background

**Notes**
- Layer-agnostic functional acceptance for UX-11. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
