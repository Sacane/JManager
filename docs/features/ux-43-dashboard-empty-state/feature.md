# UX-43 — A useful dashboard before the first booklet

**Context**
With no booklet, the dashboard still renders four KPI cards at 0.00 EUR, three empty charts and an empty account selector. A new user is shown an elaborate but meaningless screen instead of the one thing they need to do first.

**Acceptance Criteria**
Feature: A useful dashboard before the first booklet
  In order to know what to do first
  As a new user
  I want the dashboard to guide me instead of showing empty figures

Scenario: The dashboard invites me to create a booklet
  Given I have no booklet yet
  When I open the dashboard
  Then it explains that I need a booklet and offers to create one

Scenario: No meaningless figure is displayed
  Given I have no booklet yet
  When I open the dashboard
  Then no zero-valued indicator or empty chart is shown

Scenario: The full dashboard returns with a booklet
  Given I created my first booklet
  When I open the dashboard
  Then the usual indicators and charts are displayed

**Notes**
- Layer-agnostic functional acceptance for UX-43. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
