# Full-stack work pending — UI/UX workstream

Single register of everything in this workstream that **cannot be delivered frontend-only**. Each
entry still needs to be specified per layer (domain → infrastructure → application → client) before
it is picked up, following the TDD order in `CLAUDE.md`.

This file exists so that nothing here is lost between lots. **Specified so far: UX-21 and UX-27** (21 September 2026); everything else below is not.

Last updated: 21 September 2026.

---

## 1. Items already planned as full-stack

These were identified as full-stack in the original review and grouped into lot 6.

| ID | Subject | Why it needs the backend | Card |
|---|---|---|---|
| **UX-21** | "Mot de passe oublié" — ✅ **specified 21/09/2026** | No reset endpoint exists at all: no token issuance, no mail, no consumption. Specifying it also showed that a password change never ends older sessions; the fix is part of UX-21. | https://trello.com/c/o5vUbR2j |
| **UX-22** | Textual search over transactions | Search must run where the rows are; the client only ever holds one page of one period. | https://trello.com/c/MnUxNqSz |
| **UX-23** | Move the budget target off `localStorage` | The target is per user, not per browser. Needs persistence and an endpoint. | https://trello.com/c/tZEm5mo6 |


### Prerequisite found while specifying UX-21

`docs/bugs/spa-pages-401-on-direct-load/` — in production, every page missing from `SpaController`
answers **401** when opened from a link, **the email verification page included**. The reset link would
fail the same way, so this bug is fixed before UX-21's client part ships. It is a live defect on its
own: email verification cannot be completed from the email today.

---

## 1 bis. Item reclassified while specifying UX-21

| ID | Subject | Card |
|---|---|---|
| **UX-27** | Password rules and strength indicator — ✅ **specified 21/09/2026** | https://trello.com/c/BEtdi2SP |

**Was planned as:** P1, effort M, **Frontend**, attached to lot 6 alongside UX-21.
**Actually is:** full-stack. There is no password policy on the backend: every DTO accepting a password
declares `@Size(min = 1, max = 100)` and no domain service checks length. Rules shown by the client
would decorate an API that accepts a one-character password. Product decision of 21 September 2026:
one domain policy for every way of setting a password. It ships with UX-21, whose reset form uses it.

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
| Registration collapses "username taken" and "email taken" into one generic `domain.user.register.invalid` ("Une erreur est survenue") — `RegisterUserService` returns it whenever `userRepository.register` yields null | The user is told registration failed without learning what to change (UX-20) | this file — see the note below |

**Note on the registration failure.** Distinguishing the two cases is a product decision, not only a
backend change. Telling a visitor that an **email** is already registered is a user enumeration
vector; many products accept that at sign-up, others answer the same way in both cases and send a
mail to the address instead. The **username** case carries no such risk and can safely be named.
Decide before specifying.

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
