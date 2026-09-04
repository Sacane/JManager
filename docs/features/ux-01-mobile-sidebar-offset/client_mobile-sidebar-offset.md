# Client Module — Stop the mobile menu button from covering page content

**Context**
The sidebar toggle button is rendered as `position: fixed; top: 1rem; left: 1rem` at 44x44 px below
769 px (`components/app-sidebar.vue`). Only `pages/tag/index.vue` compensates for it, with a local
`margin-top: 4rem`. On the six other pages using the `sidebar-layout`, the button overlaps the page
content: on `pages/booklet/[id].vue` it covers the back button of `BookletPageHeader`, making the
"return to booklets" action unreachable on a phone.
The offset must live in the layout, not be duplicated per page.

**Acceptance Criteria**
Feature: Mobile menu button never overlaps page content
  In order to use every page on a phone
  As an authenticated user
  I want the page content to start below the floating menu button

Scenario: 1. Page content is pushed below the toggle button on mobile
  Given the viewport width is 375 px
  When I open any page using the sidebar layout
  Then the page content starts below the floating menu button
  And no interactive element is covered by it

Scenario: 2. The back button of a booklet stays reachable
  Given the viewport width is 375 px
  When I open the detail page of a booklet
  Then the back button of the page header is fully visible and clickable

Scenario: 3. No offset is applied on desktop
  Given the viewport width is 1280 px
  When I open any page using the sidebar layout
  Then no extra top offset is applied to the main content

Scenario: 4. The tag page is not offset twice
  Given the viewport width is 375 px
  When I open the tag page
  Then the content is offset exactly once, by the layout only

**Notes**
- Files: `layouts/sidebar-layout.vue` (add the offset), `pages/tag/index.vue` (remove the local `margin-top: 4rem`).
- Affected pages: dashboard, booklets, booklet detail, regular transactions, settings, admin, tags.
- Priority P0 - Effort S - Frontend only.
- Source: `docs/technical/ux-design-review/UX_DESIGN_REVIEW.md` section 2.5.
