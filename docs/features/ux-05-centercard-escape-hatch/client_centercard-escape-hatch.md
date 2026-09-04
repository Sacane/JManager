# Client Module — Remove the escape link from blocking onboarding screens

**Context**
`layouts/centercard.vue` renders a fixed back arrow pointing to `/` in the bottom-right corner. That
layout is shared by `verify-email`, `consent` and `force-password-change`. The last two are
deliberate walls: the user must accept the terms, or set a new password, before reaching the
application. Even though the middleware redirects them back, the visible arrow invites an escape and
produces a confusing redirect loop. The arrow must be opt-out per route.

**Acceptance Criteria**
Feature: Blocking screens offer no escape affordance
  In order not to be sent into a redirect loop
  As a user going through onboarding
  I want blocking screens to show no way out

Scenario: 1. The consent screen shows no back arrow
  Given my consent is required
  When the consent page is displayed
  Then no back link to the application is rendered

Scenario: 2. The forced password change screen shows no back arrow
  Given I must change my password
  When the forced password change page is displayed
  Then no back link to the application is rendered

Scenario: 3. The email verification screen keeps its back link
  Given I open the email verification page
  When the page is displayed
  Then the back link to the application is still rendered

**Notes**
- Files: `layouts/centercard.vue`, `pages/consent.vue`, `pages/force-password-change.vue`.
- Suggested mechanism: a `route.meta` flag (for example `allowBack`) read by the layout.
- Priority P0 - Effort S - Frontend only.
