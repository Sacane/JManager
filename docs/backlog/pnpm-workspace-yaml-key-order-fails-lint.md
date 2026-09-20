# `pnpm lint` fails on `client/pnpm-workspace.yaml` key order

> Consolidated on 20 September 2026 from three separate notes describing this same finding
> (`pnpm-workspace-yaml-key-order-lint-error.md`, `pnpm-workspace-yaml-lint-key-order.md`), written
> in three different sessions. One of them located the file at the repository root; it lives in
> `client/`.

## Observation

`pnpm lint` from `client/` exits 1 on a file no current task touches:

```
client/pnpm-workspace.yaml
  12:1  error  Expected mapping keys to be in specified order.
               'trustPolicyExclude' should be after 'trustPolicy'  yaml/sort-keys
```

The file is unmodified in git and its last commit is `368a31f4` (20 August 2026, the pnpm 11
migration). The error appeared without the file changing, so it comes from a newer
`eslint-plugin-yaml` / `@nuxt/eslint` resolution: the rule now expects the pnpm workspace schema
order rather than alphabetical order, and it asks for `trustPolicyExclude` to follow a `trustPolicy`
key the file does not declare at all.

## Location

`client/pnpm-workspace.yaml:12`

## Expected behaviour

`pnpm lint` is green on a clean checkout, so that a real error in changed code is not lost in a
pre-existing failure.

## Impact

Low on correctness, real on the workflow: "full suite and lint green" can no longer be asserted
from the exit code alone, and every task has to re-check that the only failure is this one. It has
now been rediscovered and re-filed three times, which is the cost of leaving it.

## Fix candidates

ESLint reports the rule as auto-fixable, so `eslint . --fix` is the first thing to try — but check
what it produces before committing it, since the order it wants is the plugin's, not alphabetical.
Otherwise, scope the rule off for this file.
