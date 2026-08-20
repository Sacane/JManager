# Booklet domain services read the system clock directly instead of an injected `Clock`

**Observation**

`CLAUDE.md` and `docs/agents/instructions/kotlin-coding-guidelines.md` both state: *no
`LocalDate.now()` / `UUID.randomUUID()` in domain — inject `Clock` / `IdGenerator`*. The booklet
cluster violates this consistently: "is the target month in the past, current, or future" — the rule
that drives whether transactions are persisted, computed virtually, or not produced at all — is
resolved against the real system clock.

`Clock` is already injectable in this domain (`VerifyEmailUseCase`, `EmailVerificationIssuer` both
take one) and `FakeFactory` already exposes a `fixedClock`, so the seam exists.

**Exact location**

- `domain/.../port/input/booklet/LoadBalancesForBookletForAMonthUseCase.kt:64` — `LocalDate.now()`
- `domain/.../port/input/booklet/LoadTransactionsForBookletForAMonthUseCase.kt:86` — `LocalDate.now()`
- `domain/.../port/input/booklet/LoadTransactionsForBookletForAMonthUseCase.kt:251` — `YearMonth.now()`
- `domain/.../port/input/booklet/RegenerateDeletedPrevisionalTransactionsUseCase.kt:76` — `YearMonth.now()`
- `domain/.../port/input/booklet/FindRegenerableTransactionsUseCase.kt:63` — `YearMonth.now()`
- `domain/.../usecase/TrendCalculator.kt:81` — `LocalDate.now()`

**Expected behaviour**

Inject `java.time.Clock` into these services and derive `YearMonth.now(clock)` /
`LocalDate.now(clock)` from it. Convert the whole cluster in one pass so the services cannot disagree
about what "now" is — `FindRegenerableTransactions` and `RegenerateDeletedPrevisionalTransactions`
in particular must classify the same month identically, otherwise the dialog can list a candidate
that the regeneration call then refuses.

**Impact**

- Tests are non-deterministic around month/year boundaries: several booklet tests build their
  fixtures from `LocalDate.now()` and would behave differently if run at the exact moment a month
  rolls over.
- The past/current/future classification cannot be exercised deliberately — only whichever branch
  the calendar happens to select on the day the suite runs.
- Direct violation of a MAJOR project rule.

Noted while implementing selective regeneration: the new query deliberately followed the existing
`YearMonth.now()` convention rather than introducing a lone `Clock` seam that would have let the
query and its paired command disagree on the current month.
