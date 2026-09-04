# UX-29 — Tag page back on the brand palette

**Context**
The primary action of the tag page is styled with an indigo gradient (rgba(99,102,241) to rgba(79,70,229)) that belongs to no part of the brand palette, and TagCard falls back to #6366f1 for its selection and sub-tag surfaces. The most prominent button of the page is therefore off-brand.

**Acceptance Criteria**
Feature: Tag page back on the brand palette
  In order to recognise the product on every screen
  As a user
  I want the tag page to use the same accent colour as the rest of the application

Scenario: The primary action uses the brand accent
  Given I open the tag page
  When I look at the button that creates a tag
  Then it uses the same accent colour as the primary actions of the other pages

Scenario: Selection surfaces use the brand accent
  Given I select a tag or a sub-tag
  When the card is highlighted
  Then the highlight uses the brand accent rather than an unrelated colour

Scenario: The page stays readable in both themes
  Given the tag page is displayed
  When I switch between the light and the dark theme
  Then the accent stays legible against its background

**Notes**
- Layer-agnostic functional acceptance for UX-29. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
