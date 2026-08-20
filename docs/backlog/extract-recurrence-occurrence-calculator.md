# Extract a pure occurrence calculator out of `RegularTransactionGeneratorService`

**Observation**

The date math that turns a `RegularTransaction` (its `RecurrenceRule` + `FrequencyProperty`) into a
list of occurrence dates is now written three times, with the same `when (frequency)` dispatch and
near-identical loops:

- `generateTransactionsInLoop` (persisting path)
- `generateVirtualTransactionsBetween` (virtual path)
- `calculateOccurrencesIgnoringExclusions` (candidate-listing path, added for the selective
  regeneration feature)

Each of the three re-implements or re-dispatches the `Forever` / `UntilDate` /
`SpecificRepetitionTimes` branching, and `createPrevisionalTransaction` /
`calculateActualTransactionDate` duplicate the day-adjustment logic between themselves.

**Exact location**

`domain/src/main/kotlin/fr/sacane/jmanager/domain/usecase/RegularTransactionComputer.kt`
- interface `RegularTransactionGenerator` — now 4 methods, all variations on "compute occurrences"
- `generateMissingPrevisionalTransactions`, `calculateVirtualTransactions`,
  `calculateOccurrencesIgnoringExclusions`, `regenerateMissingPrevisionalTransactions`
- private helpers `generateTransactionsBetween`, `generateTransactionsInLoop`,
  `generateVirtualTransactionsBetween`, `calculateActualTransactionDate`,
  `createPrevisionalTransaction`, `calculateNextOccurrence`

**Expected behaviour**

Extract a dedicated, pure domain service — no repository, no tracker, no persistence — answering a
single question: *given a regular transaction and a date range, which dates does it occur on?*
A Kotlin-idiomatic shape would be a lazily evaluated `Sequence<LocalDate>`:

```kotlin
fun RegularTransaction.occurrencesBetween(start: LocalDate, end: LocalDate): Sequence<LocalDate>
```

The three existing callers then become thin: filter by exclusion, filter by existing physical
transactions, persist — each keeping only its own concern. `RegularTransactionGenerator` shrinks
back to a focused port.

**Impact**

- SRP: `RegularTransactionGeneratorService` currently mixes occurrence computation, exclusion
  filtering, duplicate detection and persistence.
- OCP: adding a new `RecurrenceRule` or `FrequencyProperty` variant today means editing every
  `when` block instead of one.
- Risk of divergence: a fix applied to one loop and not the others produces inconsistent
  forecasts between the booklet view, the balance computation and the regeneration dialog.

Medium effort, well covered by the existing domain test suite — low regression risk, but large
enough that it was deliberately kept out of the selective-regeneration change.
