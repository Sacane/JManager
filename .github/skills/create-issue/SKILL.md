---
name: create-issue
description: Create an issue in the form of a markdown file with title, description, implementation plan, and Gherkin test scenarios from a functional request. Use when needing structured, testable issues.
---

# Instructions
1. Extract context and success criteria from the request, then complete the context with the existing feature description in `FEATURES.md` if relevant.
2. Ask 2-3 questions to clarify the request if necessary
3. Identify impacted modules. If more than one module is impacted, you MUST generate one issue per module. For each module :
    1. Summarize the context specific to the module
    2. Identify specific success criteria for the module
    3. Generate a concise title and structured description.
    4. Produce 1..N Gherkin scenarios covering happy path and edge cases.
    5. Create the issue in the `docs/features/{feature_name}/{module_name}_{issue_title}.md` file using the `templates/issue.md` template.
    6. Validate the issue using `.github/create-issue/validate_issue_format.py`.
4. Be sure to not include implementation if the module is not impacted, and to not include testing scenarios if the module is not impacted. For example, if the request only impacts the domain module, do not include any implementation plan or testing scenario in the application and infrastructure issues.

# Note
- This skill is intended to create manageable issues. Typically, it should not span more than one module.
- If the request is too broad, propose the user to break it down per module

# Examples

Input: "The user wants to export their contacts list to CSV"

Output:
Three files, one per module : one for the domain, one for the application, one for the infrastructure.

file `docs/features/export-contacts/domain_export-contacts-issue.md`
```markdown
# Export Contacts List : Domain Module impact
**Context**
The user wants to export their contacts list to CSV to facilitate sharing and backing up their data.

**Acceptance Criteria**
Feature: Export contacts list
    In order to share or backup contacts
    As a user
    I want to export my contacts to CSV

1. Scenario: Successfully export contacts
    Given an authenticated user with 20 contacts
    When executing a query to fetch contacts
    Then the system retrieves all 20 contacts and generates an export DTO

2. Scenario: No contacts to export
    Given an authenticated user with no contacts
    When executing a query to fetch contacts
    Then the system returns an empty export result
```

file `docs/features/export-contacts/application_export-contacts-issue.md`
```markdown
# Export Contacts List : Application Module impact
**Context**
The user wants to export their contacts list to CSV.
**Acceptance Criteria**
Feature: Export contacts list
1. Scenario: Successfully export contacts
    Given an authenticated user with 20 contacts
    When calling the GET /contacts/export endpoint with a MIME type of text/csv
    Then the application layer processes the request and returns a CSV file with all contacts
2. Scenario: No contacts to export
    Given an authenticated user with no contacts
    When calling the GET /contacts/export endpoint with a MIME type of text/csv
    Then the application layer returns a 204 No Content response
```

file `docs/features/export-contacts/infrastructure_export-contacts-issue.md`
```markdown
# Export Contacts List : Infrastructure Module impact
**Context**
The user wants to export their contacts list to CSV.
**Acceptance Criteria**
Feature: Export contacts list
1. Scenario: Transform export DTO to CSV format
    Given an export DTO containing 20 contacts
    When transforming the DTO to CSV format
    Then a valid CSV file is generated as stream of bytes with all contact details
```
