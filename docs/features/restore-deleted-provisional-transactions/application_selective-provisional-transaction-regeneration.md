# Application Module — Selective Regeneration of Deleted Provisional Transactions

**Context**
`BookletController` currently exposes a single `POST {bookletID}/transactions/regenerate?month=&year=` endpoint that blindly restores every excluded regular transaction for the target month. To support letting the user preview and pick which deleted forecast transactions to restore (see the domain-module issue), this endpoint must accept an explicit selection, and a new read-only endpoint must let the client fetch the list of candidates (label, amount, date) to display before the user chooses.

This replaces the current one-click behaviour: the "regenerate" REST contract becomes selection-based end to end, there is no blind "restore all" endpoint kept alongside it.

**Acceptance Criteria**
```gherkin
Feature: REST endpoints for selective regeneration of deleted provisional transactions
  In order to let the client show and act on a precise list of deleted forecast transactions
  As the frontend application
  I want dedicated endpoints to fetch regenerable candidates and to regenerate only the selected ones

Scenario: 1. Fetch regenerable candidates for the current month
  Given an authenticated user who owns a booklet with excluded provisional transactions for the current month
  When the client requests GET the regenerable transactions for that booklet, month and year
  Then the response contains one entry per excluded regular transaction with its label, amount, isIncome, tag and date

Scenario: 2. Fetch regenerable candidates when nothing is excluded
  Given an authenticated user who owns a booklet with no excluded regular transactions for the requested month
  When the client requests GET the regenerable transactions for that booklet, month and year
  Then the response contains an empty list

Scenario: 3. Regenerate a selected subset of provisional transactions
  Given an authenticated user who owns a booklet with several excluded regular transactions for the current month
  When the client requests POST regeneration for that booklet, month and year with a list of selected regular transaction identifiers
  Then the response contains only the regenerated transactions matching the selected identifiers
  And the response type reflects PREVISIONAL for the current month

Scenario: 4. Regenerate a selected subset of virtual transactions for a future month
  Given an authenticated user who owns a booklet with several excluded regular transactions for a future month
  When the client requests POST regeneration for that booklet, month and year with a list of selected regular transaction identifiers
  Then the response contains only the regenerated virtual transactions matching the selected identifiers
  And the response type reflects VIRTUAL for that future month

Scenario: 5. Regenerating with an empty selection is rejected
  Given an authenticated user who owns a booklet
  When the client requests POST regeneration with an empty list of regular transaction identifiers
  Then the API returns a 400 Bad Request response

Scenario: 6. Regenerating for a past month returns no transactions
  Given an authenticated user who owns a booklet
  When the client requests POST regeneration for a past month with one or more selected regular transaction identifiers
  Then the response contains an empty transaction list
  And the response type reflects NONE

Scenario: 7. Fetching regenerable candidates for a booklet the user does not own is rejected
  Given an authenticated user who does not own the requested booklet
  When the client requests GET the regenerable transactions for that booklet
  Then the API returns a 404 Not Found response
```

**Notes**
- Update `RegenerateTransactionsResponse`/`RegenerationType` mapping if needed to stay consistent with the selective flow.
- The GET candidates endpoint needs its own response DTO (e.g. `RegenerableTransactionDTO`: regularTransactionId, label, amount, isIncome, tagDTO, date) — align its shape with `TransactionResultDTO` where practical to ease reuse on the client.
- Keep contract naming consistent with the domain module's chosen command/query shape (`docs/features/restore-deleted-provisional-transactions/domain_*.md`).
