# Bug: Transaction Regeneration — Distinguish Previsional vs Virtual (Domain)

**Context**
The golden rule of the project states that a regular transaction can only generate **previsional transactions** (persisted, `isPreview = true`) when the targeted month is the **current month**. For any future month, transactions derived from regular transactions are **virtual transactions** (computed on-the-fly, not persisted).

Currently, `BookletFeature.regenerateDeletedPrevisionalTransactions` always calls `generateMissingPrevisionalTransactions`, which persists preview transactions regardless of the targeted month. This is a violation of the golden rule.

Additionally, the `hasRegenerableTransactions` flag in `BookletLoadingResult` is computed by checking whether `targetYearMonth` appears in any tracker's `excludedMonths`, without taking the current month into account. For past months, exposing this flag is pointless since neither previsional nor virtual transactions are shown.

**Acceptance Criteria**

Feature: Distinguish previsional regeneration from virtual regeneration

Scenario: Regeneration for the current month generates previsional transactions
    Given a booklet with a regular transaction whose current month is excluded in its tracker
    And the requested month is the current month and year
    When the user calls regenerateDeletedPrevisionalTransactions for that month
    Then the tracker is unmarked so the month is no longer excluded
    And previsional transactions (isPreview = true) are generated and persisted
    And those transactions are returned in the result

Scenario: Regeneration for a future month generates virtual transactions only
    Given a booklet with a regular transaction whose future month is excluded in its tracker
    And the requested month is a future month different from the current month
    When the user calls regenerateDeletedPrevisionalTransactions for that future month
    Then the tracker is unmarked so the month is no longer excluded
    And no previsional transaction is persisted
    And the corresponding virtual transactions are computed and returned

Scenario: Regeneration for a past month has no effect
    Given a booklet with a regular transaction whose past month is excluded in its tracker
    And the requested month is a past month prior to the current month
    When the user calls regenerateDeletedPrevisionalTransactions for that past month
    Then no transaction is generated or persisted
    And the returned result is an empty list
    And the tracker is not modified

Scenario: hasRegenerableTransactions is true for the current month with an exclusion
    Given a booklet with a tracker whose current month is excluded
    When loadTransactionsForBookletForAMonth is called for the current month
    Then hasRegenerableTransactions is true

Scenario: hasRegenerableTransactions is true for a future month with an exclusion
    Given a booklet with a tracker whose future month is excluded
    When loadTransactionsForBookletForAMonth is called for that future month
    Then hasRegenerableTransactions is true

Scenario: hasRegenerableTransactions is false for a past month
    Given a booklet with a tracker whose past month is excluded
    When loadTransactionsForBookletForAMonth is called for that past month
    Then hasRegenerableTransactions is false because past months cannot be regenerated

**Notes**
- The current/future/past distinction must be performed via `YearMonth.now()` inside `BookletFeatureImpl`.
- For future months: unmark the tracker exclusion and compute+return virtual transactions via `calculateVirtualTransactions`.
- For past months: do nothing, return an empty list without modifying the tracker.
- `hasRegenerableTransactions` in `BookletLoadingResult` must exclude past months from its computation.
- Existing tests in `BookletFeatureTest.RegenerateDeletedPrevisionalTransactionsTest` must be updated.
