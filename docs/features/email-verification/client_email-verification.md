# Client Module — Email Verification

**Context**
Frontend surfaces for the inaugural feature flag, all gated by `useFeatureFlags().isEnabled("email-verification")`.
The verification email link lands on a client page that confirms the token; for unverified simple users the app
shows a clear header banner and a settings indicator. Beta testers are auto-confirmed by following their access
link, so no banner is expected for them once confirmed.

**Scope (client)**
- A `verify-email` page reading the `token` query param, calling `POST /verify-email`, and showing success /
  expired / error states with a path forward (e.g. resend).
- A header banner in `NHeader.vue` shown when the flag is enabled and `emailVerified === false`, clearly stating
  the email is not verified, with a resend/confirm action.
- A settings-page indicator showing verification status and a resend button.
- All of the above hidden entirely when the `email-verification` flag is disabled.

**Acceptance Criteria**
Feature: Email verification UI
  In order to know and fix my unverified status
  As a user
  I want clear banners, a settings indicator, and a verification landing page

Scenario: Verification landing succeeds
  Given the flag "email-verification" is enabled
  And the user opens /verify-email?token=<valid>
  When the page calls the verify endpoint and it succeeds
  Then the user sees a confirmation message
  And the unverified banner no longer appears

Scenario: Verification landing with an expired token
  Given the user opens /verify-email?token=<expired>
  When the verify call returns an expired error
  Then the page shows an "link expired" message with a resend option

Scenario: Header banner for an unverified simple user
  Given the flag "email-verification" is enabled
  And the authenticated user has emailVerified = false
  When any authenticated page renders
  Then a clear "email not verified" banner is shown in the header

Scenario: No banner when email is verified
  Given the authenticated user has emailVerified = true
  When any authenticated page renders
  Then no unverified banner is shown

Scenario: Settings shows verification status and resend
  Given the flag "email-verification" is enabled
  And the user has emailVerified = false
  When the user opens their settings
  Then the settings clearly indicate the email is not verified
  And a resend button is available

Scenario: All UI hidden when the flag is disabled
  Given the flag "email-verification" is disabled
  When authenticated pages and settings render
  Then no verification banner, indicator, or landing behaviour is active
