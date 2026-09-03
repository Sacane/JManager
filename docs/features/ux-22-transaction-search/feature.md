# UX-22 — Finding a past transaction

**Context**
Functional acceptance for UX-22, shared by the domain, infrastructure, application and client issues. Retrieving a past entry currently means browsing month by month, page by page.

**Acceptance Criteria**
Feature: Finding a past transaction
  In order to retrieve an entry without browsing month by month
  As an authenticated user
  I want to search my transactions by their label

Scenario: I find a transaction by its label
  Given I am looking at a booklet
  When I type part of a transaction label
  Then only the transactions matching it are listed

Scenario: The search is forgiving
  Given a transaction whose label carries accents and capitals
  When I search for it without them
  Then it is still found

Scenario: I always know what is filtered
  Given a search and other filters are applied
  When I look at the list
  Then the active filters are visible and I can clear them in one action

Scenario: An empty result is explained
  Given a search matching nothing
  When the list is displayed
  Then it explains that no transaction matches, rather than looking empty

**Notes**
- Layer-agnostic functional acceptance for UX-22. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
