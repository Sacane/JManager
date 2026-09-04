# UX-49 — Legal documents reachable from the login page

**Context**
The terms and the privacy policy are only linked from the registration checkboxes. A visitor on the sign in form, or anyone who simply wants to read them before creating an account, has no way to reach them.

**Acceptance Criteria**
Feature: Legal documents reachable from the login page
  In order to read the terms before committing
  As a visitor
  I want them linked from the login page itself

Scenario: The links are available on the sign in form
  Given I am on the sign in form
  When I look at the page
  Then links to the terms and to the privacy policy are available

Scenario: The links open the documents
  Given I am on the login page
  When I follow the privacy policy link
  Then the privacy policy is displayed

Scenario: I can come back
  Given I opened a legal document from the login page
  When I use the way back
  Then I return to the application

**Notes**
- Layer-agnostic functional acceptance for UX-49. It describes what the user gets, not
  how it is built, and is the reference for acceptance and end-to-end tests.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
