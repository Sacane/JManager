---
name: bug-investigation
description: >
  Structured protocol to investigate, reproduce, locate, and fix a bug in the JManager project.
  Activate when the user reports unexpected behaviour, a crash, an incorrect HTTP response, a wrong
  database state, or a failing test they did not write. Trigger keywords: "bug", "ça plante",
  "comportement inattendu", "erreur", "ça marche pas", "je ne comprends pas pourquoi", "stacktrace",
  "exception", "regression", "test qui échoue".
---

# Bug Investigation Skill

This skill guides a structured investigation from symptom to verified fix.
It does **not** redefine coding conventions, TDD cycle, or layer responsibilities — those are covered by:
- `docs/agents/instructions/development-workflow.md` — TDD Red/Green/Refactor (referenced in Phase 4)
- `docs/agents/instructions/testing-guidelines.md` — test conventions per layer (referenced in Phase 4)
- `docs/agents/instructions/backend.instructions.md` — hexagonal layer rules (referenced in Phase 2)
- `docs/agents/instructions/frontend.instructions.md` — frontend layer rules (referenced in Phase 2)

---

## Phase 1 — Symptom Collection

Before touching any code, collect enough information to reproduce the bug deterministically.

Ask the user for the following if not already provided:

1. **Observed behaviour**: what actually happens (error message, wrong value, unexpected state)?
2. **Expected behaviour**: what should have happened?
3. **Reproduction steps**: minimal sequence of actions or API calls that triggers the bug.
4. **Context**: environment (local / CI), relevant data state, authenticated user role if applicable.
5. **Stacktrace or logs** if available — paste them in full, do not summarise.

> Do not start investigating until steps 1 and 2 are clear.  
> If reproduction steps are missing, ask the user to provide them before continuing.

---

## Phase 2 — Layer Identification

Locate which architectural layer(s) are involved using the hexagonal model:

| Symptom | Likely layer |
|---|---|
| Wrong HTTP status code or response body | `application` (controller / DTO mapping) |
| Business rule violated | `domain` (entity, value object, use case) |
| Data persisted incorrectly or not at all | `infrastructure` (adapter, JPA entity mapping) |
| Wrong data returned from the database | `infrastructure` (query, repository) |
| UI renders wrong data or crashes | `client` (component, composable, store) |
| Frontend call fails (4xx / 5xx) | `application` or `domain` — follow the chain |

**Cross-layer bugs** (symptom in one layer, root cause in another) are common. Always trace the call chain
from entry point to persistence before concluding the root cause.

Refer to `docs/agents/instructions/backend.instructions.md` for layer boundary rules.  
Refer to `docs/agents/instructions/frontend.instructions.md` for frontend-specific concerns.

---

## Phase 3 — Root Cause Analysis

Once the layer is identified, locate the exact defect.

### 3.1 Read Before Diagnosing

- Read the relevant source files before forming a hypothesis.
- Never assume the root cause from the symptom alone — verify it in the code.

### 3.2 Hypothesis Checklist (pick the most likely first)

**Domain layer**
- [ ] An entity invariant is not enforced (missing guard, wrong default value)
- [ ] A business rule is missing or inverted
- [ ] A value object validation is incomplete or incorrect
- [ ] A domain event is not raised or is raised with wrong data

**Infrastructure layer**
- [ ] A port implementation maps data incorrectly (domain object ↔ JPA entity)
- [ ] A query filters or sorts incorrectly
- [ ] A transaction boundary is missing or misplaced
- [ ] A secondary adapter calls an external service with wrong parameters

**Application layer**
- [ ] A controller maps the request DTO to the wrong domain input
- [ ] A use case is called with incorrect parameters
- [ ] An error is swallowed or mapped to the wrong HTTP status
- [ ] A response DTO omits a required field

**Frontend (client)**
- [ ] A composable transforms API data incorrectly
- [ ] A component renders from stale or wrong reactive state
- [ ] A store mutation does not reflect the server response
- [ ] An API call uses the wrong endpoint, method, or payload shape

### 3.3 State the Root Cause Explicitly

Before writing any fix, state the root cause in plain language:

> "The bug is caused by [X] in [file/class/function], which produces [incorrect behaviour] because [reason]."

---

### 3.4 — Fix Strategy Decision (MANDATORY — do not skip)

After the root cause is identified, **always** stop and assess the complexity of the fix:

**If the fix is trivial** (single-file change, obvious one-liner, no architectural impact):
- Describe the fix to the user in plain language.
- **Ask explicitly**: *"The fix is straightforward — [description of the change]. Do you want me to apply it directly, or would you prefer a full investigation report first?"*
- Do **not** apply the fix until the user explicitly says yes.

**If the fix is non-trivial** (multiple files, domain rule change, architecture impact, uncertain scope):
- Do **not** ask to fix yet.
- Proceed directly to Phase 6 (Report) to document the full analysis.
- Present the report to the user.
- **Only after the user explicitly asks** for the fix to be applied, proceed to Phase 4.

> **CRITICAL**: Never apply a fix without explicit user authorization, regardless of how obvious the fix seems.

---

## Phase 4 — Fix (requires explicit user authorization)

> **This phase must only be entered after the user has explicitly authorized the fix.**  
> If authorization was not given, go to Phase 6 (Report) first.

Apply the fix following the mandatory TDD cycle defined in `docs/agents/instructions/development-workflow.md`:

1. **Red** — write a failing test that directly exposes the bug.
   - The test must fail *because* of the bug, not because of a missing dependency.
   - Place the test in the layer that owns the broken behaviour (see Phase 2).
2. **Green** — apply the minimal fix to make the test pass.
   - Do not fix unrelated issues at this stage.
3. **Refactor** — clean up only if needed; do not change behaviour.

Follow test conventions from `docs/agents/instructions/testing-guidelines.md` for the impacted layer.

> **Do not fix bugs by adjusting tests to match wrong behaviour.**  
> The test expresses the *correct* expectation; the production code must be fixed to meet it.

---

## Phase 5 — Verification

Once the fix is applied:

1. **Run the full test suite** for the impacted layer(s). All tests must be green.
2. **Check for regressions** in adjacent layers: if the fix touches a port contract, verify
   that the application layer and the infrastructure adapter still compile and their tests pass.
3. **Re-verify the original reproduction steps** manually or via test to confirm the fix resolves
   the exact symptom reported in Phase 1.
4. If the bug was found in production or a shared environment, check whether similar patterns
   exist elsewhere in the codebase and flag them to the user without automatically fixing them.

---

## Phase 6 — Investigation Report

Generate the report **before Phase 4** for non-trivial fixes, or **after Phase 4** when the user authorized a direct trivial fix.

Always generate a report file at:

```
docs/bugs/{bug-slug}/REPORT.md
```

Where `{bug-slug}` is a short kebab-case identifier derived from the bug description (e.g., `transaction-amount-negative`, `auth-token-not-refreshed`).

The report must contain the following sections:

```markdown
# Bug Report — {title}

**Date**: {YYYY-MM-DD}

## Symptom
{What was observed. Reproduction steps.}

## Root Cause
{Plain-language statement from Phase 3.3 — which file/class/function, and why it was wrong.}

## Fix
{What was changed and in which layer. Link to the relevant file(s).}

## Non-Regression Test
{Name and location of the test added to cover this bug.}

## Follow-up (optional)
{Any design problem flagged to the user, linked issue or refactoring plan if created.}
```

> The report is mandatory, even for small bugs. It serves as a trace for future regressions
> and documents decisions made during the investigation.

---

## Notes

- If the investigation reveals a design problem rather than a simple defect (e.g., a port contract
  that is fundamentally wrong, a domain rule that lives in the wrong layer), flag it to the user.
  The fix for the immediate bug should remain minimal; a design fix belongs to a separate issue
  created with the `create-issue` skill or a refactoring planned with the `refactoring-plan` skill.
- A bug fix must always produce at least one new test. A fix with no test is incomplete.
