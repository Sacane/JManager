# Booklet Transactions Table: Add Sub-Tag Column — Client Module

**Context**
In the booklet detail page (`booklet/{id}`), the transactions table has a single "Tag" column that always displays
`tagDTO.label` directly — whether the tag is a parent tag or a sub-tag. When a transaction is tagged with a sub-tag
(a personal tag with a non-null `parentId`), there is no visual distinction between the parent and the sub-tag in
the table: the parent context is lost.

A dedicated "Sous tag" column must be added so that:
- The "Tag" column shows the **parent tag** (resolved from `parentId`) when the transaction uses a sub-tag, or the
  tag itself when there is no parent.
- The new "Sous tag" column shows the **sub-tag label** when applicable, and is empty otherwise.

The two-column tag filter panel already exists in the sidebar (parent tag selector + sub-tag selector). The filtering
rules must be verified and enforced in `filteredTransactions`:
- Filtering by a **parent tag** → include transactions tagged with that parent tag **or any of its sub-tags**.
- Filtering by a **sub-tag** → include only transactions tagged with that specific sub-tag.

**Acceptance Criteria**

Feature: Sub-tag column in booklet transactions table

```gherkin
Scenario: Tag column shows parent tag and sub-tag column shows sub-tag for sub-tagged transactions
  Given a transaction tagged with sub-tag "Restaurants" whose parent tag is "Food"
  When the user views the booklet transactions table
  Then the "Tag" column for that row shows "Food"
  And the "Sous tag" column shows "Restaurants"

Scenario: Sub-tag column is empty for transactions with a parent (top-level) tag
  Given a transaction tagged with tag "Transport" which has no parent
  When the user views the booklet transactions table
  Then the "Tag" column shows "Transport"
  And the "Sous tag" column is empty

Scenario: Filtering by a parent tag includes transactions of that tag and all its sub-tags
  Given a booklet with 3 transactions tagged "Food" and 2 transactions tagged "Restaurants" (sub-tag of "Food")
  When the user selects "Food" in the parent tag filter
  Then the table displays all 5 transactions

Scenario: Filtering by a sub-tag includes only transactions of that sub-tag
  Given a booklet with 3 transactions tagged "Food" and 2 transactions tagged "Restaurants" (sub-tag of "Food")
  When the user selects sub-tag "Restaurants" in the sub-tag filter
  Then the table displays only the 2 "Restaurants" transactions
  And the 3 "Food" transactions are not shown

Scenario: When no tag filter is active, all transactions are displayed regardless of tag hierarchy
  Given a booklet with transactions tagged with parent tags and sub-tags
  When no tag filter is selected
  Then all transactions are displayed
```

**Notes**
- The `tagDTO` already carries `parentId`. Use the already-loaded `tags` ref to resolve the parent tag object for
  display (e.g. `tags.value.find(t => t.tagId === row.tagDTO.parentId)`).
- The `bookletTransactionColumns` array must be extended with a new `subTag` column entry.
- The `body-tag` slot must be updated to render the parent tag label/color (or the tag itself when no parent exists).
- A new `body-subTag` slot must be added to render the sub-tag chip when `tagDTO.parentId` is set.
- The filtering logic in `filteredTransactions` (parent tag + sub-tag branches) appears already correct; verify with
  component tests.
