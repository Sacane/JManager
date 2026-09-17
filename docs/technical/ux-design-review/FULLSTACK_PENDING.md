# Full-stack work pending — UI/UX workstream

Single register of everything in this workstream that **cannot be delivered frontend-only**. Each
entry still needs to be specified per layer (domain → infrastructure → application → client) before
it is picked up, following the TDD order in `CLAUDE.md`.

This file exists so that nothing here is lost between lots. **Nothing below is specified yet.**

Last updated: 18 September 2026.

---

## 1. Items already planned as full-stack

These were identified as full-stack in the original review and grouped into lot 6.

| ID | Subject | Why it needs the backend | Card |
|---|---|---|---|
| **UX-21** | "Mot de passe oublié" | No reset endpoint exists at all: no token issuance, no mail, no consumption. | https://trello.com/c/o5vUbR2j |
| **UX-22** | Textual search over transactions | Search must run where the rows are; the client only ever holds one page of one period. | https://trello.com/c/MnUxNqSz |
| **UX-23** | Move the budget target off `localStorage` | The target is per user, not per browser. Needs persistence and an endpoint. | https://trello.com/c/tZEm5mo6 |

---

## 2. Item reclassified during lot 4

| ID | Subject | Card |
|---|---|---|
| **UX-25** | Running balance per transaction, like a bank statement | https://trello.com/c/2hheQnJy |

**Was planned as:** P1, effort M, **Frontend**, lot 4.
**Actually is:** full-stack. Reclassified on 18 September 2026, before any code was written.

The issue stated the opening balance of the period was available from `/booklet/{id}/balances`. It
is not. `realSold` is `Amount(persisted.amount)` — the booklet's persisted total over every
confirmed transaction ever recorded, date-blind, unaffected by `month`, `year`, `startDate` or
`endDate`; only `previewSold` reacts to the range. `Booklet.addTransaction` maintains `amount`
without looking at dates.

A running balance needs the balance immediately before the first displayed row, which the client
cannot derive: it only ever loads one period, so a past month or any paginated view is out of
reach. Showing it anyway would place a wrong balance beside correct amounts.

**Two possible shapes, to arbitrate when specifying:**

1. `GET {bookletID}/transactions` returns a `balanceAfter` per transaction. The only option that
   holds under pagination and under any sort, and it puts the arithmetic where the rows are.
2. `GET {bookletID}/balances` gains an `openingBalance` for the requested range. Cheaper, but the
   client can then only accumulate correctly under an ascending date sort with the whole period
   loaded — scenario 3 of the issue stays conditional.

Full analysis: `docs/backlog/booklet-balances-endpoint-has-no-opening-balance.md`.
The issue file carries a correction block at its head.

---

## 3. Backend gaps found while delivering the frontend lots

Not UX items of their own, but each one limits a feature that has already shipped. They belong in
the same conversation as the items above.

| Gap | Consequence today | Recorded in |
|---|---|---|
| `/transactions/regenerable` and `/transactions/regenerate` accept `month` and `year` only, unlike `/transactions`, `/balances` and `/report` | Regeneration is hidden while a custom date range is active (UX-14), rather than silently operating on a different period | `docs/backlog/regenerable-transactions-date-range.md` |
| Recurrence date math now exists in TypeScript and in the Kotlin domain, with nothing keeping them in step | Nothing is wrong today; the risk is drift, and the symptom would be a wrong next-occurrence date beside a correct list (UX-19) | `docs/backlog/recurrence-math-duplicated-client-side.md` |
| No GDPR data export (portability) endpoint | "Mon compte" offers deletion but not export, while the privacy policy advertises both (UX-13) | this file — no backlog entry, nothing is broken, the feature is simply absent |

Note that fixing the recurrence duplication and delivering UX-25 option 1 overlap: both want
date-aware computation exposed from the domain. `docs/backlog/extract-recurrence-occurrence-calculator.md`
asks for the pure occurrence calculator that would serve them.

---

## 4. P2 items that are also full-stack

Listed for completeness; they were never scheduled and have no issue or card yet, per the backlog's
rule of formalising P2 items at their lot.

| ID | Subject |
|---|---|
| **UX-37** | Search and sort on the admin user list |
| **UX-38** | Real admin actions (role, deactivation, reset) — endpoints to create |
| **UX-42** | Usage count and cumulative amount per tag |

---

## What to do with this file

When one of these is specified, create its issue folder under `docs/features/` with one file per
layer, move its Trello card out of **Bloqué** or **Backlog**, and strike the row here rather than
deleting it, so the reclassification stays traceable. Delete the file only once every entry is
delivered.
