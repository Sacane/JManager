---
description: Structured protocol to investigate, reproduce, locate, and fix a bug
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Bug Investigation

Structured investigation from symptom to verified fix. Follow this protocol exactly.

Reference files:
- `docs/agents/instructions/development-workflow.md` — TDD cycle (Phase 4)
- `docs/agents/instructions/testing-guidelines.md` — test conventions per layer (Phase 4)
- `docs/agents/instructions/backend.instructions.md` — hexagonal layer rules (Phase 2)
- `docs/agents/instructions/frontend.instructions.md` — frontend layer rules (Phase 2)

---

## Phase 1 — Symptom Collection

Before touching any code, collect enough information to reproduce the bug deterministically.

Ask the user for the following if not already provided:

1. **Observed behaviour**: what actually happens (error message, wrong value, unexpected state)?
2. **Expected behaviour**: what should have happened?
3. **Reproduction steps**: minimal sequence of actions or API calls that triggers the bug.
4. **Context**: environment (local / CI), relevant data state, authenticated user role if applicable.
5. **Stacktrace or logs** — paste them in full, do not summarise.

> Do not start investigating until steps 1 and 2 are clear.

---

## Phase 2 — Layer Identification

Locate which architectural layer(s) are involved:

| Symptom | Likely layer |
|---|---|
| Wrong HTTP status code or response body | `application` (controller / DTO mapping) |
| Business rule violated | `domain` (entity, value object, use case) |
| Data persisted incorrectly or not at all | `infrastructure` (adapter, JPA entity mapping) |
| Wrong data returned from DB | `infrastructure` (query, repository) |
| UI renders wrong data or crashes | `client` (component, composable, store) |
| Frontend call fails (4xx / 5xx) | `application` or `domain` — follow the chain |

Cross-layer bugs (symptom in one layer, root cause in another) are common. Always trace the full call chain.

---

## Phase 3 — Root Cause Analysis

### 3.1 Read Before Diagnosing
- Read the relevant source files before forming a hypothesis.
- Never assume the root cause from the symptom alone.

### 3.2 Hypothesis Checklist

**Domain layer**
- [ ] Entity invariant not enforced (missing guard, wrong default)
- [ ] Business rule missing or inverted
- [ ] Value object validation incomplete or incorrect
- [ ] Domain event not raised or raised with wrong data

**Infrastructure layer**
- [ ] Port implementation maps data incorrectly (domain ↔ JPA entity)
- [ ] Query filters or sorts incorrectly
- [ ] Transaction boundary missing or misplaced
- [ ] Secondary adapter calls external service with wrong parameters

**Application layer**
- [ ] Controller maps request DTO to wrong domain input
- [ ] Error swallowed or mapped to wrong HTTP status
- [ ] Response DTO omits a required field

**Frontend (client)**
- [ ] Composable transforms API data incorrectly
- [ ] Component renders from stale or wrong reactive state
- [ ] Store mutation does not reflect the server response
- [ ] API call uses wrong endpoint, method, or payload shape

### 3.3 State the Root Cause Explicitly

Before writing any fix, state:

> "The bug is caused by [X] in [file/class/function], which produces [incorrect behaviour] because [reason]."

---

## Phase 3.4 — Fix Strategy Decision (MANDATORY — do not skip)

**If the fix is trivial** (single-file change, obvious one-liner, no architectural impact):
- Describe the fix in plain language.
- **Ask explicitly**: *"The fix is straightforward — [description]. Do you want me to apply it directly, or would you prefer a full investigation report first?"*
- Do **not** apply the fix until the user says yes.

**If the fix is non-trivial** (multiple files, domain rule change, architecture impact):
- Do **not** ask to fix yet.
- Proceed directly to Phase 6 (Report).
- Only after the user explicitly asks, proceed to Phase 4.

> **CRITICAL**: Never apply a fix without explicit user authorisation.

---

## Phase 4 — Fix (requires explicit user authorisation)

Apply the fix following the TDD cycle from `docs/agents/instructions/development-workflow.md`:

1. **Red** — write a failing test that directly exposes the bug.
2. **Green** — apply the minimal fix to make the test pass.
3. **Refactor** — clean up only if needed; do not change behaviour.

> Do not fix bugs by adjusting tests to match wrong behaviour. The test expresses the correct expectation.

---

## Phase 5 — Verification

1. Run the full test suite for the impacted layer(s). All tests must be green.
2. Check for regressions in adjacent layers.
3. Re-verify the original reproduction steps.
4. If the bug was in production, check for similar patterns elsewhere and flag them (do not auto-fix).

---

## Phase 6 — Investigation Report

Always generate a report at `docs/bugs/{bug-slug}/REPORT.md`:

```markdown
# Bug Report — {title}

**Date**: {YYYY-MM-DD}

## Symptom
{Observed behaviour and reproduction steps.}

## Root Cause
{Plain-language statement — which file/class/function, and why it was wrong.}

## Fix
{What was changed and in which layer. Link to relevant file(s).}

## Non-Regression Test
{Name and location of the test added to cover this bug.}

## Follow-up (optional)
{Any design problem flagged, linked issue or refactoring plan if created.}
```

> The report is mandatory, even for small bugs. A bug fix must always produce at least one new test.
