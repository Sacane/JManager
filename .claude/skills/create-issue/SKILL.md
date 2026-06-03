---
name: create-issue
description: >-
  Create a structured feature issue — title, description, Gherkin scenarios, per module — as a
  markdown file from a functional request. Use when needing structured, testable issues.
  Trigger keywords: "crée une issue", "create an issue", "feature issue", "Gherkin", "acceptance criteria".
allowed-tools: Read, Edit, Write, Glob, Grep, Bash
---

# Create Issue

Create a well-structured feature issue as a markdown file with title, description, implementation plan, and Gherkin test scenarios.

---

## Instructions

1. Extract context and success criteria from the request. Complete the context with the existing feature description in `FEATURES.md` if relevant.
2. Ask 2–3 questions to clarify the request if necessary.
3. Identify impacted modules. If more than one module is impacted, **generate one issue per module**. For each module:
   1. Summarise the context specific to that module.
   2. Identify specific success criteria for the module.
   3. Generate a concise title and structured description.
   4. Produce 1–N Gherkin scenarios covering happy path and edge cases.
   5. Create the issue at `docs/features/{feature_name}/{module_name}_{issue_title}.md`, following the `template.md` structure.
   6. **Validate** the generated file with `python validate_issue_format.py <issue_file>` (script in this skill folder). Fix any reported error before moving on.

4. **Do not** include implementation details for a module that is not impacted.
5. **Do not** include testing scenarios for a module that is not impacted. For example, if the request only impacts the domain module, the application and infrastructure issues should contain no implementation plan or testing scenarios.
6. `client` (frontend) is also a module. If the request impacts the UI, create a client issue with UI/UX acceptance criteria and testing scenarios.
7. Write the issue in **English**, even if the request is in another language.
8. If the request changes behaviour already described in `FEATURES.md`, **update `FEATURES.md`** to keep it aligned with the new behaviour.

---

## Notes

- This skill is for manageable, focused issues. It should not span more than one module per file.
- If the request is too broad, propose breaking it down per module before generating.

---

## Issue format

See `template.md` for the exact skeleton and `reference.md` for the full section-by-section specification and naming convention. Summary:

```markdown
# {Module} Module — {Feature Title}

**Context**
{Description of what the user wants to achieve and why, scoped to this module.}

**Acceptance Criteria**
Feature: {Feature name}
  In order to {goal}
  As a {role}
  I want to {action}

Scenario: {Happy path title}
  Given {initial state}
  When {action}
  Then {expected outcome}

Scenario: {Edge case title}
  Given {initial state}
  When {action}
  Then {expected outcome}
```

---

## Example

**Request**: "The user wants to export their contacts list to CSV"

**Output**: Three files, one per module.

`docs/features/export-contacts/domain_export-contacts-issue.md`
```markdown
# Export Contacts List — Domain Module

**Context**
The user wants to export their contacts list to CSV to facilitate sharing and backing up their data.

**Acceptance Criteria**
Feature: Export contacts list
  In order to share or backup contacts
  As a user
  I want to export my contacts to CSV

Scenario: Successfully export contacts
  Given an authenticated user with 20 contacts
  When executing a query to fetch contacts
  Then the system retrieves all 20 contacts and generates an export DTO

Scenario: No contacts to export
  Given an authenticated user with no contacts
  When executing a query to fetch contacts
  Then the system returns an empty export result
```

---

## Reference Files

- `template.md` — Exact skeleton for the generated issue file. **Use when creating the file.**
- `reference.md` — Output format specification (required sections) and file naming convention.
- `validate_issue_format.py` — Validates issue structure and Gherkin syntax. **Run on every generated file.**
