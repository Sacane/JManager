# UX-04 — Every visible control has an effect

**Context**
Functional acceptance for UX-04, independent of any layer. The admin user rows display an actions button that is wired to nothing.

**Acceptance Criteria**
Feature: Every visible control has an effect
  In order to trust the console I am using
  As an administrator
  I want every control I can see to actually do something

Scenario: No inert control on a user row
  Given I am an administrator viewing the user list
  When I look at a user row
  Then every control displayed on it performs an action when I use it

Scenario: The user information is preserved
  Given I am an administrator viewing the user list
  When the list is displayed
  Then I still see the username, the email, the role and the creation date

**Notes**
- Layer-agnostic functional acceptance for UX-04. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
