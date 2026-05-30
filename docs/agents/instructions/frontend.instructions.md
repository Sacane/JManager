---
description: 'Frontend architecture, stack, testing, and component guidelines for the JManager client'
applyTo: 'client/**'
---

# Frontend Instructions

Guidelines for all frontend work in the JManager `client/` directory.

---

## Stack

| Concern         | Technology                          |
|-----------------|-------------------------------------|
| Framework       | [Nuxt 4](https://nuxt.com/) (Vue 3) |
| Language        | TypeScript                          |
| UI Components   | [PrimeVue 4](https://primevue.org/) with Lara theme |
| Icons           | Primeicons + Tabler (via Iconify)   |
| Styling         | [UnoCSS](https://unocss.dev/) + SASS |
| Charts          | Chart.js + vue-chartjs              |
| State           | [Pinia](https://pinia.vuejs.org/)   |
| i18n            | @nuxtjs/i18n                        |
| Date utilities  | date-fns                            |
| HTTP client     | Axios                               |
| Testing         | [Vitest](https://vitest.dev/) + [@vue/test-utils](https://test-utils.vuejs.org/) + happy-dom |

---

## Project Architecture

```
client/
├── app.vue                  # App root
├── nuxt.config.ts           # Nuxt configuration (PrimeVue, UnoCSS, i18n, color-mode)
├── assets/                  # Static CSS and background assets
├── components/              # Reusable UI components (auto-imported by Nuxt)
│   ├── card/                # Card-variant components
│   ├── dialog/              # Dialog components
│   ├── frequency-part/      # Frequency selector sub-components
│   └── locale/              # Locale-specific UI helpers
├── composables/             # Auto-imported composables (business logic + API calls)
├── constants/               # Shared constants (e.g. loading scopes)
├── layouts/                 # Nuxt layout files (default, centercard, sidebar-layout)
├── middleware/              # Nuxt route middleware (auth, admin)
├── pages/                   # File-based routing pages
├── plugins/                 # Nuxt plugins
├── stores/                  # Pinia stores
├── types/                   # Global TypeScript type declarations
├── utils/                   # Pure utility functions (no Vue reactivity)
└── tests/
    ├── setup.ts             # Global Vitest setup — Nuxt auto-import stubs
    ├── components/          # Unit tests for components
    ├── pages/               # Unit tests for pages
    └── unit/                # Unit tests for composables and utilities
```

### Key Conventions

- **Components** live in `components/` and are auto-imported by Nuxt. No manual import needed in Vue files.
- **Composables** live in `composables/` and are auto-imported. They encapsulate API calls and business-facing reactive logic.
- **Utils** in `utils/` are pure functions with no Vue reactivity — keep them framework-agnostic.
- **Stores** (Pinia) are used for global shared state only. Prefer composables for feature-local state.
- **Layouts** define the page shell. Pages declare their layout via `definePageMeta({ layout: '...' })`.

---

## Component Duplication Policy

> Before creating any new UI element, always check whether an existing component already covers the need.

### Rules

1. **Search first.** Before writing any new component or inline template, scan `components/` for an existing match.
2. **No inline duplication.** If the same markup/logic appears in two or more places, extract it into a shared component.
3. **Refactor before adding.** If an existing component almost fits but is missing a small feature, extend it rather than duplicating it.
4. **Create the abstraction first.** When duplication is detected during implementation, stop and create (or generalize) the shared component before continuing with the feature.
5. **Sub-components go in sub-folders.** If a component is only meaningful in the context of a parent feature, place it in the parent's sub-folder (e.g. `components/dialog/`, `components/card/`).

---

## Writing Unit Tests

### Location

| Test target         | Test file location                         |
|---------------------|--------------------------------------------|
| `components/Foo.vue`| `tests/components/Foo.spec.ts`             |
| `pages/bar.vue`     | `tests/pages/bar.spec.ts`                  |
| `composables/useFoo`| `tests/unit/useFoo.spec.ts`                |
| `utils/foo.ts`      | `tests/unit/foo.spec.ts`                   |

### Test Structure

Use `describe` / `it` blocks. Group by component name at the top level, then by behaviour scenario:

```typescript
import { mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import MyComponent from '../../components/MyComponent.vue'

describe('components/MyComponent', () => {
  describe('when the prop X is set', () => {
    it('renders the expected output', () => {
      const wrapper = mount(MyComponent, { props: { x: 'value' } })
      expect(wrapper.text()).toContain('value')
    })
  })

  it('emits an event on click', async () => {
    const wrapper = mount(MyComponent, { props: { x: 'value' } })
    await wrapper.trigger('click')
    expect(wrapper.emitted('my-event')).toBeTruthy()
  })
})
```

### Handling Nuxt Auto-Imports

Nuxt auto-imports Vue primitives and composables at runtime — these are **not** available in the Vitest environment by default.

- All globally required stubs are declared in `tests/setup.ts` via `vi.stubGlobal`.
- **When a new composable is added**, add its stub to `tests/setup.ts` immediately so existing tests continue to pass.
- For composables that call an API, mock them in-test using `vi.stubGlobal` or `vi.mock`.

### Stubbing PrimeVue and Third-Party Components

PrimeVue components are not rendered in the test environment. Define lightweight inline stubs locally in the spec file when testing components that use them:

```typescript
const ButtonStub = {
  name: 'Button',
  props: ['label', 'disabled'],
  template: `<button :disabled="disabled">{{ label }}</button>`,
}

const wrapper = mount(MyComponent, {
  global: { stubs: { Button: ButtonStub } },
})
```

### Mocking API Calls

Do not make real network calls in tests. Mock the composable or the axios instance:

```typescript
vi.stubGlobal('useMyComposable', () => ({
  data: ref([{ id: 1, name: 'Item' }]),
  load: vi.fn(),
}))
```

---

## Running Tests

All commands must be run from the `client/` directory:

| Command                    | Purpose                               |
|----------------------------|---------------------------------------|
| `pnpm test`                | Run the full test suite once          |
| `pnpm test:watch`          | Run tests in watch mode               |
| `pnpm test:coverage`       | Run tests and generate coverage report|

> Always run `pnpm test` after any code change and before considering a task complete. The suite must be fully green.

---

---

## Feature Flag Integration

Feature flags use the **`FeatureGate` wrapper component** — the frontend equivalent of the
backend `FeatureFlagCommandBus` decorator. Pages and components never call `isEnabled()`
inline; they delegate the check to `FeatureGate`.

### Gating a UI section

Wrap the content in `<FeatureGate>`. The component renders the default slot when the flag is
enabled, the named `fallback` slot (optional) when it is disabled, and nothing if no fallback
is provided:

```vue
<script setup lang="ts">
import { FEATURE_KEYS } from '~/constants/featureKeys'  // not auto-imported
</script>

<template>
  <!-- Flag-gated section — no isEnabled() call in the page -->
  <FeatureGate :feature="FEATURE_KEYS.USER_REGISTRATION">
    <button @click="switchMode('register')">S'inscrire</button>

    <template #fallback>
      <!-- optional: shown when flag is disabled -->
    </template>
  </FeatureGate>
</template>
```

`FeatureGate` auto-imports like any other component in `components/`. `FEATURE_KEYS` is in
`client/constants/featureKeys.ts` and requires an **explicit import** (it is not
auto-imported by Nuxt).

### Adding a new flag key

1. Add the key to `FEATURE_KEYS` and `FEATURE_KEY_LABELS` in
   `client/constants/featureKeys.ts`. The key string must exactly match the backend `FeatureKey`
   enum name.

### Testing components that use FeatureGate

`shallowMount` stubs `FeatureGate` as an empty component — slot content will not render.
Provide a functional stub that honours the `isEnabled` mock:

```typescript
const FeatureGateStub = {
  name: 'FeatureGate',
  props: ['feature'],
  setup(props: { feature: string }) {
    const enabled = computed(() => isEnabledMock(props.feature))
    return { enabled }
  },
  template: '<slot v-if="enabled" /><slot v-else name="fallback" />',
}

// in mountComponent():
shallowMount(MyPage, {
  global: { stubs: { FeatureGate: FeatureGateStub, ... } },
})
```

### What NOT to do

```vue
<!-- ❌ inline check — couples the page to the flag mechanism -->
<div v-if="isEnabled(FEATURE_KEYS.USER_REGISTRATION)">...</div>

<!-- ✅ declarative wrapper — page only declares intent -->
<FeatureGate :feature="FEATURE_KEYS.USER_REGISTRATION">...</FeatureGate>
```

---

## References

- [Nuxt docs](https://nuxt.com/docs)
- [PrimeVue docs](https://primevue.org/components/)
- [Vue Test Utils docs](https://test-utils.vuejs.org/)
- [Vitest docs](https://vitest.dev/)
- [UnoCSS docs](https://unocss.dev/)
- `client/agents.frontend.md` — complementary in-repo frontend agent rules
