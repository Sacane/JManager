# Domain Module — Search transactions by label

**Context**
Finding a past transaction is impossible today: the booklet detail page offers tag and sub-tag
filters only, so retrieving an entry means browsing month by month, page by page. The domain must
support filtering the transactions of a booklet by a free-text fragment of their label, combined with
the existing period, pagination and sorting criteria.

**Acceptance Criteria**
Feature: Free text search over transactions
  In order to find a past entry quickly
  As an authenticated user
  I want to filter my transactions by a fragment of their label

Scenario: 1. A matching fragment returns the transactions
  Given a booklet holding transactions labelled Courses Carrefour and Essence
  When the transactions are searched with the fragment carrefour
  Then only the Courses Carrefour transaction is returned

Scenario: 2. The search ignores case and accents
  Given a booklet holding a transaction labelled Peage autoroute
  When the transactions are searched with the fragment PEAGE
  Then the transaction is returned

Scenario: 3. The search combines with the existing filters
  Given a booklet holding transactions across several tags
  When the transactions are searched with a fragment and a tag filter
  Then only the transactions matching both criteria are returned

Scenario: 4. An empty fragment does not filter
  Given a booklet holding transactions
  When the transactions are searched with an empty fragment
  Then the result is the same as without any search criterion

Scenario: 5. No match returns an empty page
  Given a booklet holding transactions
  When the transactions are searched with a fragment matching nothing
  Then an empty page is returned with a total of zero

**Notes**
- Extend the existing transaction query criteria rather than adding a parallel use case.
- Keep pagination and sorting semantics unchanged.
- Priority P1 - Effort L (full stack) - Part 1 of 4.
