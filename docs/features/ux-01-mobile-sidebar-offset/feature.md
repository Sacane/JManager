# UX-01 — Mobile menu button never covers page content

**Context**
Functional acceptance for UX-01, independent of any layer. On a phone the floating menu button currently overlaps page content on six of the seven pages using the sidebar layout.

**Acceptance Criteria**
Feature: Mobile menu button never covers page content
  In order to use the application on my phone
  As an authenticated user
  I want the page content to stay clear of the floating menu button

Scenario: Every control stays reachable on a phone
  Given I browse the application on a 375 px wide screen
  When I open any page of the application
  Then no title, button or link is hidden behind the floating menu button

Scenario: The desktop layout is unaffected
  Given I browse the application on a desktop screen
  When I open any page of the application
  Then the layout is unchanged

**Notes**
- Layer-agnostic functional acceptance for UX-01. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
