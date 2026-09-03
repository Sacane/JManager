# UX-07 — Correct French wording

**Context**
Functional acceptance for UX-07, independent of any layer. Several user-facing labels are missing accents or contain grammar mistakes.

**Acceptance Criteria**
Feature: Correct French wording
  In order to trust the product
  As a French-speaking user
  I want every label to be correctly written

Scenario: No visible spelling mistake
  Given I browse the application
  When I read any label, hint or confirmation message
  Then it is correctly spelled and accented

**Notes**
- Layer-agnostic functional acceptance for UX-07. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
