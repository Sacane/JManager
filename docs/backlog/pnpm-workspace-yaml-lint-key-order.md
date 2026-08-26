## Observation

`pnpm lint` fails on `pnpm-workspace.yaml` with a `yaml/sort-keys` ESLint error, unrelated to any
frontend feature work:

```
C:\Users\johan\Documents\dev\JManager\pnpm-workspace.yaml
  12:1  error  Expected mapping keys to be in specified order. 'trustPolicyExclude' should be after 'trustPolicy'  yaml/sort-keys
```

## Location

`pnpm-workspace.yaml:12` (repo root, not `client/`).

## Expected behaviour

`pnpm lint` should be fully green so CI/local lint runs reliably signal only real regressions.

## Impact

- Every `pnpm lint` run currently reports 1 error regardless of what was actually changed,
  making it harder to notice a genuinely new lint regression introduced by a change.
- Low risk fix (the tool reports it as auto-fixable with `--fix`), but out of scope for any
  single feature/bugfix task, so left as a dedicated backlog item.
