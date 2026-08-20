# `BookletBookingRequest.amount` uses `Double` for a monetary value

## Observation

The booklet creation request DTO carries the initial amount as a `Double`, which
contradicts the Kotlin coding guidelines ("No `Double`/`Float` for monetary values —
use `BigDecimal`"). The value is converted through `Double.toAmount()`, which builds
`BigDecimal(this)` from the binary double — the constructor that carries the full
binary expansion, not the decimal representation.

## Exact location

- `application/src/main/kotlin/fr/sacane/jmanager/application/api/booklet/DTO.kt` — `BookletBookingRequest.amount: Double`
- `domain/src/main/kotlin/fr/sacane/jmanager/domain/models/Amount.kt` — `fun Double.toAmount(...)` uses `BigDecimal(this)`

## Expected behaviour

The amount should travel as `BigDecimal` (as `BookletDTO.amount` already does, via
`BigDecimalSerializer`), so no binary rounding can occur between the client payload and
the persisted `Amount`.

## Impact

Currently masked because `Amount`'s `init` block re-scales to 2 decimals with
`HALF_UP`, and because the client serialises monetary fields as `toFixed(2)` strings.
Both are compensating behaviours, not a fix — any future path that skips them can
persist a value off by a cent.

## Spotted during

Removal of the `@Positive` constraint on booklet creation (allowing zero/negative
starting amounts).
