# Client Module — Add a My Account section with account deletion

**Context**
The settings page has no account section: no username, no email, no creation date, and no way to
delete the account. The privacy policy advertises GDPR rights and the consent page states that they
can be exercised "at any time from your settings", which is currently false. The backend already
exposes `DELETE /api/user/me`, which no screen calls, so closing this gap is frontend work only.
Data portability export has no endpoint yet and is out of scope of this issue.

**Acceptance Criteria**
Feature: My Account section
  In order to exercise my rights over my account
  As an authenticated user
  I want to see my account information and be able to delete my account

Scenario: 1. My account information is displayed
  Given I open the settings page
  When the account section is rendered
  Then my username and my email address are displayed

Scenario: 2. Deleting my account requires an explicit confirmation
  Given I am on the account section
  When I ask to delete my account
  Then a confirmation asking me to type my username is displayed
  And the deletion is only sent once the confirmation matches

Scenario: 3. A confirmed deletion signs me out
  Given I confirmed the deletion of my account
  When the deletion succeeds
  Then I am signed out and redirected to the login page

Scenario: 4. A failed deletion keeps me signed in
  Given I confirmed the deletion of my account
  When the deletion request fails
  Then an error is displayed and I stay signed in

**Notes**
- Backend endpoint already available: `DELETE /api/user/me` (`application/.../api/session/Controller.kt`).
- Files: `pages/settings/index.vue`, `composables/useAuth.ts`.
- Data export (GDPR portability) needs a new endpoint and must be a separate issue.
- Priority P1 - Effort M - Frontend only.
