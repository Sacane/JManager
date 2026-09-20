# Client Module — Move the dashboard onto UnoCSS shortcuts and tokens

**Context**
`pages/index.vue` carries 66 `style` attributes. Two are dynamic bindings that colour a tag from the
colour the user chose, and stay. The other 64 are static, and hold nine distinct values:

| Value | Occurrences |
|---|---|
| `color: var(--text-secondary)` | 26 |
| `color: var(--text-primary)` | 20 |
| `color: var(--text-tertiary)` | 6 |
| `background-color: var(--bg-tertiary)` | 6 |
| `background-color: var(--card-bg)` | 2 |
| the four remaining one-offs (muted text, a bottom border, the account selector, a chip) | 4 |

They already point at design tokens, so the colours themselves are right. What is wrong is where
they are written: an inline style beats every class, so no variant — `dark:`, `hover:`, a future
contrast fix on a token — can reach these blocks, and UnoCSS never sees them.

Repeated class + style combinations show where the page needs a name rather than a copy:

| Role | Repeats | Written as |
|---|---|---|
| Figure label of a stat card | 5 | `text-sm mb-2 font-medium` + secondary |
| Figure value of a stat card | 4 | `text-3xl font-extrabold mb-2` + primary |
| Figure hint under a value | 5 | `text-xs` + tertiary |
| Inset panel inside a card | 6 | `rounded-xl p-3` + `bg-tertiary` |
| Block title with its icon | 8 | **five different class lists** |

The eight block titles are the drift worth naming: `text-lg`/`text-xl`, `font-bold`/`font-semibold`,
`gap-2`/`gap-2.5`, four margin combinations — for one semantic role, `h3` inside a zone. They are
unified on the majority form, so three titles go from `text-xl` to `text-lg`. That is the one
visible change of this item, and it is deliberate.

The zone headings (`.zone-title`, h2) are untouched: they are a deliberately quiet uppercase
eyebrow, and UX-18 shipped them three days ago.

**Acceptance Criteria**
Feature: Dashboard styling through the design system

  In order to keep one appearance per role and let the tokens reach every block
  As a developer
  I want the dashboard styled by shortcuts and utilities rather than inline styles

Scenario: 1. No static inline style is left on the dashboard
  Given the dashboard is rendered
  When its markup is inspected
  Then no element carries a `style` attribute holding a design token

Scenario: 2. The tag colours stay dynamic
  Given a tag whose colour the user chose
  When the dashboard renders that tag
  Then its colour is still applied through a style binding

Scenario: 3. Block titles share one class
  Given the dashboard is rendered
  When its block titles are inspected
  Then each one carries the shared title shortcut

Scenario: 4. The repeated patterns become shortcuts
  Given the stat figures and the inset panels
  When their markup is inspected
  Then each role is expressed by one shortcut rather than a repeated class list

Scenario: 5. Nothing else about the page changes
  Given the tests covering the dashboard structure, the all-accounts mode and the quick actions
  When the migration is complete
  Then they pass unchanged

**Notes**
- Files: `pages/index.vue`, `unocss.config.ts`.
- New shortcuts: `block-title`, `kpi-label`, `kpi-value`, `kpi-hint`, `panel-sunken`, plus
  `text-label-strong` and `text-note`, found during the refactor pass — the strong line that opens
  a panel or an alert appears four times, and the small secondary line eleven times. They are named
  by role, not by page, so the other pages can adopt them when their turn comes.
- Shortcuts are generated in the `shortcuts` layer, ahead of the utilities layer, so a `text-[…]`
  utility still overrides a shortcut's colour. That is what lets the projection figure use
  `kpi-value` while colouring itself green or red.
- Values that appear once keep a utility with an arbitrary value — `text-[var(--text-muted)]` —
  rather than a shortcut nothing else would use.
- Priority P2 - Effort S - Frontend only. Last item of lot 5.
