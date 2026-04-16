# Reference — create-issue

## Output format

Each issue is a standalone Markdown file with the following sections:
0
| Section | Required | Description |
|---|---|---|
| `# Title` | ✅ | Concise title of the issue. Include the impacted module name when the feature spans multiple modules. |
| `**Contexte**` | ✅ | Functional context explaining *why* the issue exists and *who* it concerns. |
| `**Critères d'acceptation**` | ✅ | A Gherkin `Feature:` block with 1–N `Scenario:` entries covering the happy path and edge cases. |
| `**Notes**` | ❌ | Optional. Any additional references, constraints, or open questions. |

## File naming convention

`docs/features/{feature-name}/{module}_{issue-title}.md`
