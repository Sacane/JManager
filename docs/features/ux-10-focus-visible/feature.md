# UX-10 — Keyboard navigation is visible

**Context**
Functional acceptance for UX-10, independent of any layer. Several components suppress the focus outline without providing a replacement.

**Acceptance Criteria**
Feature: Keyboard navigation is visible
  In order to use the application without a mouse
  As a keyboard user
  I want to always see which element has focus

Scenario: The focused element is identifiable
  Given I navigate with the keyboard
  When the focus moves to a control
  Then I can see which control currently has focus

Scenario: Focus stays visible in both themes
  Given I navigate with the keyboard
  When I switch between the light and the dark theme
  Then the focus indicator stays visible in both

**Notes**
- Layer-agnostic functional acceptance for UX-10. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
