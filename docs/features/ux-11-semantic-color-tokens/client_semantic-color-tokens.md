# Client Module — Introduce semantic colour tokens

**Context**
`assets/css/variables.css` defines the brand palette and the surfaces but no semantic colour for
success, danger, warning, income or expense. As a result each file invents its own shade: five
different greens and five different reds were found across the client (`text-green-500`, `#10b981`,
`text-emerald-600`, `#009CFE`, `text-green-400` for the positive side). Semantic tokens are the
prerequisite for the colour unification (UX-12) and the dashboard rework (UX-18).

**Acceptance Criteria**
Feature: Semantic colour tokens
  In order to keep one visual language across the application
  As a developer
  I want a single token per semantic colour, defined for both themes

Scenario: 1. The tokens exist for both themes
  Given the stylesheet of design tokens
  When I inspect the light and the dark palettes
  Then success, danger, warning, info, income and expense are defined in both

Scenario: 2. Shortcuts expose the amount colours
  Given the UnoCSS configuration
  When I use the positive and negative amount shortcuts
  Then they resolve to the income and expense tokens

Scenario: 3. Contrast is sufficient in both themes
  Given a monetary amount rendered with a semantic token
  When I measure the contrast against the card background
  Then the ratio meets at least 4.5 to 1 in light and in dark mode

**Notes**
- Files: `assets/css/variables.css`, `unocss.config.ts`.
- Blocks UX-12 and UX-18. Do it before any page rework.
- Priority P1 - Effort S - Frontend only.
