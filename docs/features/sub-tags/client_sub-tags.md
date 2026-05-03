# Sub-Tags — Client Module

**Context**
The backend now supports sub-tags: a personal tag can have children (sub-tags).
The frontend must reflect this hierarchy across three areas:

1. **Tag management page** (`client/pages/tag/index.vue`): the "New tag" dialog must offer a
   "Sub-tag" option that reveals a parent-tag dropdown and creates the sub-tag via
   `POST /tag/sub-tag`.

2. **Booklet main page** (`client/pages/booklet/[id].vue`): the tag-filter area must display
   a two-column layout — column A for parent tags, column B for the sub-tags of the currently
   selected parent. Clicking a parent filters all transactions belonging to that parent and all
   its sub-tags; clicking a sub-tag filters only its own transactions.

3. **Dashboard page** (`client/pages/dashboard/index.vue`): the "Dépenses par catégorie"
   doughnut chart must aggregate each parent tag's amount (including all its sub-tags). When the
   user clicks on a parent-tag slice, a secondary pie chart appears next to the primary one
   showing each sub-tag's share. Tags with no sub-tags do not trigger the secondary chart but
   show a visual indicator.

**Acceptance Criteria**

Feature: Sub-tag creation in the tag management page

    Scenario: 1 — User selects "Sub-tag" option when creating a new tag
        Given the user is on the tag management page
        When the user clicks "Nouveau tag"
        Then the creation dialog displays a toggle to switch between "Tag" and "Sous-tag" modes

    Scenario: 2 — Sub-tag mode shows a parent-tag selector
        Given the creation dialog is open in "Sous-tag" mode
        Then a dropdown listing all existing personal tags is visible
        And the user must select one parent before the "Create" button is enabled

    Scenario: 3 — Submitting the sub-tag form calls POST /tag/sub-tag
        Given the user has entered a label, picked a colour, and selected a parent tag
        When the user clicks "Créer le tag"
        Then the composable calls POST /tag/sub-tag with { tagLabel, colorDTO, parentId }
        And on success the new sub-tag appears in the tag list below its parent

    Scenario: 4 — Sub-tags are displayed indented under their parent in the tag list
        Given a parent tag "Food" and its sub-tags "Restaurants" and "Groceries"
        When the tag list is rendered
        Then "Restaurants" and "Groceries" are displayed visually nested under "Food"

Feature: Two-column tag filter in the booklet main page

    Scenario: 5 — Column A shows top-level (parent) tags, column B shows sub-tags of the selected parent
        Given the booklet main page is open
        And there are parent tags and sub-tags
        When the tag filter panel is rendered
        Then column A lists all parent tags (and standalone tags)
        And column B is initially empty

    Scenario: 6 — Clicking a parent tag in column A populates column B with its sub-tags
        Given a parent tag "Food" with sub-tags "Restaurants" and "Groceries"
        When the user clicks "Food" in column A
        Then column B shows "Restaurants" and "Groceries"

    Scenario: 7 — Clicking a parent tag filters transactions including those of all sub-tags
        Given parent tag "Food" and sub-tags "Restaurants" and "Groceries"
        And transactions tagged "Food" (direct), "Restaurants", and "Groceries"
        When the user clicks "Food" in column A
        Then all three transactions are shown in the transaction list

    Scenario: 8 — Clicking a sub-tag in column B filters only its own transactions
        Given transactions tagged "Restaurants" and "Groceries"
        When the user clicks "Restaurants" in column B
        Then only the "Restaurants" transaction is shown

    Scenario: 9 — Tags with no sub-tags remain selectable in column A without activating column B
        Given a standalone tag "Transport" with no sub-tags
        When the user clicks "Transport" in column A
        Then the transaction list filters to "Transport" transactions only
        And column B remains empty

Feature: Hierarchical doughnut chart on the dashboard

    Scenario: 10 — The primary doughnut chart shows one slice per parent tag (or standalone tag)
        Given parent tag "Food" with sub-tags, and standalone tag "Transport"
        When the dashboard loads the category distribution
        Then the primary chart has one slice for "Food" (aggregate of all its sub-tags + direct)
        And one slice for "Transport"
        And no separate slices for sub-tags

    Scenario: 11 — Clicking a parent-tag slice with sub-tags opens a secondary chart
        Given the primary doughnut shows "Food" aggregating "Restaurants" (20 €) and "Groceries" (30 €)
        When the user clicks the "Food" slice
        Then a secondary pie chart appears next to the primary
        And it shows two slices: "Restaurants" (20 €) and "Groceries" (30 €)
        And the secondary chart title reads "Food"

    Scenario: 12 — Clicking a standalone tag slice (no sub-tags) does not open a secondary chart
        Given the primary doughnut has a slice for "Transport" with no sub-tags
        When the user clicks "Transport"
        Then no secondary chart appears
        And a visual indicator (e.g. a tooltip or label) signals that there are no sub-tags

    Scenario: 13 — Clicking outside or on the same slice again closes the secondary chart
        Given the secondary chart for "Food" is visible
        When the user clicks the "Food" slice again
        Then the secondary chart is dismissed

    Scenario: 14 — The secondary chart is responsive and does not overflow on small screens
        Given the user is on a screen ≤ 640 px wide
        When the secondary chart is open
        Then it appears below the primary chart (stacked layout)
        And neither chart clips or overflows its container

**Notes**
- The `useTag` composable must be extended with an `addSubTag(label, colorDTO, parentId)` method
  that calls `POST /tag/sub-tag`.
- `TagDTO` (in `client/types/index.d.ts`) must be updated to include `parentId?: string | null`.
- `CategoryDataDTO` must be updated to include `subCategories?: CategoryDataDTO[]`.
- The booklet-page tag filter is currently a flat `<SelectButton>` or dropdown; the two-column
  layout requires a new UI component or a restructured section — keep it consistent with existing
  PrimeVue / UnoCSS patterns.
- The secondary pie chart should reuse the same `<Doughnut>` component from Chart.js already
  present in the dashboard page. No new chart library should be introduced.
- Frontend tests (`client/tests/`) must cover:
    - Sub-tag creation dialog toggle (Scenario 1–3).
    - Booklet filter column B appearance when parent is selected (Scenarios 5–8).
    - Secondary chart appearance/dismissal on dashboard (Scenarios 11–13).
