# Client Module — Search field and active filter chips on the booklet page

**Context**
The booklet detail page has no search field, and its tag and sub-tag filters give no global signal
that a filter is active, so a filtered list can be mistaken for an empty booklet. A search input must
be added to the filter bar, together with chips summarising every active filter and a way to clear
them all.

**Acceptance Criteria**
Feature: Searching and clearing filters on a booklet
  In order to find a transaction and understand what is filtered
  As an authenticated user
  I want a search field and a visible summary of the active filters

Scenario: 1. Typing a fragment filters the list
  Given I am on the detail page of a booklet
  When I type a fragment in the search field
  Then the list shows only the transactions matching that fragment

Scenario: 2. The search is debounced
  Given I am on the detail page of a booklet
  When I type several characters quickly in the search field
  Then a single search request is sent once I stop typing

Scenario: 3. Active filters are summarised
  Given a search fragment and a tag filter are applied
  When the filter bar is rendered
  Then a chip is displayed for each active filter

Scenario: 4. Filters can be cleared
  Given several filters are active
  When I activate the clear filters action
  Then every filter is removed and the full list is displayed

Scenario: 5. An empty result is explained
  Given a search fragment matching no transaction
  When the list is rendered
  Then an empty state explains that no transaction matches the filters

**Notes**
- Depends on the application part of UX-22.
- Files: `pages/booklet/[id].vue`, `components/booklet/BookletFilterActionBar.vue`, `composables/useBooklet.ts`.
- Priority P1 - Effort L (full stack) - Part 4 of 4.
