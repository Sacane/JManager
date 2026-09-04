# Legacy issue format in `docs/features/first-login-consent/`

**Observation**
The four issue files of the `first-login-consent` feature fail the format validator shipped with the
`create-issue` skill (`.claude/skills/create-issue/validate_issue_format.py`). They use markdown
headings (`## Context`, `## Acceptance Criteria`) where the current template expects bold markers
(`**Context**`, `**Acceptance Criteria**`), and wrap the Gherkin in a fenced code block.

**Exact location**
- `docs/features/first-login-consent/domain_first-login-consent.md`
- `docs/features/first-login-consent/infrastructure_first-login-consent.md`
- `docs/features/first-login-consent/application_first-login-consent.md`
- `docs/features/first-login-consent/client_first-login-consent.md`

These are the only four files out of 59 in `docs/features/` that do not validate.

**Expected behaviour**
`python .claude/skills/create-issue/validate_issue_format.py <file>` prints `VALID` for every file
under `docs/features/`.

**Impact**
Low. Purely documentary: the feature is already implemented. It only matters if the validator is ever
wired into CI, which would then fail on these legacy files.

**Found while**
Generating the UI/UX chantier issues (UX-01 to UX-27) and running the validator across the whole
`docs/features/` tree.
