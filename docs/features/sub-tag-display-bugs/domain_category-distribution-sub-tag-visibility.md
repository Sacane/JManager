# Category Distribution: Show Sub-Tags as Top-Level Entries When Parent Has No Direct Transactions — Domain Module

**Context**
The `CategoryDistributionCalculatorImpl` always groups sub-tag transactions under their parent tag when building the
distribution. If a parent tag (e.g. "Food") has no transactions tagged directly with it — only its sub-tags
(e.g. "Restaurants", "Groceries") are used — "Food" still appears as a top-level entry in the distribution, with
the sub-tags hidden in its `subCategories` list.

In the dashboard pie chart, a slice for "Food" appears, but the user has no way to interact with the individual
sub-tag data (e.g. navigate or drill down to "Restaurants" transactions). Only clicking the "Food" slice opens a
secondary chart that reveals the sub-tags — but clicking the secondary chart has no further effect.

The fix: when a parent tag has **no direct transactions** in the current result set (i.e. every transaction that
would be rolled up under it actually belongs to a sub-tag), each sub-tag must appear as its **own top-level entry**
in the distribution, not grouped under the unused parent. The parent tag must not appear in the result at all in
this case. This makes every sub-tag entry a first-class, directly interactive segment in the pie chart.

When the parent tag **does** have at least one direct transaction, the current grouping behaviour (sub-tags rolled
up under the parent with `subCategories`) must be preserved.

**Acceptance Criteria**

Feature: Category distribution sub-tag visibility

```gherkin
Scenario: Sub-tags appear as top-level entries when parent tag has no direct transactions
  Given parent tag "Food" with no transactions tagged directly with "Food"
  And sub-tag "Restaurants" (parent: "Food") with 50 € of expense transactions
  When the category distribution is calculated
  Then the distribution contains a top-level entry for "Restaurants" with totalAmount 50 €
  And the distribution does NOT contain a top-level entry for "Food"
  And the "Restaurants" entry has an empty subCategories list

Scenario: Multiple sub-tags each appear at top level when their shared parent has no direct transactions
  Given parent tag "Food" with no transactions tagged directly with "Food"
  And sub-tag "Restaurants" (parent: "Food") with 30 € of expense transactions
  And sub-tag "Groceries" (parent: "Food") with 20 € of expense transactions
  When the category distribution is calculated
  Then the distribution contains a top-level entry for "Restaurants" with totalAmount 30 €
  And the distribution contains a top-level entry for "Groceries" with totalAmount 20 €
  And the distribution does NOT contain a top-level entry for "Food"

Scenario: Sub-tags are grouped under parent when parent has at least one direct transaction
  Given parent tag "Food" with 10 € of transactions tagged directly with "Food"
  And sub-tag "Restaurants" (parent: "Food") with 50 € of expense transactions
  When the category distribution is calculated
  Then the distribution contains a top-level entry for "Food" with totalAmount 60 €
  And the "Restaurants" entry appears in the subCategories of "Food"
  And "Restaurants" does NOT appear as a top-level entry

Scenario: Tags with no sub-tags continue to appear as top-level entries unchanged
  Given tag "Transport" with no sub-tags and 15 € of expense transactions
  When the category distribution is calculated
  Then "Transport" appears as a top-level entry with totalAmount 15 €
  And its subCategories list is empty

Scenario: Parent tag with only sub-tags is excluded and percentages are computed correctly
  Given parent tag "Food" with no direct transactions
  And sub-tag "Restaurants" (parent: "Food") with 40 € of expenses
  And top-level tag "Transport" with 60 € of expenses
  When the category distribution is calculated
  Then total expenses equal 100 €
  And "Restaurants" has percentage ≈ 40 %
  And "Transport" has percentage ≈ 60 %
  And "Food" is absent from the distribution
```

**Notes**
- The determination of "no direct transactions" must be based solely on the transactions passed to
  `calculateDistribution` (filtered by date range if provided), not on global tag usage.
- The algorithm change is internal to `CategoryDistributionCalculatorImpl`; the `CategoryDistributionCalculator`
  interface and the `CategoryData` / `CategoryDistributionOutput` model do not need to change.
- Domain tests in `CategoryDistributionCalculatorTest` must be added/updated for all new scenarios.
- The dashboard frontend does not require a dedicated change: sub-tag entries appearing as top-level
  `CategoryDataDTO` objects with empty `subCategories` are handled by the existing pie chart logic.
