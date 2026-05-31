# Client Module — Email Verification

**Context**
The frontend surfaces the unverified state in two places: a warning indicator on the "Paramètres" tab
(visible on every authenticated page), and a dedicated section inside the settings page. A separate
`/verify-email` page handles the landing from the email link.

Admin-created users have `emailVerified = true` from day one and never see any of these surfaces.
Only self-registered users with `emailVerified = false` are affected.

**Scope (client)**
- **Paramètres tab indicator** — when `emailVerified === false`, the "Paramètres" navigation tab shows
  a warning badge or icon. Hovering it displays a tooltip: *"Votre compte doit être vérifié"*.
  Disappears once `emailVerified` becomes `true`.
- **Settings page section** — a visible block inside the settings page when `emailVerified === false`:
  - Displays the user's email address.
  - Clear message that the address is not yet verified.
  - A *"Vérifier mon e-mail"* button that calls `POST /verify-email/resend` and shows feedback
    (success toast / error state).
  - Hidden entirely when `emailVerified === true`.
- **`/verify-email` page** — public route, reads the `token` query param, calls
  `GET /verify-email?token=...`, and renders:
  - **Success**: confirmation message, link to log in or go to the dashboard.
  - **Expired link**: error message with a *"Renvoyer un e-mail"* button calling resend (if authenticated)
    or a note to log in first.
  - **Unknown/invalid**: generic error message.
- Refresh the `emailVerified` state in the composable / store after a successful verification so all
  indicators update immediately without a full reload.

**Acceptance Criteria**
Feature: Email verification UI
  In order to know and fix my unverified status
  As a self-registered user
  I want a tab indicator, a settings section, and a verification landing page

Scenario: Paramètres tab shows warning badge for unverified user
  Given an authenticated user with emailVerified = false
  When any authenticated page renders
  Then the "Paramètres" tab displays a warning indicator
  And hovering it shows the tooltip "Votre compte doit être vérifié"

Scenario: No warning badge when email is verified
  Given an authenticated user with emailVerified = true
  When any authenticated page renders
  Then the "Paramètres" tab shows no warning indicator

Scenario: Settings section shows email and resend button when unverified
  Given an authenticated user with emailVerified = false
  When the user opens the settings page
  Then a section shows the user's email address with an "not verified" indication
  And a "Vérifier mon e-mail" button is visible

Scenario: Settings section is hidden when email is verified
  Given an authenticated user with emailVerified = true
  When the user opens the settings page
  Then no unverified email section is rendered

Scenario: Resend button dispatches a new email and shows feedback
  Given an authenticated user with emailVerified = false on the settings page
  When the user clicks "Vérifier mon e-mail"
  Then POST /verify-email/resend is called
  And a success toast confirms the email was sent

Scenario: Verification landing — success
  Given the user opens /verify-email?token=<valid>
  When the page calls GET /verify-email?token=<valid> and receives 200
  Then a confirmation message is displayed
  And the emailVerified state is refreshed in the app

Scenario: Verification landing — expired link
  Given the user opens /verify-email?token=<expired>
  When the page calls GET /verify-email?token=<expired> and receives 410
  Then the page shows "lien expiré" with an option to resend or log in first

Scenario: Verification landing — unknown token
  Given the user opens /verify-email?token=<unknown>
  When the page calls GET /verify-email?token=<unknown> and receives 404
  Then the page shows a generic error message
