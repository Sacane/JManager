# UX-09 — No unreachable or placeholder screen

**Context**
Functional acceptance for UX-09, independent of any layer. An unimplemented user route is reachable in production and four components are never used.

**Acceptance Criteria**
Feature: No unreachable or placeholder screen
  In order not to land on an unfinished screen
  As a user
  I want every reachable route to be a real page

Scenario: An unimplemented route behaves like an unknown one
  Given a route that was never implemented
  When I navigate to it
  Then the not-found page is displayed instead of a placeholder

Scenario: The application still works
  Given the unused components were removed
  When I use the application
  Then every screen behaves as before

**Notes**
- Layer-agnostic functional acceptance for UX-09. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
