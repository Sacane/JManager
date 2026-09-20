# `pnpm lint` fails on `client/pnpm-workspace.yaml` key order

## Observation

`pnpm lint` from `client/` exits 1 on a file no current task touches:

```
client/pnpm-workspace.yaml
  12:1  error  Expected mapping keys to be in specified order.
               'trustPolicyExclude' should be after 'trustPolicy'  yaml/sort-keys
```

The file is unmodified in git and its last commit is `368a31f4` (20 August 2026). The error
appeared without the file changing, so it comes from a newer `eslint-plugin-yaml` /
`@nuxt/eslint` resolution: the rule now expects the pnpm workspace schema order rather than
alphabetical order, and `trustPolicyExclude` is listed while `trustPolicy` does not exist.

## Location

`client/pnpm-workspace.yaml:12`

## Expected behaviour

`pnpm lint` is green on a clean checkout, so that a real error in changed code is not lost in a
pre-existing failure.

## Impact

Low on correctness, real on the workflow: "full suite and lint green" can no longer be asserted
from the exit code alone, and every task has to re-check that the only failure is this one.

## Fix candidates

Reorder the keys to match whatever order the rule now specifies, or scope the rule off for this
file. Both need the rule's actual key order checked first — do not guess it.
