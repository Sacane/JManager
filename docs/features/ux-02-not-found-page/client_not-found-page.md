# Client Module — Build a real 404 page

**Context**
`pages/[...all].vue` currently renders `<h1>Error 404</h1>` only: no layout, no styling, no way back,
and in English while the whole application is in French. Any mistyped or stale URL therefore looks
like a broken application. The page must use the existing `centercard` layout and offer a way back.

**Acceptance Criteria**
Feature: Friendly not-found page
  In order to recover from a wrong URL
  As a visitor
  I want a styled page in French with a way back to the application

Scenario: 1. An unknown URL renders the themed not-found page
  Given I am on the application
  When I navigate to an URL that matches no route
  Then a styled not-found page is displayed in French
  And it offers a link back to the dashboard

Scenario: 2. The page follows the active theme
  Given the dark theme is active
  When I navigate to an URL that matches no route
  Then the not-found page uses the dark design tokens

Scenario: 3. An unauthenticated visitor is not trapped
  Given I am not authenticated
  When I navigate to an URL that matches no route
  Then the not-found page offers a link to the login page

**Notes**
- Files: `pages/[...all].vue`.
- Reuse `layouts/centercard.vue` and the existing typography shortcuts (`heading-2`, `body-base`, `btn-primary`).
- Priority P0 - Effort S - Frontend only.
