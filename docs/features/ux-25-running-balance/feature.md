# UX-25 — Following my balance like a bank statement

**Context**
Functional acceptance for UX-25, independent of any layer. The transaction table never shows the balance resulting from each entry.

**Acceptance Criteria**
Feature: Following my balance like a bank statement
  In order to see how my balance evolved
  As an authenticated user
  I want each transaction to show the resulting balance

Scenario: Each transaction shows the resulting balance
  Given I look at the transactions of a booklet in date order
  When the list is displayed
  Then each line shows the balance after that transaction

Scenario: The figures are consistent
  Given the transactions of a period are fully displayed
  When I read the last line
  Then its balance matches the closing balance of the period

Scenario: The column is hidden when it would be misleading
  Given I sort the list by something other than the date
  When the list is displayed
  Then the running balance is hidden with an explanation

**Notes**
- Layer-agnostic functional acceptance for UX-25. It describes what the user gets, not how
  it is built, and is the reference for acceptance and end-to-end tests.
- The `{module}_*.md` files in this folder hold the implementation-level scenarios and edge
  cases for each layer.
- Backlog: `docs/technical/ux-design-review/UX_BACKLOG.md`
