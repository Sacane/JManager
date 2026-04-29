# Delete Virtual Transaction : Client Module Impact

**Context**
Virtual transactions are displayed in the booklet detail page (`pages/booklet/[id].vue`) with
`id === null`. They carry a `regularTransactionId` (non-null) and a `date`.

Currently `confirmDelete()` silently skips virtual transactions and shows a warning toast when
the selection contains only virtual transactions. The fix must:

1. Split selected transactions into **physical** (id non-null) and **virtual** (id null,
   regularTransactionId non-null).
2. Build a single unified payload for the existing `DELETE /transaction` endpoint, containing
   the physical ids **and** the virtual descriptors `{ regularTransactionId, month, year }`.
3. Send one HTTP call regardless of whether the selection is physical-only, virtual-only, or mixed.
4. Remove all successfully processed transactions from the local list.

The `deleteTransaction` composable method in `useTransaction.ts` must be updated to accept the
extended payload instead of only `ids: string[]`.

**Acceptance Criteria**

Feature: Delete virtual transactions from the booklet page

Scenario: User deletes a selection containing only virtual transactions
    Given the user is on the booklet detail page for a given month
    And the selection contains one or more virtual transactions (id null, regularTransactionId set)
    When the user confirms deletion
    Then a single DELETE /transaction call is made with an empty transactionIds list and the virtual descriptors
    And the virtual transactions are removed from the displayed list
    And a success toast is shown

Scenario: User deletes a mixed selection of physical and virtual transactions
    Given the user is on the booklet detail page for a given month
    And the selection contains both physical and virtual transactions
    When the user confirms deletion
    Then a single DELETE /transaction call is made carrying both physical ids and virtual descriptors
    And all selected transactions are removed from the displayed list
    And a single success toast is shown

Scenario: Virtual transaction without regularTransactionId is skipped with a warning
    Given the user is on the booklet detail page
    And the selection contains a virtual transaction where regularTransactionId is null or undefined
    When the user confirms deletion
    Then that virtual transaction is excluded from the request payload
    And a warning toast informs the user that some transactions could not be excluded

Scenario: Confirmation dialog still shown before deletion
    Given the user selects one or more transactions including virtual ones
    When the user clicks the delete button
    Then a confirmation dialog is displayed listing the count of selected transactions
    And deletion only proceeds after the user confirms

**Notes**
- Update `deleteTransaction` signature in `useTransaction.ts`:
  ```ts
  function deleteTransaction(
    bookletId: string,
    ids: string[],
    virtualTransactions?: VirtualTransactionDescriptor[]
  ): Promise<TransactionDeletionDTO>
  ```
  where `VirtualTransactionDescriptor = { regularTransactionId: string; month: number; year: number }`.
- Add `VirtualTransactionDescriptor` to `client/types/index.d.ts`.
- The month passed to the descriptor must be derived from `transaction.date` (1-based month number
  obtained via `new Date(transaction.date).getMonth() + 1`).
- The `LOADING_SCOPES.bookletDetails.deleteTransaction` scope is reused; no new loading scope needed.
- Unit tests must cover all new branches in `confirmDelete()` and the updated composable.
