# The balances endpoint exposes no opening balance for a period

**Observation**

UX-25 (running balance per transaction, "like a bank statement") is specified as frontend-only, on
the stated grounds that *"the opening balance of the period is already available through the balances
endpoint"*. It is not. Reading the domain shows `/booklet/{id}/balances` returns:

- `realSold = Amount(persisted.amount)` — the booklet's persisted amount, computed from **every**
  confirmed transaction ever recorded, with no date bound. It does not vary with `month`, `year`,
  `startDate` or `endDate`; those parameters only affect `previewSold`.
- `previewSold` — the persisted amount plus the provisional transactions of the requested range.

`Booklet.addTransaction` keeps `amount` as a running total over all non-preview transactions and
never looks at their dates, which is why no date-scoped balance exists anywhere in the response.

**Exact location**

- `domain/.../port/input/booklet/LoadBalancesForBookletForAMonthUseCase.kt` — `realSold` is set from
  `persisted.amount`
- `domain/.../models/Booklet.kt:109` — `addTransaction` maintains `amount` date-blind
- `application/.../api/booklet/Controller.kt:126` — `GET {bookletID}/balances`
- consumer that would need it: `client/pages/booklet/[id].vue`

**Why this blocks the feature as written**

A running balance needs the balance immediately before the first displayed transaction. Deriving it
client side would require every confirmed transaction preceding the period, and the client only ever
loads one period. Working backwards from `realSold` is only possible when the displayed period ends
today **and** is fully loaded — so a past month, or any paginated view, could not be computed at
all. Shipping the column anyway would put a wrong balance next to correct amounts, which is the
defect class UX-03 was about.

**Expected behaviour**

Either:

1. `GET {bookletID}/transactions` returns a `balanceAfter` per transaction, computed server side.
   This is the only option that works under pagination and under any sort, and it puts the balance
   arithmetic where the transactions are.
2. `GET {bookletID}/balances` gains an `openingBalance` for the requested range — the sum of
   confirmed transactions strictly before `startDate` (or before the first day of the month). The
   client then accumulates over the rows it has, which stays correct only under an ascending date
   sort with the whole period loaded.

Option 1 is the better one and makes scenarios 2 and 3 of the issue achievable rather than
conditional.

**Impact**

UX-25 cannot be delivered frontend-only. It is a full-stack item: domain (a date-bounded balance
query), application (DTO plus endpoint), then client. Its effort estimate of **M / Frontend** in
`UX_BACKLOG.md` is wrong and its lot assignment should move — lot 6 already groups the full-stack
work (UX-21, UX-22, UX-23).

Nothing is currently broken; this records why the item was not implemented as written.
