# Client Module — Extract page shell and header shortcuts

**Context**
`pages/index.vue` repeats `rounded-2xl p-6 shadow-lg` with an inline `background-color` twelve times, while `booklet/index`, `tag/index` and `admin/index` each define `.page-header`, a title and a subtitle in their own scoped SCSS. `unocss.config.ts` already holds a `card` shortcut that only the legal pages use. The design system rule says to add a shortcut when a pattern appears twice.

**Acceptance Criteria**
Feature: Shared page shell shortcuts
  In order to stop redefining the same frame per page
  As a developer
  I want page shell and header patterns available as shortcuts

Scenario: The shortcuts exist
  Given the UnoCSS configuration
  When I inspect the shortcuts
  Then a page shell, a page header and a stat card shortcut are defined

Scenario: The dashboard uses them
  Given the dashboard page
  When I inspect its cards
  Then they reference the shared shortcuts instead of repeating utility strings

Scenario: The page frames converge
  Given the dashboard, the booklets, the tags and the admin pages
  When I compare the corner radius and padding of their frames
  Then they resolve to the same values

Scenario: No visual regression on the existing pages
  Given the pages migrated to the shortcuts
  When the test suite runs
  Then every existing page test still passes

**Notes**
- Files: `unocss.config.ts`, `pages/index.vue`, `pages/booklet/index.vue`, `pages/tag/index.vue`,
  `pages/admin/index.vue`.
- Reuse and extend the existing `card` shortcut rather than adding a parallel one.
- This is the prerequisite that keeps UX-28 (dashboard migration) from inventing its own frame.
- Priority P2 - Effort M - Frontend only.
