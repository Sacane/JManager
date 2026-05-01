# Dashboard Charts — Mouse Wheel Y-Axis Scale Adjustment — Client Module

**Context**
On the dashboard page, the "Évolution des finances" (Line) and "Comparaison de période" (Bar) charts share
a common `chartOptionsComputed` object built from `chartOptions`, which currently relies on Chart.js
auto-scaling (`beginAtZero: true`) with no user control over the Y-axis range.

Users want to adjust the visible Y-axis scale interactively by scrolling the mouse wheel over a chart:
- Scrolling **forward** (toward the screen) zooms the Y-axis **in**: the visible range narrows around the
  current data, reducing both the min and max bounds proportionally (e.g. 1000–6000 → 500–4000).
- Scrolling **backward** (away from the screen) zooms the Y-axis **out**: the visible range expands,
  increasing both bounds proportionally (e.g. 1000–6000 → 0–8000).

The interaction must prevent the default page scroll while the pointer is inside a chart container.
When the displayed period or booklet changes, the custom scale must reset to let Chart.js re-auto-scale.

Both charts are rendered via `vue-chartjs` (`<Line>` and `<Bar>`) in `client/pages/dashboard/index.vue`
and driven by `chartOptionsComputed`.

**Acceptance Criteria**

Feature: Mouse wheel adjusts the Y-axis scale range of dashboard charts

    Scenario: 1 — Scrolling forward over a chart zooms the Y-axis in
        Given a user is on the dashboard page
        And the "Évolution des finances" or "Comparaison de période" chart is rendered with data
        When the user scrolls the mouse wheel forward (toward the screen) over the chart container
        Then the Y-axis maximum decreases
        And the Y-axis minimum decreases
        And the visible range becomes narrower than the previous range
        And the chart re-renders to reflect the new Y-axis bounds

    Scenario: 2 — Scrolling backward over a chart zooms the Y-axis out
        Given a user is on the dashboard page
        And the "Évolution des finances" or "Comparaison de période" chart is rendered with data
        When the user scrolls the mouse wheel backward (away from the screen) over the chart container
        Then the Y-axis maximum increases
        And the Y-axis minimum decreases (or stays at 0 if already at minimum)
        And the visible range becomes wider than the previous range
        And the chart re-renders to reflect the new Y-axis bounds

    Scenario: 3 — Page scroll is prevented while the wheel is used over a chart
        Given a user is on the dashboard page
        And the page content is scrollable
        When the user scrolls the mouse wheel over a chart container
        Then the default browser scroll action on the page is prevented
        And only the chart Y-axis scale changes

    Scenario: 4 — Custom Y-axis scale resets when the selected period changes
        Given a user is on the dashboard page
        And the user has adjusted the Y-axis scale via the mouse wheel
        When the user changes the selected period (month / quarter / year)
        Then the custom Y-axis min and max are cleared
        And the chart reverts to automatic Chart.js scaling

    Scenario: 5 — Custom Y-axis scale resets when the selected booklet changes
        Given a user is on the dashboard page
        And the user has adjusted the Y-axis scale via the mouse wheel
        When the user selects a different booklet
        Then the custom Y-axis min and max are cleared
        And the chart reverts to automatic Chart.js scaling

    Scenario: 6 — Both Line and Bar charts respond to the wheel interaction independently
        Given a user is on the dashboard page
        And both charts are rendered
        When the user scrolls the mouse wheel over the Line chart container
        Then only the Line chart Y-axis scale changes
        When the user scrolls the mouse wheel over the Bar chart container
        Then only the Bar chart Y-axis scale changes

    Scenario: 7 — No crash or error when chart has no data
        Given a user is on the dashboard page
        And the selected period has no financial data (empty datasets)
        When the user scrolls the mouse wheel over a chart container
        Then no JavaScript error is thrown
        And the empty chart remains displayed without change

**Notes**
- The wheel step (zoom increment) should be proportional to the current range to keep the interaction
  feeling natural across very different amount scales.
- Use a `wheel` event listener attached to each `chart-container` div wrapping `<Line>` and `<Bar>`.
- Expose two reactive refs per chart (`chartYMin` / `chartYMax`) and inject them into `chartOptionsComputed`
  when they are set (non-null), falling back to Chart.js auto-scaling otherwise.
- The `chartOptionsComputed` computed property is shared; consider splitting into per-chart computed
  options or passing overrides via a factory function to support independent axis states.
- Do not use the `chartjs-plugin-zoom` library; implement the wheel logic natively to avoid adding
  a heavy dependency.
