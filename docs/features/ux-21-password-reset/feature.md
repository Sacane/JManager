# UX-21 — Recovering a forgotten password

**Context**
Functional acceptance for UX-21, shared by the domain, infrastructure, application and client issues. A user who forgets their password currently has no recovery path at all.

**Acceptance Criteria**
Feature: Recovering a forgotten password
  In order to regain access to my account
  As a user who forgot their password
  I want to reset it from a link sent to my email address

Scenario: I request a reset link
  Given I forgot my password
  When I request a reset from the login page
  Then I receive a reset link at my registered email address

Scenario: I set a new password
  Given I open a valid reset link
  When I choose a new password
  Then I can sign in with it

Scenario: An old link cannot be reused
  Given a reset link that is expired or already used
  When I open it
  Then I am told it is no longer valid and offered a new one

Scenario: The flow reveals nothing about accounts
  Given an email address that may or may not be registered
  When a reset is requested for it
  Then the response is the same in both cases

**Notes**
- Layer-agnostic functional acceptance for UX-21. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
