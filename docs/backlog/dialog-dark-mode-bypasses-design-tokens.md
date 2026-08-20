# Dialog components hardcode a different dark palette instead of using the design tokens

**Observation**

`assets/css/variables.css` defines the full token set twice — once on `:root`, once under `.dark` —
so any rule written as `background: var(--card-bg)` already themes correctly in both modes.

Several dialog components nevertheless ship a second, hand-written `.dark` block with literal hex
values, and those values come from a **different palette** than the design system: Tailwind *gray*
(`#111827`, `#1f2937`, `#374151`, `#f3f4f6`, `#9ca3af`) instead of the project's *slate* tokens
(`--card-bg: #1e293b`, `--bg-tertiary: #334155`, `--border-color: #334155`, `--text-primary: #f1f5f9`).

In dark mode these dialogs therefore do not match the cards and surfaces around them, and the
hardcoded values silently drift from the tokens whenever the palette is adjusted.

**Exact location**

- `client/components/booklet/BookletConfirmPreviewDialog.vue` — the `.dark .preview-confirm-*` rules
  in both the scoped and the global `<style>` blocks, plus the `.dark :deep(.preview-confirm-field …)`
  input overrides
- Worth grepping `client/components/**/*.vue` for `\.dark .*#[0-9a-f]{6}` to catch the same pattern
  elsewhere (`CsvImportDialog.vue`, `RegularTransactionDialogCard.vue` are likely candidates)

**Expected behaviour**

Delete the `.dark` hex blocks and let the token layer do the work, as
`client/components/booklet/BookletRegenerateTransactionsDialog.vue` now does. If dialogs genuinely
need a surface darker than `--card-bg`, introduce a dedicated token (e.g. `--dialog-bg`) defined for
both themes in `variables.css` and point every dialog at it — rather than repeating literals per
component.

**Impact**

- Violates the non-negotiable design-system rule "never hardcode hex colours — use PrimeVue CSS
  custom properties or UnoCSS design tokens".
- Visual inconsistency in dark mode between dialogs and the rest of the UI, and between dialogs that
  do and do not carry the override.
- A palette change in `variables.css` will not reach these components.

**Related: duplicated dialog chrome**

The same files also repeat a near-identical block of PrimeVue passthrough styling (`root`, `mask`,
`header`, `title`, `closeButton`, `content` classes with the same `!important` structure). Once the
colours come from tokens, that chrome is a good candidate for extraction into a shared themed dialog
wrapper component, removing the copy-paste entirely.

Noted while building the selective-regeneration dialog: the new component was written against the
tokens rather than copying the sibling's override, so the two now differ slightly in dark mode until
this is resolved.
