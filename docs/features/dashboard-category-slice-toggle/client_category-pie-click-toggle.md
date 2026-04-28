# Dashboard — "Dépenses par catégorie": Toggle Amount/Percentage on Slice Click — Client Module

**Context**
On the dashboard page, the "Dépenses par catégorie" doughnut chart currently only shows amounts and percentages combined inside the tooltip. Users cannot quickly highlight which proportion of their spending a given category represents. The request is twofold:
1. Clicking a slice of the doughnut chart should toggle the **center label** of the chart between the selected slice's amount (e.g. `42.50 €`) and its percentage (e.g. `23.5%`). Clicking the same slice again cycles back to the amount. Clicking a different slice resets to the amount view for the newly selected slice.
2. The euro sign (`€`) must be consistently appended to every amount displayed inside the chart area (center label and tooltip callbacks).

The chart is rendered via `vue-chartjs` (`<Doughnut>`) in `client/pages/dashboard/index.vue`, driven by `categoryExpensesData` and `doughnutOptionsComputed`.

**Acceptance Criteria**

Feature: Pie slice click toggles amount/percentage display in the doughnut chart

    Scenario: 1 — Clicking a slice for the first time shows its amount in the center label
        Given a user is on the dashboard page
        And the "Dépenses par catégorie" chart has loaded with at least one slice
        When the user clicks on a slice
        Then the center of the doughnut chart displays the amount of that slice formatted as "X.XX €"
        And the slice is visually highlighted (offset or opacity change)

    Scenario: 2 — Clicking the same slice a second time toggles the center label to percentage
        Given a user is on the dashboard page
        And a slice is already selected and its amount is displayed in the center
        When the user clicks the same slice again
        Then the center label switches to display the percentage of that slice formatted as "XX.X%"

    Scenario: 3 — Clicking the same slice a third time toggles back to amount
        Given a user is on the dashboard page
        And a slice is selected and its percentage is displayed in the center
        When the user clicks the same slice again
        Then the center label switches back to the amount formatted as "X.XX €"

    Scenario: 4 — Clicking a different slice resets to amount view
        Given a user is on the dashboard page
        And slice A is selected showing a percentage
        When the user clicks on slice B (a different category)
        Then slice B becomes selected
        And the center label displays slice B's amount formatted as "X.XX €"

    Scenario: 5 — Center label shows no selection hint when no slice has been clicked
        Given a user is on the dashboard page
        And no slice has been clicked yet (initial state)
        When the chart is rendered
        Then the center of the doughnut chart shows no label or a neutral hint (e.g. total expenses)

    Scenario: 6 — Tooltip continues to display both amount (with € sign) and percentage
        Given a user is on the dashboard page
        And the chart is loaded with multiple slices
        When the user hovers over any slice
        Then the tooltip shows the slice label, amount formatted as "X.XX €", and percentage formatted as "XX.X%"

    Scenario: 7 — No regression when there are no category data
        Given a user is on the dashboard page
        And the selected period has no expense categories
        When the chart is rendered
        Then the chart remains empty
        And no center label is displayed

    Scenario: 8 — Toggle is accessible on mobile (touch)
        Given a user is on the dashboard page on a small screen (≤ 640 px wide)
        And the doughnut chart is rendered with the legend positioned at the bottom
        When the user taps a slice
        Then the center label appears with the slice's amount formatted as "X.XX €"
        And tapping the same slice again toggles to the percentage
        And the center label overlay does not overflow the chart container or overlap the legend

Feature: Chart container uses a horizontal rectangle layout

    Scenario: 9 — Chart container is wider than tall on desktop
        Given a user is on the dashboard page on a desktop screen (> 640 px wide)
        When the "Dépenses par catégorie" section is rendered
        Then the chart container has a horizontal rectangle shape (wider than it is tall)
        And the doughnut circle occupies the left portion of the container
        And the legend labels occupy the right portion without crowding the circle

    Scenario: 10 — Chart container adapts gracefully on mobile
        Given a user is on the dashboard page on a small screen (≤ 640 px wide)
        When the "Dépenses par catégorie" section is rendered
        Then the chart container stacks the doughnut circle above the legend (bottom position)
        And the container height is sufficient to display both without overflow or clipping

    Scenario: 11 — No regression on chart readability after layout change
        Given the chart container has been resized to a horizontal rectangle
        When category expense data is loaded with multiple slices
        Then the doughnut circle remains centered and fully visible inside the container
        And slice labels in the legend remain readable and properly aligned

**Notes**
- The interaction logic (selected slice index + toggle state) must be managed with a reactive `ref` in `client/pages/dashboard/index.vue`.
- The center label should be implemented as a Chart.js custom plugin passed via `doughnutOptionsComputed` or as a Vue overlay element positioned absolutely inside the chart container.
- Prefer a Vue overlay (`<div>` absolutely positioned over the chart canvas) over a Chart.js inline plugin for easier reactivity with Vue's data model.
- The `onClick` handler must be wired through Chart.js `options.onClick` to capture the clicked element index. On touch devices, Chart.js maps touch events to the same `onClick` handler — no extra touch handling required.
- The `€` sign is already present in the tooltip callback — ensure it is also present in the center label.
- **Responsive / mobile**: the existing `isSmallScreen` ref (≤ 640 px) already moves the legend to the bottom on small screens. The center overlay must not overlap the legend in that layout; use `pointer-events-none` and size the overlay relative to the `cutout` (65%) so it always fits inside the donut hole regardless of chart height.
- The overlay font size should scale with screen size (e.g. `text-base` on desktop, `text-sm` on mobile).
- Unit tests covering the toggle logic and the center label rendering must be added or updated in `client/tests/pages/dashboard-index.spec.ts`.
- **Chart container layout**: Replace the current fixed `h-70` height on `.chart-container` inside the "Dépenses par catégorie" card with a horizontal rectangle shape. Use an aspect-ratio utility (e.g. `aspect-[16/7]`) together with `w-full` so the container naturally scales wide rather than tall, keeping the Chart.js `maintainAspectRatio: false` and `responsive: true` flags in sync.
- On desktop (> 640 px), the right-side legend benefits from the wider container — no extra padding needed.
- On mobile (≤ 640 px), the legend already moves to the bottom (`isSmallScreen`); the container should fall back to a fixed height (e.g. `h-72`) so the stacked circle + legend do not get squeezed by the aspect-ratio constraint.
- The center overlay `<div>` must remain sized relative to the donut `cutout` (65%) and must not be affected by the wider container shape.
