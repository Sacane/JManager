# Delete Virtual Transaction : Application Module Impact

**Context**
Virtual transactions are in-memory occurrences (no database id) generated from regular transactions.
The existing `DELETE /transaction` endpoint (`BookletTransactionsIdRequest`) only accepts real UUID lists
and therefore silently ignores virtual transactions.

Rather than introducing a second endpoint, the existing `DELETE /transaction` endpoint must be extended
to accept an optional list of virtual transaction descriptors alongside the existing `transactionIds`.
The controller dispatches `DeleteTransactionsByIdsCommand` for physical ids (unchanged) **and**
`ExcludeVirtualTransactionCommand` for each virtual descriptor, then merges the results into a
single response. This allows the client to delete a mixed selection in one HTTP call.

**Acceptance Criteria**

Feature: Unified DELETE /transaction endpoint handling both physical and virtual transactions

Scenario: Successfully delete a mixed selection of physical and virtual transactions
    Given an authenticated user
    And a request body containing physical transactionIds and virtual transaction descriptors
    When calling DELETE /transaction with the unified request body
    Then the physical transactions are deleted via DeleteTransactionsByIdsCommand
    And each virtual descriptor is excluded via ExcludeVirtualTransactionCommand
    And the API returns 200 OK with the merged deletion result

Scenario: Successfully delete only virtual transactions
    Given an authenticated user
    And a request body where transactionIds is empty and virtualTransactions is non-empty
    When calling DELETE /transaction
    Then only ExcludeVirtualTransactionCommand is dispatched
    And the API returns 200 OK

Scenario: Successfully delete only physical transactions (backward compatibility)
    Given an authenticated user
    And a request body with transactionIds and an absent or empty virtualTransactions list
    When calling DELETE /transaction
    Then only DeleteTransactionsByIdsCommand is dispatched
    And the API returns 200 OK — existing behaviour is fully preserved

Scenario: Request with both lists empty returns 400 Bad Request
    Given an authenticated user
    And a request body where both transactionIds and virtualTransactions are empty
    When calling DELETE /transaction
    Then the API returns 400 Bad Request

Scenario: Booklet not found returns 404 Not Found
    Given an authenticated user
    And a bookletId that does not exist
    When calling DELETE /transaction with any non-empty list
    Then the API returns 404 Not Found

Scenario: Unauthenticated request returns 401 Unauthorized
    Given a request without a valid session token
    When calling DELETE /transaction
    Then the API returns 401 Unauthorized

**Notes**
- Extend `BookletTransactionsIdRequest` with an optional field:
  `virtualTransactions: List<VirtualTransactionDescriptor> = emptyList()`
  where `VirtualTransactionDescriptor` has fields `regularTransactionId: String`, `month: Month`, `year: Int`.
- The `transactionIds` field becomes nullable/optional (defaults to empty list) so that a
  virtual-only request is valid.
- The response DTO `TransactionDeletionResponse` can be reused or extended with an
  `excludedVirtualTransactions: List<String>` field (YearMonth strings) for observability.
- Both commands are dispatched sequentially inside the same controller method; if physical deletion
  fails the endpoint returns the failure immediately without dispatching virtual exclusions.
- Wiring must use the existing `CommandBus` pattern.
