# UX-26 — Perceived responsiveness while loading

**Context**
Functional acceptance for UX-26, independent of any layer. Four different loading treatments coexist and no page shows its structure while loading.

**Acceptance Criteria**
Feature: Perceived responsiveness while loading
  In order not to face a blank screen
  As a user
  I want the shape of the page to appear while its data loads

Scenario: The page shape appears immediately
  Given a page is loading its data
  When I wait for it
  Then I see the structure of the page rather than an empty screen

Scenario: The content settles without jumping
  Given a page is showing its loading placeholders
  When the data arrives
  Then the content replaces them without the layout shifting

Scenario: Loading looks the same everywhere
  Given any page of the application is loading
  When I compare it with another loading page
  Then both use the same loading treatment

**Notes**
- Layer-agnostic functional acceptance for UX-26. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
