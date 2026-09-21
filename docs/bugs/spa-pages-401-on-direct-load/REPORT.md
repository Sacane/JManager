# Bug Report — Public pages answer 401 when opened directly

**Date**: 2026-09-21
**Found while**: specifying UX-21 (password reset), whose email link would hit the same failure.
**Status**: reproduced in production, not fixed.

## Symptom

Opening a page by its URL — from an email, a bookmark, a new tab or a browser reload — returns a raw
JSON 401 instead of the page, for every page the SPA controller does not list:

```
$ curl -s -o /dev/null -w "%{http_code}" https://jmanager.sacane.fr/<path>
```

| Path | Status | Who opens it directly |
|---|---|---|
| `/login`, `/dashboard`, `/booklet`, `/tag` | 200 | — |
| `/verify-email` | **401** | **every user, from the verification email** |
| `/privacy`, `/terms` | **401** | anyone following an external link to the legal pages |
| `/consent`, `/force-password-change` | **401** | a user reloading a blocking screen |
| `/settings` | **401** | a user reloading the settings page |

Body returned for `/verify-email?token=abc`:

```json
{"type":"about:blank","title":"Unauthorized","status":401,
 "detail":"Authentication is required to access this resource", ...}
```

Navigating to these pages from inside the app works, because the Nuxt router changes the URL without
asking the server. That is why nothing looked broken during normal use.

## Impact

- **Email verification cannot be completed from the email.** The link in the welcome and resend emails
  lands on this 401. A user only verifies if they happen to be signed in and navigate there by hand.
- **The privacy policy is unreachable from outside the app.** The consent screen states the rights it
  describes; a policy that cannot be opened by link is a compliance concern, not only a UX one.
- **UX-21 would ship broken**: the password reset link would land on the same 401.

## Root cause

Read from the code, and consistent with every status in the table above — including the permitted
pages that still fail. It has not been stepped through in a debugger; the regression test proposed
below is what should confirm it.

Two lists of SPA routes disagree, and neither is complete.

1. `SpaController.forward()` (`application/.../api/spa/SpaController.kt`) maps only `/`, `/dashboard`,
   `/booklet`, `/login`, `/admin`, `/tag`, `/user`, `/regular-transaction` (and sub-paths) to
   `index.html`.
2. Any other page is a 404 for Spring MVC. The `spaErrorViewResolver` is meant to catch that and forward
   to `index.html` — but the 404 is rendered through an **ERROR dispatch to `/error`**.
3. `SecurityConfig` ends with `authorize(anyRequest, denyAll)` and does not permit `/error`. Spring
   Security 6 authorises every dispatch type, ERROR included, so the error dispatch is denied, and an
   anonymous request gets the authentication entry point's 401.

This also explains why `/privacy`, `/terms`, `/consent` and `/force-password-change` fail **although they
are `permitAll`**: the page request passes security, finds no controller, and dies on the error
dispatch. `/verify-email` and `/settings` are not in `SecurityConfig` at all.

> Pages reach the browser only when they are listed both in `SecurityConfig` and in `SpaController`;
> the fallback designed to cover the rest is itself blocked by `denyAll` on the ERROR dispatch.

## Fix candidates — to decide, not applied

1. **List every page in `SpaController`** and in `SecurityConfig`. Smallest change, but the two lists
   will drift again the next time a page is added — this bug is that drift.
2. **Let the fallback work**: permit the ERROR (and FORWARD) dispatch types, or `/error`, so that the
   existing `spaErrorViewResolver` serves `index.html` for any unknown path. One rule instead of two
   lists. Must be checked so that a genuine API 404 under `/api/**` still answers JSON, not the SPA.
3. **One source of truth** for the page routes, shared by both classes.

Whichever is chosen, a test should request each page path anonymously and expect `index.html`, so the
next missing page fails in CI rather than in a user's mailbox.

## Out of scope here

`SecurityConfig` also lists `/user/**` and `/admin/**` as public page routes. That is correct for an SPA
(the API under `/api/**` is what is protected), noted only so nobody "fixes" it by mistake.
