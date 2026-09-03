# Client Module — Remove the dead actions button from the admin console

**Context**
Each user row of `pages/admin/index.vue` renders a `pi pi-ellipsis-v` button with no `@click`
handler, in both the desktop table and the mobile card list. It cannot be wired today either:
`AdminController` only exposes `POST /api/admin/users` and `GET /api/admin/users`, with no endpoint
to update, disable or delete a user. A control that does nothing erodes trust in the whole console,
so it must be removed until the backend supports the actions (tracked separately).

**Acceptance Criteria**
Feature: No inert control in the admin console
  In order to trust what the console offers
  As an administrator
  I want every visible control to have an effect

Scenario: 1. The desktop user table has no dead actions column
  Given I am an administrator on the users tab
  When the user table is rendered on desktop
  Then no actions column with an inert button is displayed

Scenario: 2. The mobile user list has no dead actions button
  Given I am an administrator on the users tab
  When the user list is rendered on mobile
  Then no inert actions button is displayed on the user cards

Scenario: 3. The remaining user information is unchanged
  Given I am an administrator on the users tab
  When the user list is rendered
  Then username, email, role and creation date are still displayed

**Notes**
- Files: `pages/admin/index.vue` (`adminUserColumns` and the mobile card template).
- The real admin actions require new backend endpoints and are tracked as a separate P2 item (UX-38).
- Priority P0 - Effort XS - Frontend only.
