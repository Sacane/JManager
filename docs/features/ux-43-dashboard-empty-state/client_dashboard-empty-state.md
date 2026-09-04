# Client Module — Replace the empty dashboard with an onboarding state

**Context**
`pages/index.vue` renders its whole layout regardless of `booklets.length`. With none, the KPI cards show 0.00 EUR, the charts render with no dataset and the `select` has no option, while the header still claims a period and a projection. The empty states that already exist inside the booklets, tags and upcoming panels do not cover the page-level case.

**Acceptance Criteria**
Feature: Dashboard onboarding state
  In order not to render a dashboard over no data
  As a developer
  I want the page to switch to an onboarding state when no booklet exists

Scenario: The onboarding state replaces the dashboard
  Given the booklet list is empty after loading
  When the dashboard renders
  Then the indicators, charts and period controls are not rendered

Scenario: The onboarding state offers the creation dialog
  Given the dashboard is in its onboarding state
  When I activate the primary action
  Then the booklet creation dialog opens

Scenario: Creating a booklet leaves the onboarding state
  Given the dashboard is in its onboarding state
  When a booklet is created
  Then the dashboard renders its usual content

Scenario: Loading is not mistaken for emptiness
  Given the dashboard data is still loading
  When the page renders
  Then the loading state is shown rather than the onboarding state

**Notes**
- Files: `pages/index.vue`.
- Guard on the loaded state, not only on `booklets.length`, so the first paint does not flash the
  onboarding screen.
- Reuse `BookletBookingDialog`, already wired on this page.
- Priority P2 - Effort S - Frontend only.
