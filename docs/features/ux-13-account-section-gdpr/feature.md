# UX-13 — Control over my account

**Context**
Functional acceptance for UX-13, independent of any layer. The settings page has no account section, while the consent screen promises that GDPR rights can be exercised from there.

**Acceptance Criteria**
Feature: Control over my account
  In order to exercise my rights over my data
  As an authenticated user
  I want to see my account information and be able to delete my account

Scenario: I can see my account information
  Given I am signed in
  When I open my settings
  Then I see my username and my email address

Scenario: I can delete my account deliberately
  Given I am on my account section
  When I ask to delete my account
  Then I must confirm by typing my username before it proceeds

Scenario: A confirmed deletion signs me out
  Given I confirmed the deletion of my account
  When the deletion succeeds
  Then I am signed out and taken back to the login page

**Notes**
- Layer-agnostic functional acceptance for UX-13. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
