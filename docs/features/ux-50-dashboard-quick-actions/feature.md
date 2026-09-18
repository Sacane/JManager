# UX-50 — Acting from the dashboard

**Context**
Functional acceptance for UX-50, independent of any layer. The dashboard's "Actions rapides" block
offers three buttons that only navigate to pages the sidebar already links to. The actions a user
actually comes to the dashboard for — recording a transaction, importing a statement, setting up a
recurring entry — are not available there.

**Acceptance Criteria**
Feature: Acting from the dashboard
  In order to record what happened without leaving the overview
  As an authenticated user
  I want the dashboard's quick actions to do something rather than navigate

Scenario: I can record a transaction from the dashboard
  Given I am on the dashboard with an account selected
  When I use the quick action to add a transaction
  Then I can record it without leaving the dashboard

Scenario: The action names the account it applies to
  Given I am on the dashboard with an account selected
  When I look at the quick actions
  Then the ones acting on a single account name that account

Scenario: The dashboard reflects what I just recorded
  Given I recorded a transaction from the dashboard
  When the recording succeeds
  Then the dashboard figures are refreshed

Scenario: The quick actions no longer duplicate the sidebar
  Given I am on the dashboard
  When I look at the quick actions
  Then none of them is a plain link to a page the sidebar already offers

**Notes**
- Layer-agnostic functional acceptance for UX-50. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
