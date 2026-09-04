# UX-02 — Recovering from a wrong URL

**Context**
Functional acceptance for UX-02, independent of any layer. A wrong or outdated URL currently renders an unstyled English heading with no way back.

**Acceptance Criteria**
Feature: Recovering from a wrong URL
  In order not to be stuck on a broken screen
  As a visitor
  I want a clear page and a way back when an address does not exist

Scenario: A wrong address offers a way back
  Given I follow a broken or outdated link
  When the page loads
  Then I see a styled page in French explaining that the address does not exist
  And I can return to the application in one click

Scenario: The page respects my theme
  Given I use the application in dark theme
  When I land on a page that does not exist
  Then the page is displayed in dark theme

**Notes**
- Layer-agnostic functional acceptance for UX-02. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
