# UX-18 — Understanding my situation at a glance

**Context**
Functional acceptance for UX-18, independent of any layer. The dashboard stacks about fifteen blocks of equal visual weight, several of which repeat the same figures.

**Acceptance Criteria**
Feature: Understanding my situation at a glance
  In order to know where I stand in a few seconds
  As an authenticated user
  I want the dashboard organised around a clear reading order

Scenario: The dashboard has a reading order
  Given I open my dashboard
  When the page is displayed
  Then the information is grouped into a few identifiable zones

Scenario: No figure is repeated
  Given I open my dashboard
  When I read the page
  Then each key figure appears in exactly one place

Scenario: No label promises something that does not exist
  Given I open my dashboard
  When I read the header
  Then it never announces a view the application cannot show

**Notes**
- Layer-agnostic functional acceptance for UX-18. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
