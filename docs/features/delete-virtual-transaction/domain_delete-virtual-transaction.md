# Delete Virtual Transaction : Domain Module Impact

**Context**
Virtual transactions are in-memory representations of future regular transaction occurrences.
They have no database `id` (`null`) but always carry a `regularTransactionId` referencing the regular
transaction they originate from, as well as a `date` indicating the month they belong to.

Today, when a user tries to delete such a transaction the system silently ignores it because the
`DeleteTransactionsByIdsService` requires a list of real UUIDs. The fix must introduce a dedicated
use case that marks the corresponding occurrence month as excluded in the `RegularTransactionTracker`,
preventing the virtual transaction from being re-generated for that month.

**Acceptance Criteria**

Feature: Exclude a virtual transaction occurrence for a given month

Scenario: Successfully exclude a virtual transaction for a future month
    Given an authenticated user
    And a booklet that contains a regular transaction
    And no physical transaction exists for that regular transaction in the target month
    When the use case is invoked with a valid bookletId, regularTransactionId, month and year
    Then the tracker for that (regularTransactionId, bookletId) pair is updated
    And the target month is added to the tracker's excludedMonths set
    And a success result is returned

Scenario: Successfully exclude a virtual transaction for the current month
    Given an authenticated user
    And a booklet that contains a regular transaction starting before or during the current month
    And no physical transaction exists for that regular transaction in the current month
    When the use case is invoked with the current month and year
    Then the tracker's excludedMonths contains the current YearMonth
    And a success result is returned

Scenario: Excluding a month that is already excluded is idempotent
    Given an authenticated user
    And the target month is already marked as excluded for that (regularTransactionId, bookletId) pair
    When the use case is invoked again with the same parameters
    Then the tracker still contains the target month in excludedMonths exactly once
    And a success result is returned

Scenario: Booklet not found returns BOOKLET_NOT_FOUND failure
    Given an authenticated user
    And the provided bookletId does not correspond to any existing booklet
    When the use case is invoked
    Then the result is a domain failure with state BOOKLET_NOT_FOUND

Scenario: Invalid session token returns UNAUTHENTICATED failure
    Given an invalid or expired session token
    When the use case is invoked
    Then the result is a domain failure with state UNAUTHENTICATED

**Notes**
- The new use case must be named `ExcludeVirtualTransactionUseCase` with its command
  `ExcludeVirtualTransactionCommand(token, bookletId, regularTransactionId, month, year)`.
- The service must only call `trackerRepository.markMonthAsExcluded`; no physical transaction
  deletion is performed (the virtual transaction has no id to delete).
- The exclusion must work for past, current, and future months without restriction — the
  "do not regenerate past months" guard lives in the regenerate use case, not here.
