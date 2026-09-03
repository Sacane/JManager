# UX-19 — Anticipating my recurring charges

**Context**
Functional acceptance for UX-19, independent of any layer. The regular transactions page never states when the next occurrence falls nor the monthly commitment.

**Acceptance Criteria**
Feature: Anticipating my recurring charges
  In order to plan my month
  As an authenticated user
  I want to know what falls next and how much I am committed to

Scenario: I know what is coming
  Given I have regular transactions
  When I open the page
  Then each entry shows the date of its next occurrence

Scenario: I know my monthly commitment
  Given I have regular transactions
  When I open the page
  Then a summary shows my monthly recurring expenses and my monthly recurring income

Scenario: An ended recurrence is marked
  Given a recurrence that has ended
  When I look at it
  Then it is marked as ended rather than showing a next occurrence

**Notes**
- Layer-agnostic functional acceptance for UX-19. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
