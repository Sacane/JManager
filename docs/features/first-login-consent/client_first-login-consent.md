# Client Module — First-Login Consent Gate

## Context

Admin-created beta test accounts have `consentRequired: true` on their first login.
The frontend must intercept this state and display a full-screen consent page before
granting access to the application. The user cannot navigate away until both the TOS
and Privacy Policy are accepted.

This is a one-time flow: once `POST /api/user/consent` succeeds, the gate is never
shown again for that account.

---

## Acceptance Criteria

```gherkin
Feature: First-login consent gate

  In order to comply with GDPR Art. 7 (conditions for consent)
  As a beta-test user logging in for the first time
  I want to be shown a consent screen before accessing the application
  So that my acceptance is recorded with a proper timestamp

  Scenario: User with consentRequired = true sees the consent gate
    Given an authenticated user with consentRequired = true
    When the user completes the login flow
    Then the user is redirected to /consent before any protected page
    And cannot navigate to /dashboard or any other page

  Scenario: User accepts both consents and accesses the app
    Given the user is on the /consent page
    When the user checks both checkboxes and clicks "I accept"
    Then POST /api/user/consent is called with tosAccepted = true and privacyAccepted = true
    And on success the user is redirected to /dashboard
    And the consent gate is never shown again on subsequent logins

  Scenario: User tries to submit with one checkbox unchecked
    Given the user is on the /consent page
    When the user checks only one checkbox and clicks "I accept"
    Then an inline validation error is shown
    And POST /api/user/consent is not called

  Scenario: User with consentRequired = false skips the consent gate
    Given an authenticated user with consentRequired = false
    When the user logs in
    Then the user is redirected directly to /dashboard

  Scenario: API returns 400 — error displayed in form
    Given the user is on the /consent page with both checkboxes checked
    When POST /api/user/consent returns 400
    Then a toast error is shown
    And the user remains on the /consent page
```

---

## UI/UX Acceptance Criteria

- Full-screen layout using the `centercard` layout — no sidebar, no navigation.
- Displays two checkboxes:
  - "I accept the Terms of Service (v{tosVersion})" — links to the TOS document.
  - "I accept the Privacy Policy" — links to the privacy policy document.
- "I accept" button is **disabled** until both checkboxes are checked.
- Loading state on button click (use `useLoading`).
- On success: redirect to `/dashboard`.
- On API error: toast via `useJToast`.
- Works in both light and dark mode.
- Responsive: readable on mobile (single-column, full-width checkboxes).

---

## Implementation Plan

### New page: `client/pages/consent.vue`

- Uses `centercard` layout.
- Fetches `tosVersion` from `useAuth` store or hardcodes current version.
- Calls `useConsent` composable.
- Middleware: redirect to `/dashboard` if already consented.

### New composable: `client/composables/useConsent.ts`

```ts
export function useConsent() {
  const tosAccepted = ref(false)
  const privacyAccepted = ref(false)
  const canSubmit = computed(() => tosAccepted.value && privacyAccepted.value)

  async function submitConsent(tosVersion: string) {
    // POST /api/user/consent
    // on success: navigateTo('/dashboard')
  }

  return { tosAccepted, privacyAccepted, canSubmit, submitConsent }
}
```

### Auth middleware update: `client/middleware/auth.ts`

After a successful login, check `consentRequired` on the response (from `GET /api/user/me`
or embedded in the login response). If `true`, redirect to `/consent`.

### New route: `/consent`

- Public with auth guard (must be logged in but consent can be null).
- No sidebar / navigation visible.

### Tests

| Target | File |
|---|---|
| `pages/consent.vue` | `tests/pages/consent.spec.ts` |
| `composables/useConsent.ts` | `tests/unit/useConsent.spec.ts` |
