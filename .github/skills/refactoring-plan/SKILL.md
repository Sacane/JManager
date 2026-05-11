---
name: refactoring-plan
description: Analyse the project's source code and generate a structured refactoring planning file, which the AI executes step by step with developer validation between each step. Use this skill whenever the user mentions a refactoring to perform (hexagonal, CQRS, use case split, extract service, clean architecture, package restructuring…) and wants a plan before acting. Also trigger on: "prepare the refactoring", "make a plan", "what needs to change", "where do I start", "analyse my code before refactoring".
---

# Refactoring Plan Skill

This skill produces a planning file (`REFACTORING_PLAN.md`) that the AI and the developer follow together. The AI first analyses the project, selects the best strategy, then walks through the steps one by one — the developer validates before each transition.

---

## Phase 1 — Project Analysis

Before producing anything, analyse the provided code. The goal is to understand the real state of the project, not to apply a pattern mechanically.

**What to extract:**

1. **Current structure**: which packages, classes, and interfaces exist
2. **Detected couplings**: what depends on what (services, repositories, controllers)
3. **Pain points**: overly broad interfaces, God classes, cross-layer dependencies, business logic in the wrong layer
4. **What already works well**: do not touch what is sound
5. **Stack and conventions**: Spring annotations present, naming style, existing organisation

Read the provided files. If the full project structure is not provided, ask for the missing files before continuing.

---

## Phase 2 — Strategy Selection

Based on the analysis, choose the refactoring strategy best suited to the **real state** of the project. Do not impose a dogmatic pattern if the project does not need it.

Consult `references/patterns.md` to identify the relevant pattern(s).

**Selection criteria:**
- Interfaces with too many unrelated methods → Use Case Split
- Domain depends on Spring/JPA → Hexagonal (isolate the domain)
- Reads and writes mixed with complex logic → CQRS
- A class does everything → Extract Service
- Layers are unclear → Package restructuring first, then the rest

The chosen strategy must be justified in the plan.

---

## Phase 3 — Planning File Generation

Produce the `REFACTORING_PLAN.md` file following the template in `references/plan-template.md`.

**Mandatory rules for the plan:**

- Each step is **atomic**: a single action, a single file or a single concept changed
- Each step has an **explicit validation criterion**: how the developer knows it is done
- The order respects **dependencies**: interfaces are created before implementations, packages are restructured before classes are moved
- **Non-blocking steps** are marked as such (can be done in parallel)
- The plan clearly states **what does not change** to reassure about the scope
- The plan file must be created at `docs/refactoring/{refactoring-name}/REFACTORING_PLAN.md`

---

## Phase 4 — Guided Execution

Once the plan is validated by the developer, execute it step by step.

**Execution protocol:**

1. Announce the current step: number, title, objective
2. Perform the action (generate code, propose the modification)
3. Recall the step's validation criterion
4. **Wait for the developer's confirmation** before moving to the next step
5. If the developer requests an adjustment, apply it before continuing
6. **Immediately** update the step status in `REFACTORING_PLAN.md` once validated (✅ / 🔄 / ⏸️) — **do not batch updates**
7. Also update the **overall status** line at the top of the file (e.g. `Step N / Total completed`)

**CRITICAL — Real-time plan updates:**
> The `REFACTORING_PLAN.md` file is the single source of truth for progress. Every time a step transitions to a new state — started, validated, blocked — the file **must be edited immediately**, in the same response that marks the transition. Never let the plan fall behind the actual execution state.

Never chain two steps without confirmation. Never modify something outside the plan without flagging it.

---

## Reference Files

- `references/plan-template.md` — Exact template for the `REFACTORING_PLAN.md` file to produce. **Read before generating the plan.**
- `references/patterns.md` — Descriptions of common patterns and selection criteria. **Read during Phase 2.**
