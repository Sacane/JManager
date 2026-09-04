# Client Module — Restore a visible keyboard focus indicator

**Context**
`components/booklet/BookletPageHeader.vue` and `components/booklet/BookletFilterActionBar.vue`
declare `outline: none !important` on their inputs and selects without providing any replacement, so
keyboard navigation across the booklet filter bar is invisible. The whole client contains only two
uses of `:focus-visible`. A global focus style based on the brand token is needed.

**Acceptance Criteria**
Feature: Visible keyboard focus
  In order to navigate without a mouse
  As a keyboard user
  I want to always see which element has focus

Scenario: 1. The booklet filter bar shows focus
  Given I am on the booklet detail page
  When I move the focus through the filter bar with the keyboard
  Then every focused control displays a visible focus ring

Scenario: 2. The focus ring is visible in dark mode
  Given the dark theme is active
  When I move the focus through the filter bar with the keyboard
  Then the focus ring stays visible against the dark background

Scenario: 3. Mouse interaction does not display the focus ring
  Given I am on the booklet detail page
  When I click a control with the mouse
  Then no focus ring is displayed

**Notes**
- Files: `components/booklet/BookletPageHeader.vue`, `components/booklet/BookletFilterActionBar.vue`, `assets/css/reset.css`.
- Suggested global rule: `:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }`.
- Priority P0 - Effort S - Frontend only.
