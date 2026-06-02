# Bug Report — CSV import blocked when label is numeric

**Date**: 2026-06-02

## Symptom
Importing a CSV file where a transaction label is a purely numeric string (e.g. `"123"`, `"4501"`) was rejected with a `POSSIBLE_COLUMN_SWAP` error, preventing any import. The system incorrectly assumed the numeric label was a misplaced amount.

## Root Cause
`validateLabelColumn` in [`CsvFileValidator.kt`](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/usecase/csv/CsvFileValidator.kt) called `CsvValidationUtils.looksLikeAmount()` on the label value and returned a **blocking error** (`CsvValidationIssue`) when the check returned `true`. Because `looksLikeAmount` parses any string that is a valid `BigDecimal`, any integer or decimal label failed validation with `POSSIBLE_COLUMN_SWAP`, stopping the import.

## Fix
Extracted the numeric-label heuristic out of `validateLabelColumn` into a new private method `checkLabelAmountPattern`. This method returns a `CsvValidationIssue` that is now added to **warnings** (not errors) in `validateDataLine`. A numeric label still produces a `POSSIBLE_COLUMN_SWAP` warning to inform the user, but no longer blocks the import.

Changed files:
- [`domain/src/main/kotlin/…/usecase/csv/CsvFileValidator.kt`](../../../domain/src/main/kotlin/fr/sacane/jmanager/domain/usecase/csv/CsvFileValidator.kt)

## Non-Regression Tests
- `should accept a numeric label without blocking the import` — new test covering integer and decimal-looking labels
- `should warn but not block when label column contains a numeric value` — updated existing test to reflect warning (not error) behaviour

Location: [`domain/src/test/kotlin/…/usecase/csv/CsvFileValidatorTest.kt`](../../../domain/src/test/kotlin/fr/sacane/jmanager/domain/usecase/csv/CsvFileValidatorTest.kt)
