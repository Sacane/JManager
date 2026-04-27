# Template — REFACTORING_PLAN.md

The generated file must follow this structure exactly.

---

```markdown
# REFACTORING_PLAN — [Module / Feature Name]

**Generated on**: YYYY-MM-DD  
**Applied pattern**: [Use Case Split / Hexagonal / CQRS / Extract Service / …]  
**Stack**: [e.g. Kotlin + Spring Boot 3]  
**Overall status**: ⏳ To do — Step 0 / N

---

## Initial Analysis

### What was detected
[Factual description of the analysed code state — 3 to 6 points, no value judgements]

- `BudgetService` contains 11 methods covering distinct responsibilities (create, read, calculate, delete)
- The `application` layer directly imports JPA entities (`@Entity`), coupling the domain to persistence
- Existing tests target `BudgetServiceImpl` as a whole, making fine-grained unit tests difficult
- [...]

### What will NOT be modified
[Explicit negative scope — what stays intact]

- The business logic internal to each operation
- The domain model (`Budget`, `Transaction`, `BudgetId`)
- Output ports (`BudgetRepository`, `NotificationPort`)
- Existing integration tests

### Justification of the chosen pattern
[2 to 4 sentences explaining why THIS pattern for THIS project, not another]

---

## Refactoring Steps

<!--
  Format of each step:
  - Number and clear title
  - Objective: what we want to achieve
  - Actions: ordered list of what the AI will do
  - Validation criterion: how the dev confirms it is done
  - Status: ⏳ To do | 🔄 In progress | ✅ Done | ⏸️ Awaiting dev | ❌ Blocked
-->

---

### Step 1 — [Short and precise title]

**Status**: ⏳ To do  
**Objective**: [What this step concretely achieves]  
**Blocking for**: Steps 2, 3 *(or "No dependency")*

**Actions:**
1. [Atomic action 1]
2. [Atomic action 2]
3. [...]

**Validation criterion:**
> [What the dev must verify to confirm the step succeeded]
> e.g. "The project compiles without error. `BudgetServiceImpl` is no longer referenced directly in any controller."

---

### Step 2 — [Title]

**Status**: ⏳ To do  
**Depends on**: Step 1  
**Objective**: [...]

**Actions:**
1. [...]

**Validation criterion:**
> [...]

---

### Step N — [Title]

**Status**: ⏳ To do  
**Depends on**: Steps X, Y  
**Objective**: [...]

**Actions:**
1. [...]

**Validation criterion:**
> [...]

---

## Recommended Execution Order

```
Step 1 → Step 2 → Step 3
                ↘ Step 4 (parallel with 3)
                          → Step 5
```

*Parallel steps can be done in any order relative to each other.*

---

## Validation Protocol

At each step:
1. The AI announces the step and what it will do
2. The AI produces the modifications
3. The dev verifies according to the step's validation criterion
4. The dev replies **"OK"** (or requests an adjustment)
5. The AI updates the step status and moves to the next one

**Never skip a step without explicit confirmation.**

---

## Identified Risks

| Risk | Probability | Mitigation |
|------|-------------|------------|
| [e.g. Spring injection broken after split] | Medium | Verify `@Autowired` bindings after each step |
| [...] | [...] | [...] |

---

## References
- [Link to pattern doc if relevant]
- [Associated ticket / PR if available]
```

---

## Generation Notes

- The number of steps must be proportional to the size of the refactoring: 3–5 for a small module, 8–15 for a full architecture refactoring
- Each step must be achievable in under 15 minutes by the AI — if longer, split it
- The validation criterion must be **verifiable by the dev without deep expertise**: compilation, passing test, absence of inconsistent import, etc.
- Do not mix "create files" and "delete files" in the same step
- Always start with the least risky steps (creating new files before deleting old ones)
- All plan files must be created at `docs/refactoring/{refactoring-name}/REFACTORING_PLAN.md`
