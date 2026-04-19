# Fix: Doughnut Tag Chart Truncated to Top 5/6 Tags on Dashboard — Client Module

**Contexte**
On the dashboard page, the "Dépenses par catégorie" doughnut chart and the "Top tags de la période" list below it are artificially limited: the chart renders at most 6 slices (`.slice(0, 6)`) and the insight list shows at most 5 entries (`.slice(0, 5)`). When a user has more than 6 tags with expenses in the selected period, the remaining tags are silently omitted — their amounts are not represented in the chart and the total displayed does not add up. The fix must remove these hard limits so every tag with a non-zero expense for the period is displayed.

**Critères d'acceptation**

Feature: Complete tag distribution on the dashboard doughnut chart

    Scenario: 1 — Doughnut chart shows all tags when there are 6 or fewer
        Given a user is on the dashboard page
        And the selected period has 4 expense tags
        When the category distribution data is loaded
        Then the doughnut chart renders exactly 4 slices
        And the "Top tags de la période" list shows all 4 tags

    Scenario: 2 — Doughnut chart shows all tags when there are more than 6
        Given a user is on the dashboard page
        And the selected period has 9 expense tags
        When the category distribution data is loaded
        Then the doughnut chart renders exactly 9 slices, one per tag
        And the "Top tags de la période" list shows all 9 tags
        And the percentages visible in the list sum to 100 %

    Scenario: 3 — Tags are still ordered by descending expense amount
        Given a user is on the dashboard page
        And the selected period has 8 expense tags with varying amounts
        When the category distribution data is loaded
        Then the doughnut chart slices and the tag list are both ordered from the highest to the lowest expense amount

    Scenario: 4 — No regression when there are no tags
        Given a user is on the dashboard page
        And the selected period has no expense tags
        When the category distribution data is loaded
        Then the doughnut chart is empty
        And the message "Aucun tag de dépense sur cette période" is displayed

**Notes**
- Impacted computed properties in `client/pages/dashboard/index.vue`: `categoryExpensesData` (`.slice(0, 6)`) and `topTagsInsights` (`.slice(0, 5)`).
- Both slices must be removed; sorting by descending amount must be preserved.
- Existing unit/component tests in `client/tests/pages/dashboard-index.spec.ts` must be updated to add scenarios with more than 6 tags.
- No backend change is required — the API already returns all categories.
