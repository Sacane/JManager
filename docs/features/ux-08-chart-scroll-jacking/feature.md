# UX-08 — Predictable scrolling over charts

**Context**
Functional acceptance for UX-08, independent of any layer. The dashboard charts currently capture the wheel to rescale their axis, trapping the page scroll.

**Acceptance Criteria**
Feature: Predictable scrolling over charts
  In order to browse a long page
  As an authenticated user
  I want the wheel to scroll unless I explicitly ask to zoom

Scenario: Scrolling over a chart scrolls the page
  Given my pointer is over a chart
  When I scroll with the wheel
  Then the page scrolls and the chart is unchanged

Scenario: Zooming is deliberate
  Given my pointer is over a chart
  When I scroll while holding the zoom modifier key
  Then the chart scale changes and the page does not scroll

Scenario: Zooming is reversible
  Given I changed a chart scale
  When I look at the chart
  Then a control lets me restore the automatic scale

**Notes**
- Layer-agnostic functional acceptance for UX-08. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
