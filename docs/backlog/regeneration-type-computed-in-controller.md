# `RegenerationType` is re-derived in the controller instead of coming from the domain

**Observation**

`BookletController.regenerateDeletedPrevisionalTransactions` classifies the target month as
past / current / future to build the `RegenerationType` returned to the client. The domain command it
dispatches makes exactly the same classification, from its own `YearMonth.now()` read, to decide
whether to persist previsional transactions, compute virtual ones, or do nothing.

The business rule ("current month → persisted, future month → virtual, past month → nothing") is
therefore written twice, once of them in the application layer — which
`docs/agents/instructions/backend.instructions.md §1.1` states must contain no business logic.

**Exact location**

- `application/src/main/kotlin/fr/sacane/jmanager/application/api/booklet/Controller.kt` —
  `regenerateDeletedPrevisionalTransactions`, the `val type = when { ... }` block
- `domain/src/main/kotlin/fr/sacane/jmanager/domain/port/input/booklet/RegenerateDeletedPrevisionalTransactionsUseCase.kt` —
  the `targetYearMonth.isBefore(currentYearMonth)` / `targetYearMonth == currentYearMonth` branches

**Expected behaviour**

The domain command should return the classification together with the transactions — e.g. a
`RegenerationResult(transactions, kind)` where `kind` is a domain enum — and the controller should
map that enum to its DTO instead of recomputing it. The rule then lives in exactly one place.

**Impact**

- The two `YearMonth.now()` reads are independent. A request served across a month boundary can be
  classified `PREVISIONAL` by the controller while the domain took the virtual branch (or the
  reverse), and the client would then apply the wrong toast and the wrong refresh behaviour.
- Duplicated business rule: a future change to the golden rule must be applied in two layers, and
  nothing fails if only one is updated.

Related to [`booklet-domain-uses-system-clock.md`](booklet-domain-uses-system-clock.md) — moving the
whole cluster to an injected `Clock` and returning the classification from the domain are best done
in the same pass. Noted while implementing the selective regeneration endpoints; deliberately left
out because it changes the domain command's return type, which was already closed and reviewed.
