# UX-23 — A budget that follows me

**Context**
Functional acceptance for UX-23, shared by the domain, infrastructure, application and client issues. The budget target lives in browser storage, so it is lost across devices.

**Acceptance Criteria**
Feature: A budget that follows me
  In order to track the same budget everywhere
  As an authenticated user
  I want my budget target stored with my account

Scenario: My budget is the same on every device
  Given I set a budget target on one device
  When I open my dashboard on another device
  Then the same target and the same alerts are displayed

Scenario: An existing local budget is not lost
  Given a budget target saved by a previous version in my browser
  When I open my dashboard
  Then that target is attached to my account

Scenario: An invalid target is refused
  Given I am setting a budget target
  When I enter a negative amount
  Then it is refused and my previous target is kept

**Notes**
- Layer-agnostic functional acceptance for UX-23. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
