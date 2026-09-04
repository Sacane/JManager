# UX-30 — One page shell across the application

**Context**
Four pages declare their own page wrapper and header: the dashboard uses inline styles, the booklets, tags and admin pages each define their own SCSS classes. The result is four rounded corner radii, three shadow depths and two heading scales for what is visually the same page frame.

**Acceptance Criteria**
Feature: One page shell across the application
  In order to perceive the screens as one product
  As a user
  I want every page to share the same frame and heading treatment

Scenario: Pages share the same frame
  Given I move between the main pages of the application
  When I compare their background, padding and card corners
  Then they are identical

Scenario: Page headers share the same treatment
  Given I move between the main pages of the application
  When I compare their titles and subtitles
  Then they use the same size and spacing

Scenario: Both themes stay consistent
  Given I switch between the light and the dark theme
  When I move between the main pages
  Then the shared frame holds in both

**Notes**
- Layer-agnostic functional acceptance for UX-30. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
