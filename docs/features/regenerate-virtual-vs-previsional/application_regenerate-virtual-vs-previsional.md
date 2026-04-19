# Bug: Transaction Regeneration — Distinguish Previsional vs Virtual (Application)

**Context**
The endpoint `POST /api/booklet/{bookletID}/transactions/regenerate?month=X&year=Y` currently calls `BookletFeature.regenerateDeletedPrevisionalTransactions` and always returns a `List<TransactionResult>`.

Following the domain layer fix, the endpoint behavior must reflect the distinction between current month, future month, and past month:
- **Current month**: returned transactions are persisted previsional transactions.
- **Future month**: returned transactions are virtual (not persisted).
- **Past month**: empty list returned, no effect.

The HTTP response must let the frontend know whether returned transactions are virtual or previsional, so the UI can adapt accordingly.

**Acceptance Criteria**

Feature: Regeneration endpoint — previsional vs virtual distinction

Scenario: Regeneration for the current month returns 200 with previsional transactions
    Given an existing booklet with a regular transaction excluded for the current month
    When POST api/booklet/{id}/transactions/regenerate is called with the current month and year
    Then the HTTP response is 200 OK
    And the body contains the list of newly created previsional transactions
    And the type field in the response equals PREVISIONAL

Scenario: Regeneration for a future month returns 200 with virtual transactions
    Given an existing booklet with a regular transaction excluded for a future month
    When POST api/booklet/{id}/transactions/regenerate is called with a future month
    Then the HTTP response is 200 OK
    And the body contains the list of computed virtual transactions
    And the type field in the response equals VIRTUAL

Scenario: Regeneration for a past month returns 200 with an empty list
    Given an existing booklet with a regular transaction excluded for a past month
    When POST api/booklet/{id}/transactions/regenerate is called with a past month
    Then the HTTP response is 200 OK
    And the body contains an empty list
    And the type field in the response equals NONE

Scenario: Non-existent booklet returns 404
    Given a bookletID that does not exist in the database
    When POST api/booklet/{id}/transactions/regenerate is called
    Then the HTTP response is 404 Not Found

**Notes**
- Create a new DTO `RegenerateTransactionsResponse` containing `transactions: List<TransactionResult>` and `type: RegenerationType` (enum `PREVISIONAL` / `VIRTUAL` / `NONE`).
- `BookletController.regenerateDeletedPrevisionalTransactions` must map the domain result to the new DTO.
- Update tests in `BookletControllerTest.RegenerateDeletedPrevisionalTransactionsTest`.
