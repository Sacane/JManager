# Infrastructure Module — Persistence support for the label search

**Context**
The persistence adapter must support the label search criterion added by the domain, without
degrading the performance of the existing paginated queries. The project recently fixed a JPA
cartesian fetch issue on this area, so the new predicate must not reintroduce a join explosion.

**Acceptance Criteria**
Feature: Indexed label search
  In order to keep the transaction list fast
  As the system
  I want the label search executed in the database with an index

Scenario: 1. The adapter filters on the label
  Given transactions stored for a booklet
  When the adapter queries with a label fragment
  Then only the matching rows are returned

Scenario: 2. Search combines with pagination and sorting
  Given more transactions match than one page can hold
  When the adapter queries with a label fragment, a page and a sort
  Then the returned page respects the requested page, size and order

Scenario: 3. The query stays free of cartesian fetch
  Given a booklet whose transactions carry tags
  When the adapter queries with a label fragment
  Then the number of returned rows equals the number of matching transactions

Scenario: 4. The search is case and accent insensitive in the database
  Given a transaction labelled Peage autoroute
  When the adapter queries with the fragment PEAGE
  Then the transaction is returned

**Notes**
- Add a Flyway migration for the supporting index.
- Integration tests against a real database, per the project testing strategy.
- Priority P1 - Effort L (full stack) - Part 2 of 4.
