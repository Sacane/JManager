# Client Module — Put the tag page back on the brand palette

**Context**
`pages/tag/index.vue` styles `.modern-fab` with an indigo gradient and indigo shadows, and `components/tag/TagCard.vue` uses `var(--p-primary-color, #6366f1)` in five places. Neither colour exists in the brand palette, which is built on the violet #6508CC. The design system already exposes `--primary` and its tints, so the fallbacks are unnecessary.

**Acceptance Criteria**
Feature: Tag page uses the design system accent
  In order to keep one accent colour across the product
  As a developer
  I want the tag page to reference the primary token instead of raw indigo values

Scenario: The create action references the primary token
  Given the tag page stylesheet
  When I inspect the primary action
  Then its background and shadow derive from the primary token

Scenario: TagCard references the primary token
  Given the TagCard stylesheet
  When I inspect the selection and sub-tag surfaces
  Then none of them falls back to a hardcoded indigo value

Scenario: No off-palette colour remains on the page
  Given the tag page and TagCard sources
  When I search them for raw indigo values
  Then none is found

**Notes**
- Files: `pages/tag/index.vue` (`.modern-fab`), `components/tag/TagCard.vue`.
- Replace `var(--p-primary-color, #6366f1)` with `var(--primary)`; the PrimeVue variable is already
  aliased to the brand violet by the theme preset in `nuxt.config.ts`.
- Priority P2 - Effort XS - Frontend only.
