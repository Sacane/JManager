# Client Module — Copy Diagnostic Context to Clipboard on Error

**Context**
When an error occurs, users need to send diagnostic information to support (via email or a report form).
Once the backend exposes a `requestId` and `userId` in every error response, the frontend must surface
these values in a way that the user can copy with a single click, ready to paste into a support message.

The copy action must be frictionless: one button, immediate feedback, no manual selection required.

**Scope (client)**
- Extend `ApiProblemDetail` type (`utils/errorCodeMap.ts`) with optional `requestId: string` and
  `userId: string` fields.
- Update `useJToast.errorAxios()` (or a shared helper) to detect the presence of `requestId` in the
  error response and, when present, append a **"Copier les infos de débogage"** action to the toast.
- Clicking that action copies a formatted diagnostic block to the clipboard:
  ```
  requestId: <value>
  userId: <value>       ← omitted when absent
  code: <value>
  date: <ISO timestamp>
  ```
- After a successful copy, replace the button label with **"Copié !"** for 2 seconds, then restore it.
- Errors without a `requestId` (e.g. network-only failures before the server responds) show the
  existing toast behaviour unchanged — no regression.
- No backend, domain, or infrastructure changes are part of this issue.

**Acceptance Criteria**
Feature: Copy diagnostic context to clipboard on error
  In order to include precise information in my support report
  As a user who encounters an error
  I want to copy the diagnostic context to my clipboard with one click

Scenario: Error response includes requestId — diagnostic button appears
  Given an authenticated user whose action triggers an API error
  And the error response includes a "requestId" field
  When the error toast appears
  Then a "Copier les infos de débogage" button is visible inside the toast

Scenario: Clicking the button copies the diagnostic block
  Given an error toast with a "Copier les infos de débogage" button
  When the user clicks the button
  Then the clipboard contains requestId, userId (if present), error code, and an ISO timestamp
  And the button label changes to "Copié !" for approximately 2 seconds then reverts

Scenario: Error response without requestId — no diagnostic button
  Given a network error or a legacy error response without "requestId"
  When the error toast appears
  Then no "Copier les infos de débogage" button is shown
  And the toast behaves exactly as before

Scenario: userId is omitted from the copied block when absent
  Given an unauthenticated request that returns a 401 error with requestId but no userId
  When the user copies the diagnostic block
  Then the clipboard text includes "requestId" but does NOT include a "userId" line
