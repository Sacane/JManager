# UX-21 — Recovering a forgotten password

**Context**
Functional acceptance for UX-21, shared by the domain, infrastructure, application and client issues.
A user who forgets their password has no recovery path at all: no link on the login page, no endpoint
behind it.

Specified on 21 September 2026 against the actual backend, which changed the scope of the first draft.
Decisions taken with the product owner that day:

- **Password rules live in the domain and apply everywhere** — registration, password change, forced
  change and reset. That work is UX-27, which becomes full-stack and ships with UX-21: UX-21 consumes
  the policy, it does not define it.
- **A reset signs out every other session immediately.** Today a session survives any password change
  for up to an hour, because a request is authenticated by the JWT signature alone. The fix applies to
  every way of changing a password, not only to the reset.
- **The reset looks the address up without regard to case.** Addresses are stored as typed and never
  normalised; a user registered as `Johan@…` who types `johan@…` would otherwise receive nothing and,
  because the answer is neutral, never learn why.
- **Production runs behind a reverse proxy, and Spring serves the pages.** This makes the client IP a
  configuration matter, and it makes UX-21 depend on the bug in
  `docs/bugs/spa-pages-401-on-direct-load/` — without that fix, the reset link lands on a 401 exactly as
  the email verification link does today.

**Acceptance Criteria**
Feature: Recovering a forgotten password
  In order to regain access to my account
  As a user who forgot their password
  I want to reset it from a link sent to my email address

Scenario: I request a reset link
  Given I forgot my password
  When I request a reset from the login page with my email address
  Then I receive a reset link at that address, valid for 30 minutes

Scenario: The case of my address does not matter
  Given I registered with an address containing capital letters
  When I request a reset typing it in lower case
  Then I receive the reset link

Scenario: I set a new password
  Given I open a valid reset link
  When I choose a new password that satisfies the password rules
  Then I can sign in with it, and only with it

Scenario: A reset signs out my other sessions
  Given I am signed in on another device
  When I reset my password
  Then that other device is signed out at its next action

Scenario: I am told my password was changed
  Given I reset my password
  When the reset succeeds
  Then I receive an email saying my password was changed

Scenario: An old link cannot be reused
  Given a reset link that is expired, already used, or replaced by a newer one
  When I open it
  Then I am told it is no longer valid and offered to request a new one

Scenario: The flow reveals nothing about accounts
  Given an email address that may or may not be registered
  When a reset is requested for it
  Then the answer is the same in both cases

Scenario: The flow cannot be used to flood a mailbox
  Given many reset requests for the same address
  When they arrive in a short time
  Then only the first few send an email

**Notes**
- Layer-agnostic functional acceptance. It describes what the user gets, not how it is built, and is
  the reference for acceptance and end-to-end tests.
- Implementation order, TDD per layer: **UX-27 domain policy → UX-21 domain → infrastructure →
  application → client**, with the SPA routing bug fixed before the client part.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge cases.
- Priority P1 · Effort XL · Full-stack · Lot 6.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`, `FULLSTACK_PENDING.md`.
