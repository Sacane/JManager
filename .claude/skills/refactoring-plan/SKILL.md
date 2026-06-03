---
name: refactoring-plan
description: >-
  Analyse code and generate a structured REFACTORING_PLAN.md, then execute step by step with developer
  validation between each step. Use whenever the user mentions a refactoring to perform (hexagonal, CQRS,
  use case split, extract service, clean architecture, package restructuring) and wants a plan before acting.
  Trigger keywords: "prepare the refactoring", "make a plan", "what needs to change", "where do I start",
  "analyse my code before refactoring", "prépare le refactoring", "fais un plan".
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Refactoring Plan

Produce a planning file (`REFACTORING_PLAN.md`) then execute it step by step with developer validation between each step.

**Activate for**: hexagonal architecture extraction, CQRS, use case split, extract service, clean architecture, package restructuring, or any refactoring the user wants to plan before acting. Also trigger on: "prepare the refactoring", "make a plan", "what needs to change", "where do I start", "analyse my code before refactoring".

---

## Phase 1 — Project Analysis

Before producing anything, analyse the provided code. Goal: understand the real state of the project, not apply a pattern mechanically.

Extract:
1. **Current structure**: packages, classes, interfaces that exist.
2. **Detected couplings**: what depends on what (services, repositories, controllers).
3. **Pain points**: overly broad interfaces, God classes, cross-layer dependencies, business logic in the wrong layer.
4. **What already works well** — do not touch what is sound.
5. **Stack and conventions**: Spring annotations, naming style, existing organisation.

If the full project structure is not provided, ask for the missing files before continuing.

---

## Phase 2 — Strategy Selection

Choose the refactoring strategy best suited to the **real state** of the project. Do not impose a dogmatic pattern if the project does not need it.

Consult `references/patterns.md` to identify the relevant pattern(s) and their selection criteria.

Selection criteria:
- Interfaces with too many unrelated methods → Use Case Split
- Domain depends on Spring/JPA → Hexagonal (isolate the domain)
- Reads and writes mixed with complex logic → CQRS / Command-Query Bus
- A class does everything → Extract Service
- Layers are unclear → Package restructuring first, then the rest

Justify the chosen strategy in the plan.

---

## Phase 3 — Planning File Generation

Produce the `REFACTORING_PLAN.md` following the exact structure in `references/plan-template.md`, at:

```
docs/refactoring/{refactoring-name}/REFACTORING_PLAN.md
```

Mandatory rules:
- Each step is **atomic**: a single action, a single file or concept changed.
- Each step has an **explicit validation criterion**: how the developer knows it is done.
- The order respects **dependencies**: interfaces created before implementations, packages restructured before classes are moved.
- **Non-blocking steps** are marked as such (can be done in parallel).
- The plan clearly states **what does not change** to reassure about the scope.

---

## Phase 4 — Guided Execution

Once the plan is validated by the developer, execute it step by step.

**Protocol**:
1. Announce the current step: number, title, objective.
2. Perform the action.
3. Recall the step's validation criterion.
4. **Wait for developer confirmation** before moving to the next step.
5. If the developer requests an adjustment, apply it before continuing.
6. **Immediately** update the step status in `REFACTORING_PLAN.md` once validated (✅ / 🔄 / ⏸️) — **do not batch updates**.
7. Update the **overall status** line at the top of the file (e.g. `Step N / Total completed`).

> **CRITICAL**: The `REFACTORING_PLAN.md` file is the single source of truth for progress. Every time a step transitions, edit the file **in the same response** that marks the transition. Never let the plan fall behind.

Never chain two steps without confirmation. Never modify something outside the plan without flagging it.

---

## Reference Files

- `references/plan-template.md` — Exact template for the `REFACTORING_PLAN.md` to produce. **Read before generating the plan.**
- `references/patterns.md` — Common refactoring patterns (Use Case Split, Hexagonal, Command/Query Bus, Extract Service, Package Restructuring) with selection criteria. **Read during Phase 2.**
