# Client Module — Stop the dashboard charts from hijacking the scroll

**Context**
`pages/index.vue` binds `@wheel.prevent` on the line and bar chart containers to zoom the Y axis.
The dashboard is several screens tall and the line chart spans the full width, so a user scrolling
the page with the pointer over a chart is trapped and silently rescales the axis. There is also no
way to reset the scale once it has been overridden.

**Acceptance Criteria**
Feature: Predictable scrolling over the dashboard charts
  In order to browse my dashboard
  As an authenticated user
  I want the wheel to scroll the page unless I explicitly ask to zoom

Scenario: 1. Plain wheel scrolls the page
  Given the pointer is over a dashboard chart
  When I scroll with the wheel without any modifier key
  Then the page scrolls and the chart scale is unchanged

Scenario: 2. Modifier plus wheel zooms the chart
  Given the pointer is over a dashboard chart
  When I scroll with the wheel while holding the control or command key
  Then the chart Y axis is rescaled and the page does not scroll

Scenario: 3. A reset control restores automatic scaling
  Given I rescaled a chart axis manually
  When I activate the reset scale control
  Then the chart returns to automatic scaling
  And the reset control is hidden again

**Notes**
- Files: `pages/index.vue` (`onLineChartWheel`, `onBarChartWheel`, `applyWheelToScale`).
- The reset control must appear only while `lineChartYMin`/`lineChartYMax` (or the bar equivalents) are non-null.
- Priority P0 - Effort S - Frontend only.
