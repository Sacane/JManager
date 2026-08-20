## pnpm-workspace.yaml fails `pnpm lint`

**Observation**: `pnpm lint` (client/) fails with a `yaml/sort-keys` error.

**Location**: `client/pnpm-workspace.yaml:12` — `trustPolicyExclude` is declared before `trustPolicy`.

**Expected behaviour**: `pnpm lint` should be green; YAML mapping keys should follow the order the linter expects (`trustPolicy` before `trustPolicyExclude`).

**Impact**: Low — auto-fixable with `eslint . --fix`. Blocks a fully green `pnpm lint` run, which could mask a real new lint error introduced later in CI.

**Origin**: Predates this session — file last touched in commit `368a31f4` (pnpm 11 migration). Not introduced by the PrimeVue v5→v4 revert done in this session.
