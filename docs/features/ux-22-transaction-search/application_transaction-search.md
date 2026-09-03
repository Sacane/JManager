# Application Module — Expose the search parameter on the transactions endpoint

**Context**
`GET /api/booklet/{bookletID}/transactions` already accepts `month`, `year`, optional `startDate` and
`endDate`, `page`, `size`, `sortField` and `sortDirection`. It must also accept an optional free-text
search parameter and pass it through to the domain query.

**Acceptance Criteria**
Feature: Search parameter on the transactions endpoint
  In order to search from the client
  As an authenticated user
  I want the transactions endpoint to accept a search fragment

Scenario: 1. The endpoint filters on the search parameter
  Given a booklet holding several transactions
  When the transactions endpoint is called with a search fragment
  Then only the matching transactions are returned

Scenario: 2. Omitting the parameter keeps the current behaviour
  Given a booklet holding several transactions
  When the transactions endpoint is called without a search parameter
  Then the response is identical to the current behaviour

Scenario: 3. An over long search value is rejected
  Given an authenticated user
  When the transactions endpoint is called with a search value longer than 100 characters
  Then the response is 400 Bad Request

Scenario: 4. Another user booklet stays inaccessible
  Given a booklet that belongs to another user
  When the transactions endpoint is called with a search fragment on that booklet
  Then the response is 403 Forbidden

**Notes**
- Apply `@Size(max = 100)` on the search parameter, consistent with the label constraint.
- Priority P1 - Effort L (full stack) - Part 3 of 4.
