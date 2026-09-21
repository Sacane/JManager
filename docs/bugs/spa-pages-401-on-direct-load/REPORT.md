# Bug Report — Public pages answer 401 when opened directly

**Date**: 2026-09-21
**Found while**: specifying UX-21 (password reset), whose email link would hit the same failure.
**Status**: fixed, root cause verified by experiment.

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

- **Email verification could not be completed from the email.** The link in the welcome and resend emails
  landed on this 401. A user only verified if they happened to be signed in and navigate there by hand.
- **The privacy policy was unreachable from outside the app.** The consent screen states the rights it
  describes; a policy that cannot be opened by link is a compliance concern, not only a UX one.
- **UX-21 would have shipped broken**: the password reset link would have landed on the same 401.

## Root cause

Two lists of SPA routes disagreed, and neither was complete. Reproduced locally with an anonymous
browser-like request against a running server (`SpaRoutingTest`), which gave the same statuses as
production, then confirmed by an experiment:

1. `SpaController.forward()` mapped only `/`, `/dashboard`, `/booklet`, `/login`, `/admin`, `/tag`,
   `/user`, `/regular-transaction` (and sub-paths) to `index.html`.
2. `SecurityConfig` whitelisted a similar but different list, and ended with `anyRequest denyAll`.
   `/verify-email`, `/settings` and any new page were in neither, so an anonymous request was refused on
   the first dispatch.
3. A page that **was** whitelisted but had no controller — `/privacy`, `/terms`, `/consent`,
   `/force-password-change` — passed security, found no handler, and produced a 404. Spring Boot renders
   that 404 through an **ERROR dispatch to `/error`**, which Spring Security 6 authorises like any other
   dispatch; `/error` was not permitted, so the anonymous ERROR dispatch was refused with 401.

**Experiment for point 3**: permitting `/error` temporarily turned exactly those four pages from 401 into
404, and left the non-whitelisted ones at 401. The 401 of a whitelisted page therefore came from the error
dispatch, not from the page request.

The `spaErrorViewResolver` in `SpaConfiguration` was meant to catch such 404s and forward to `index.html`,
but it could never run for an anonymous user for the reason above — and even then it would have served the
shell with a 404 status.

> Pages reached the browser only when listed both in `SecurityConfig` and in `SpaController`; the fallback
> designed to cover the rest was itself blocked by `denyAll` on the ERROR dispatch.

## Fix applied

One definition of "a page", shared by both classes: `application/.../api/spa/SpaRoutes.kt`.

A page is now **any path that is not something else**: not under `api`, `actuator`, `error`, `_nuxt` or
`assets`, and with no dot in its first segment (a dot marks a file such as `/favicon.ico`). Both
`SpaController` and `SecurityConfig` use `SpaRoutes.PAGE` and `SpaRoutes.NESTED_PAGE`, so there is no list
to keep in step with `client/pages`: a new page needs no backend change, and an unknown path gets the
client's own 404 screen with a 200 status.

Points worth knowing:

- **The reserved prefixes are security-relevant.** A mutation check reducing them to `error` made the
  guard test fail and showed why: `/actuator/env` and `/actuator/heapdump` were served, and so were
  missing `_nuxt` scripts as HTML. A request mapping is consulted before the resource handlers, so without
  the exclusion the controller would answer a script request with the shell.
- **The constraint regex may not contain a capture group.** The first attempt used `(api|actuator|…)` and
  Spring refused it at match time (`No capture groups allowed in the constraint regex`) — see the next
  section for why that surfaced as a 401 on every request.
- The four route tests in `SpaControllerTest` each called `forward()` with no argument and asserted the
  same string; they could not detect a missing page and were replaced by one honest test.
- `SpaRoutingTest` opens 18 paths anonymously with a browser-like `Accept: text/html`, including the pages
  that used to fail, both pages UX-21 adds and one path no page owns; it also checks that the API, the
  actuator and the static directories are not served the shell, and that protected endpoints still answer
  401.

## Found on the way — not fixed here

`JwtCookieAuthenticationFilter` wraps `filterChain.doFilter(...)` in a `try` whose `catch (Exception)`
answers **401 "Unauthorized"**. Any exception thrown further down the chain — here, a route pattern refused
at match time — is therefore reported to the client as an authentication failure, on every request, public
pages included. A server error looks like a bad session. See
`docs/backlog/jwt-filter-turns-server-errors-into-401.md`.

The same finding explains the earlier diagnosis being slower than it needed to be: with no Spring profile
active, Logback prints nothing (`docs/backlog/logback-silent-without-active-profile.md`), so the exception
behind the 401 was invisible until the tests ran with `SPRING_PROFILES_ACTIVE=test`.
