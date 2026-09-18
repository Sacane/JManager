# Client Module — Show the running balance on the transaction list

> **CORRECTION — this issue cannot be delivered as written (frontend-only).**
> The context below claims the opening balance of the period is available through the balances
> endpoint. Reading the domain shows it is not: `realSold` is the booklet's persisted amount over
> every confirmed transaction ever, date-blind, and unaffected by `month`, `year`, `startDate` or
> `endDate`. No date-scoped balance exists in the response.
> A running balance therefore cannot be derived client side for a past month or a paginated view.
> Full analysis and the two possible backend shapes:
> `docs/backlog/booklet-balances-endpoint-has-no-opening-balance.md`.
> **This item is full-stack, not frontend-only.** Its effort and lot assignment in `UX_BACKLOG.md`
> need revising before it is picked up again.

**Context**
The booklet detail table shows the date, label, expense, income and tags of each transaction but
never the resulting balance, which is standard in a bank statement. The opening balance of the period
is already available through the balances endpoint, so the running balance can be derived client side
for the loaded rows when they are ordered by date.

**Acceptance Criteria**
Feature: Running balance per transaction
  In order to follow how my balance evolves
  As an authenticated user
  I want each transaction to show the resulting balance

Scenario: 1. The running balance is displayed in date order
  Given a booklet with an opening balance and transactions sorted by ascending date
  When the table is rendered
  Then each row displays the balance resulting from that transaction

Scenario: 2. The last row matches the period balance
  Given a booklet whose transactions are fully loaded for the period
  When the table is rendered
  Then the balance of the last row equals the closing balance of the period

Scenario: 3. The column is hidden under an incompatible sort
  Given the table is sorted by a column other than the date
  When the table is rendered
  Then the running balance column is hidden with an explanation

Scenario: 4. Forecast transactions are visually distinguished
  Given the period holds forecast transactions
  When the running balance is displayed
  Then the rows resulting from forecast transactions stay visually distinguished

**Notes**
- Files: `pages/booklet/[id].vue`, `composables/useBooklet.ts`.
- Constraint: the list is paginated and sorted server side, so a client-side running balance is only
  meaningful under an ascending date sort. If the product needs it under every sort, a server-side
  computation must be opened as a separate full-stack issue.
- Priority P1 - Effort M - Frontend only.
