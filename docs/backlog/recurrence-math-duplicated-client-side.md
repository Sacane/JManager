# Recurrence date math now exists on both sides of the API

**Observation**

`client/utils/recurrence.ts` (added for UX-19) computes occurrence dates from a
`RegularTransactionDTO`: its `startDate`, its `regularity` and its `frequencyProperty`. The same
rules already live in the Kotlin domain, where `RegularTransactionComputer.kt` materialises
provisional transactions.

Nothing keeps the two in step. The client rules were written from the DTO shape and from the
backend's observable behaviour, not from a shared contract. The day clamping is the most likely
place to diverge: a charge set on the 31st is clamped to the last day of a shorter month and
returns to the 31st afterwards, rather than drifting. If the backend ever drifts instead, the page
announces a next occurrence the backend will not produce.

**Exact location**

- `client/utils/recurrence.ts` — `occurrenceAt`, `clampedToMonth`, `candidateIndexFor`
- `domain/src/main/kotlin/fr/sacane/jmanager/domain/usecase/RegularTransactionComputer.kt` —
  `calculateActualTransactionDate`, `calculateNextOccurrence`
- consumed by `client/pages/regular-transaction/index.vue`

**Expected behaviour**

One of:

1. The backend exposes the next occurrence (and optionally the count of occurrences in a given
   month) on `RegularTransactionDTO`, and the client utility is deleted. This is the honest fix:
   the recurrence rules are domain knowledge.
2. The two implementations are pinned against each other by a shared fixture set — the same
   recurrences, the same expected dates, asserted on both sides.

Option 1 also settles `docs/backlog/extract-recurrence-occurrence-calculator.md`, which asks for a
pure occurrence calculator in the domain: once extracted, exposing it through the DTO is small.

**Impact**

Medium. Nothing is wrong today — the client tests pin the clamping behaviour described above, and
it matches what the backend does. The risk is drift over time, and the symptom would be a wrong
date shown next to a correct list of transactions, which is the kind of quiet inconsistency UX-03
was about.

**Why it was not fixed inline**

UX-19 is scoped frontend-only, and its issue states the derivation can be done without a backend
change. Adding a field to the DTO is a domain plus application change and belongs to its own task.
