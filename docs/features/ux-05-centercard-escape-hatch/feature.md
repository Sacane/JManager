# UX-05 — Onboarding walls cannot be bypassed

**Context**
Functional acceptance for UX-05, independent of any layer. The shared layout displays a back arrow on screens the user is not allowed to leave.

**Acceptance Criteria**
Feature: Onboarding walls cannot be bypassed
  In order not to be sent into a redirect loop
  As a user going through a required step
  I want blocking screens to offer no way out

Scenario: A required step offers no escape
  Given I must accept the terms or set a new password before continuing
  When the screen is displayed
  Then no link invites me to leave it

Scenario: An informational screen keeps its exit
  Given I am on the email verification screen
  When the screen is displayed
  Then I can still return to the application

**Notes**
- Layer-agnostic functional acceptance for UX-05. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
