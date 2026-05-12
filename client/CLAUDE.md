# Frontend Context — JManager Client

Loaded automatically when working in `client/`. For the full task workflow, use `/dev-frontend`.

---

## Stack

| Concern | Technology |
|---|---|
| Framework | Nuxt 4 (Vue 3) |
| Language | TypeScript |
| UI Components | PrimeVue 4 with Lara theme |
| Icons | Primeicons + Tabler (via Iconify) |
| Styling | UnoCSS + SASS |
| Charts | Chart.js + vue-chartjs |
| State | Pinia |
| i18n | @nuxtjs/i18n |
| Date utilities | date-fns |
| HTTP client | Axios |
| Testing | Vitest + @vue/test-utils + happy-dom |

---

## Architecture

```
client/
├── app.vue
├── nuxt.config.ts           # Nuxt config (PrimeVue, UnoCSS, i18n, color-mode)
├── unocss.config.ts         # Design tokens, shortcuts
├── components/              # Auto-imported reusable UI components
│   ├── card/                # Card variants
│   ├── dialog/              # Dialog components
│   └── ...
├── composables/             # Auto-imported — business logic + API calls
├── constants/               # Shared constants
├── layouts/                 # default, centercard, sidebar-layout
├── middleware/              # Route middleware (auth, admin)
├── pages/                   # File-based routing
├── plugins/                 # One-time app init registrations
├── stores/                  # Pinia stores (global state only)
├── types/                   # TypeScript type declarations
├── utils/                   # Pure utility functions (no Vue reactivity)
└── tests/
    ├── setup.ts             # Vitest global stubs for Nuxt auto-imports
    ├── components/
    ├── pages/
    └── unit/
```

---

## Non-Negotiable Rules

### Design system
- Use existing UnoCSS shortcuts from `unocss.config.ts` before writing custom classes.
- If the same pattern appears in two places and no shortcut exists, add one to `unocss.config.ts`.
- Dark mode: every component/style must work in both light and dark modes via `dark:` variants.
- Never hardcode hex colors — use PrimeVue CSS custom properties or UnoCSS design tokens.

### Components
- Search `components/` before creating anything new.
- If one almost fits, extend it — do not duplicate.
- Single root element preferred. Follow script → template structure.
- Always type props with TypeScript interface + `withDefaults`. No untyped `defineProps`.
- Type all emits.

### Composables and stores
- Composables are auto-imported — no manual import needed.
- **Prefer composables over stores** for feature-local state. Stores are for truly global state.
- When adding a new composable, immediately add its `vi.stubGlobal` stub to `tests/setup.ts`.

### TypeScript
- No `any`. Use `unknown` + type narrowing, generics, or proper interfaces.
- Define shared types in `types/index.d.ts` or a feature-specific `.d.ts`.

---

## Testing

| Target | File location |
|---|---|
| `components/Foo.vue` | `tests/components/Foo.spec.ts` |
| `pages/bar.vue` | `tests/pages/bar.spec.ts` |
| `composables/useFoo.ts` | `tests/unit/useFoo.spec.ts` |
| `utils/foo.ts` | `tests/unit/foo.spec.ts` |

- No real network calls in tests. Mock composables with `vi.stubGlobal`.
- PrimeVue components are not rendered in Vitest — stub them locally per spec.
- Run `pnpm test` after every change. Suite must be green before done.

---

## Reference Files

- `client/agents.frontend.md` — testing discipline and reuse rules
- `docs/agents/instructions/frontend.instructions.md` — full architecture and testing guidelines
- `/dev-frontend` — full frontend task workflow
