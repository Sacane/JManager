# UX-33 — Onboarding card fits a narrow phone

**Context**
`layouts/centercard.vue` sizes its card with `w-100` — a fixed 25rem, or 400px — and no upper bound relative to the viewport. On a 375px-wide phone the card is wider than the screen, so the consent, email verification, forced password change and not-found screens overflow horizontally.

**Acceptance Criteria**
Feature: Onboarding card fits a narrow phone
  In order to complete onboarding on any phone
  As a user
  I want the card to fit the screen width

Scenario: The card fits a narrow screen
  Given I browse on a 375 px wide screen
  When I open a screen using the centered card layout
  Then the card fits within the viewport

Scenario: The page does not scroll sideways
  Given I browse on a 375 px wide screen
  When I open a screen using the centered card layout
  Then the page does not scroll horizontally

Scenario: Wider screens are unaffected
  Given I browse on a desktop screen
  When I open a screen using the centered card layout
  Then the card keeps its usual width

**Notes**
- Layer-agnostic functional acceptance for UX-33. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
