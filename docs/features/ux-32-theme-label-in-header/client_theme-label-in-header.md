# Client Module — Remove the theme label from the shared header

**Context**
`components/NHeader.vue` renders `{{ isDark ? t('theme.dark') : t('theme.light') }}` inside a `text-8` paragraph. It is the largest text on the consent and email verification screens, and it says nothing the sun/moon toggle beside it does not already say. The i18n keys it uses resolve to nothing either, since no locale file exists.

**Acceptance Criteria**
Feature: Shared header without the theme label
  In order to keep the header to actual controls
  As a developer
  I want the theme name removed from NHeader

Scenario: The label is gone
  Given the NHeader component is rendered
  When I read its text content
  Then it contains neither the light nor the dark theme name

Scenario: The controls are preserved
  Given the NHeader component is rendered
  When I inspect its controls
  Then the locale switch and the theme toggle are both present

Scenario: The toggle still reports its state accessibly
  Given the NHeader component is rendered
  When I inspect the theme toggle
  Then it carries an accessible name describing what it does

**Notes**
- Files: `components/NHeader.vue`.
- The toggle currently has no accessible name at all; give it one while the label it leaned on is
  removed, otherwise the control becomes unlabelled for assistive technology.
- `useCustomI18n` may become unused in this component once the label is gone — check before leaving
  the import in place. The wider i18n decision is UX-31.
- Priority P2 - Effort XS - Frontend only.
