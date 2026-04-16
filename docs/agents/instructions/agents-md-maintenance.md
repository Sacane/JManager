# AGENTS.md Maintenance Strategy

Maintenance strategy for the agent guidelines system to keep instructions clear, consistent, and actionable over time.

## 1. Scope

This document covers the maintenance of the following artefacts:
- `AGENTS.md`
- `docs/agents/instructions/*.md`
- any agent workflow document tied to the repository

This document does not cover:
- product business decisions (in `FEATURES.md`)
- implementation details of a specific feature

## 2. Goals

- Prevent drift between instructions and the actual state of the codebase.
- Ensure rules are actionable, testable, and non-contradictory.
- Reduce ambiguity for both humans and agents.
- Keep the guidelines system simple to maintain.

## 3. Roles and Responsibilities

- Guidelines owner:
  - validates overall document consistency
  - arbitrates conflicts between instructions
  - schedules periodic reviews
- Contributor:
  - proposes targeted updates
  - justifies every change with a concrete problem
  - applies the quality checklist before raising a PR
- Reviewer:
  - verifies the absence of contradictions
  - checks clarity, precision, and actionability
  - requests examples when a rule is ambiguous

## 4. Update Triggers

An update is mandatory when at least one of the following occurs:
- architectural change (module, boundaries, conventions)
- addition or removal of a tool or technical constraint
- evolution of the development workflow (tests, review, release)
- recurring bug linked to a missing or misleading instruction
- conflict detected between two guidelines documents

## 5. Maintenance Cadence

- Continuous maintenance: every PR that modifies a workflow must update the affected guidelines.
- Periodic review: 1 light monthly review of the full system.
- Major review: 1 quarterly review to simplify, deprecate, or merge rules.

## 6. Change Workflow

1. Identify the problem
- describe the observed symptom
- link the symptom to a missing, outdated, or contradictory rule

2. Define the proposal
- specify the target document
- write the rule in actionable terms (verb + condition + expected result)
- limit the scope to the actual need

3. Implement the update
- modify the minimum number of files necessary
- preserve the existing editorial style
- add a concrete example if the rule is open to interpretation

4. Verify quality
- run through the checklist in section 7
- have at least one reviewer read the changes

5. Trace the change
- add a short entry in the maintenance history (section 8)
- reference the PR in the commit message or PR description

## 7. Quality Checklist

Before merging, verify:
- [ ] the rule is clear and testable
- [ ] the rule does not conflict with another document
- [ ] the scope is explicit (where, when, for whom)
- [ ] vague formulations have been eliminated
- [ ] internal links and file paths are valid
- [ ] the example (if present) is consistent with the current codebase

## 8. Maintenance History

Recommended format:

```text
YYYY-MM-DD | type(change|cleanup|clarification|deprecation) | file(s) | short summary
```

## 9. Writing Rules

- Prefer short, imperative sentences.
- One rule = one intention.
- Avoid unjustified absolute formulations.
- Avoid duplication across documents; link to the source of truth.
- Quand une regle devient obsolete, la supprimer au lieu de la laisser inactive.

## 10. Definition of Done (maintenance)

Une mise a jour de guidelines est complete si:
- le probleme initial est explicite et resolu
- les conflits potentiels ont ete traites
- la checklist qualite est complete
- la trace de changement est ajoutee
- la nouvelle version est comprensible sans contexte oral supplementaire
