# REFACTORING_PLAN — Domain Port Tests Migration (Phase 1)

**Generated on**: 2026-05-11  
**Applied pattern**: Test Infrastructure Adoption (Fixtures + TestScenario DSL + ScenarioBlocks)  
**Stack**: Kotlin + JUnit 5 (no Spring in domain)  
**Overall status**: 🔄 In progress — Step 9 / 12 completed

---

## Initial Analysis

### What was detected

- **10 port test files** (243 test methods, ~5590 lines) all use inline object construction: `User(...)`, `Booklet(...)`, `Transaction(...)` built manually in every test instead of using the available fixture builders (`UserFixture`, `BookletFixture`, `TransactionFixture`, etc.)
- **196 usages of `launchWithUserId` / `launchWithNewUserId`** across 8 files. This pattern wraps the entire test body in a receiver lambda, hiding the "arrange" phase inside the same block as "act" and "assert". TestScenario DSL provides a cleaner separation.
- **`given/act/then` blocks exist but are never used**: no test adopts the ScenarioBlocks structure.
- **`TestScenario` DSL exists but is never instantiated**: the fluent builder was created but no test file imports or uses it.
- **`initWith` extension exists but is never used**: tests still call `state.init(listOf(...))` instead of `state.initWith(...)`.
- **`generateTransaction()` helper in FeatureTest** is used 113 times across 3 files. It couples tests to FeatureTest's tag repository for the default tag; `TransactionFixture.aTransaction()` provides the same capability without coupling.
- **`BookletFeatureTest` (1993 lines, 53 tests)** is by far the largest file and mixes two distinct patterns: direct state init with a registered user, and `launchWithUserId` with an ephemeral user. It will require special handling.
- **`AdminFeatureTest` (180 lines, 17 tests)** and **`SessionManagerTest` (75 lines, 4 tests)** do not use `launchWithUserId` at all — they set up state directly. Only fixtures and `given/act/then` apply.

### What will NOT be modified

- The **business logic** verified by each test (assertions, expected values, edge cases)
- The **domain model** (entities, value objects, ports, use-cases)
- The **fake infrastructure** (`FakeFactory`, `InMemoryDatabase`, `InMemory*Repository`)
- The **test fixture files** (already complete)
- The **TestScenario DSL** (already complete — may receive minor additions if a gap is found)
- The **ScenarioBlocks** (`given/act/then`) and **StateExtensions** (`initWith`)
- **Phase 2 tests** (usecase calculator tests in `domain/usecase/`) are out of scope

### Justification of the chosen pattern

The test infrastructure refactoring (steps A–F) established fixture builders, a composable TestScenario DSL, and ScenarioBlocks — but none of them are actually used in the existing tests. This migration adopts these tools systematically across the 10 port test files. The pattern is "Test Infrastructure Adoption": replace inline construction with fixtures, replace `launchWithUserId` scaffolding with the fluent TestScenario DSL, and structure test bodies with `given/act/then` for immediate readability. Each test's **behaviour and assertions remain identical** — only the setup and structure change.

---

## Refactoring Steps

---

### Step 0 — Green baseline

**Status**: ✅ Done  
**Objective**: Confirm all domain tests pass before any modification  
**Blocking for**: All subsequent steps

**Actions:**
1. Run `./gradlew :domain:test`
2. Confirm 0 failures

**Validation criterion:**
> `BUILD SUCCESSFUL` with 0 test failures.

---

### Step 1 — ExcludeVirtualTransactionFeatureTest (pilot)

**Status**: ✅ Done  
**Depends on**: Step 0  
**Objective**: Validate the migration approach on the smallest file (109 lines, 4 tests)

**Actions:**
1. Replace `FeatureTest()` inheritance with standalone `FakeFactory` + `TestScenario`
2. Replace `launchWithUserId { ... }` blocks with `scenario.withUser().withBooklet()` setup
3. Structure each test body with `given { } / act { } / then { }`
4. Replace inline `RegularTransaction(...)` construction with `RegularTransactionFixture`
5. Clean up imports
6. Run `./gradlew :domain:test` to confirm green

**Validation criterion:**
> The file no longer extends `FeatureTest`. All 4 tests pass. Each test uses `given/act/then` and fixtures.

---

### Step 2 — SessionManagerTest

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Migrate the standalone test (75 lines, 4 tests) — already doesn't extend FeatureTest

**Actions:**
1. Replace inline `User(...)` / `UserWithPassword(...)` with `UserFixture`
2. Structure test bodies with `given/act/then`
3. Use `initWith` for state initialization
4. Clean up imports
5. Run `./gradlew :domain:test`

**Validation criterion:**
> All 4 tests pass. Inline `User(...)` / `UserWithPassword(...)` replaced with fixture calls.

---

### Step 3 — AdminFeatureTest

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Migrate the admin test (180 lines, 17 tests) — uses direct state init, no `launchWithUserId`

**Actions:**
1. Replace `FeatureTest()` inheritance with standalone `FakeFactory`
2. Replace inline `User(...)` / `UserWithPassword(...)` with `UserFixture`
3. Replace the `createAdmin()` helper with a fixture-based version
4. Structure test bodies with `given/act/then`
5. Use `initWith` for state initialization
6. Clean up imports
7. Run `./gradlew :domain:test`

**Validation criterion:**
> The file no longer extends `FeatureTest`. All 17 tests pass. No inline `User(...)` construction remains.

---

### Step 4 — UserFeatureTest

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Migrate the user test (275 lines, 18 tests) — 3 `launchWithUserId` + many direct init

**Actions:**
1. Replace `FeatureTest()` inheritance with standalone `FakeFactory` + `TestScenario`
2. Replace 3 `launchWithUserId` blocks with TestScenario DSL
3. Replace inline `User(...)` / `UserWithPassword(...)` with `UserFixture`
4. Structure test bodies with `given/act/then`
5. Clean up imports
6. Run `./gradlew :domain:test`

**Validation criterion:**
> The file no longer extends `FeatureTest`. All 18 tests pass. No `launchWithUserId` remains.

---

### Step 5 — TagFeatureTest

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Migrate the tag test (370 lines, 27 tests) — 25 `launchWithUserId`, 4 `generateTransaction`

**Actions:**
1. Replace `FeatureTest()` inheritance with standalone `FakeFactory` + `TestScenario`
2. Replace 25 `launchWithUserId` blocks with TestScenario DSL
3. Replace 4 `generateTransaction()` calls with `TransactionFixture.aTransaction()`
4. Replace inline `Tag.Personal(...)` with `TagFixture.aPersonalTag()`
5. Structure test bodies with `given/act/then`
6. Clean up imports
7. Run `./gradlew :domain:test`

**Validation criterion:**
> The file no longer extends `FeatureTest`. All 27 tests pass. No `launchWithUserId` or `generateTransaction` remains.

---

### Step 6 — TransactionFeatureTest

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Migrate the transaction test (555 lines, 25 tests) — 25 `launchWithUserId`, 55 `generateTransaction`

**Actions:**
1. Replace `FeatureTest()` inheritance with standalone `FakeFactory` + `TestScenario`
2. Replace 25 `launchWithUserId` blocks with TestScenario DSL
3. Replace 55 `generateTransaction()` calls with `TransactionFixture.aTransaction()`
4. Replace inline `RegularTransaction(...)` with `RegularTransactionFixture`
5. Structure test bodies with `given/act/then`
6. Clean up imports
7. Run `./gradlew :domain:test`

**Validation criterion:**
> The file no longer extends `FeatureTest`. All 25 tests pass. No `launchWithUserId` or `generateTransaction` remains.

---

### Step 7 — FileImportExportFeatureTest

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Migrate the file import/export test (730 lines, 32 tests) — 30 `launchWithUserId`

**Actions:**
1. Replace `FeatureTest()` inheritance with standalone `FakeFactory` + `TestScenario`
2. Replace 30 `launchWithUserId` blocks with TestScenario DSL
3. Replace inline object construction with fixtures where applicable
4. Structure test bodies with `given/act/then`
5. Clean up imports
6. Run `./gradlew :domain:test`

**Validation criterion:**
> The file no longer extends `FeatureTest`. All 32 tests pass. No `launchWithUserId` remains.

---

### Step 8 — RegularTransactionFeatureTest

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Migrate the regular transaction test (605 lines, 27 tests) — 26 `launchWithUserId`

**Actions:**
1. Replace `FeatureTest()` inheritance with standalone `FakeFactory` + `TestScenario`
2. Replace 26 `launchWithUserId` blocks with TestScenario DSL
3. Replace inline `RegularTransaction(...)` with `RegularTransactionFixture`
4. Structure test bodies with `given/act/then`
5. Clean up imports
6. Run `./gradlew :domain:test`

**Validation criterion:**
> The file no longer extends `FeatureTest`. All 27 tests pass. No `launchWithUserId` remains.

---

### Step 9 — StatsFeatureTest

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Migrate the stats test (698 lines, 36 tests) — 36 `launchWithUserId`, 54 `generateTransaction`

**Actions:**
1. Replace `FeatureTest()` inheritance with standalone `FakeFactory` + `TestScenario`
2. Replace 36 `launchWithUserId` blocks with TestScenario DSL
3. Replace 54 `generateTransaction()` calls with `TransactionFixture.aTransaction()`
4. Structure test bodies with `given/act/then`
5. Clean up imports
6. Run `./gradlew :domain:test`

**Validation criterion:**
> The file no longer extends `FeatureTest`. All 36 tests pass. No `launchWithUserId` or `generateTransaction` remains.

---

### Step 10 — BookletFeatureTest (part 1: non-nested tests)

**Status**: ✅ Done  
**Depends on**: Step 1  
**Objective**: Migrate the top-level tests of BookletFeatureTest (the tests outside `@Nested` classes)

**Actions:**
1. Replace `FeatureTest()` inheritance with standalone `FakeFactory` + `TestScenario`
2. Migrate the registered `user` pattern to use `UserFixture` + direct repo init
3. Replace top-level tests' `bookletState.init(...)` with TestScenario DSL where applicable
4. Replace inline `Booklet(...)` with `BookletFixture.aBooklet()`
5. Structure test bodies with `given/act/then`
6. Clean up imports
7. Run `./gradlew :domain:test`

**Validation criterion:**
> Top-level tests pass. No inline `Booklet(...)` or `User(...)` construction in migrated tests.

---

### Step 11 — BookletFeatureTest (part 2: nested classes)

**Status**: ✅ Done  
**Depends on**: Step 10  
**Objective**: Migrate all `@Nested` inner class tests (47 `launchWithUserId` blocks)

**Actions:**
1. Replace 47 `launchWithUserId` blocks with TestScenario DSL
2. Replace inline object construction with fixtures
3. Structure test bodies with `given/act/then`
4. Clean up imports
5. Run `./gradlew :domain:test`

**Validation criterion:**
> All 53 tests pass. The file no longer extends `FeatureTest`. No `launchWithUserId` remains.

---

### Step 12 — Final cleanup & validation

**Status**: ✅ Done  
**Depends on**: Steps 1–11  
**Objective**: Verify full migration, clean up FeatureTest, full green suite

**Actions:**
1. Remove `launchWithUserId()`, `launchWithNewUserId()`, `generateTransaction()`, `createBooklet()` from `FeatureTest` (no longer used by port tests)
2. Remove `UserIdOnly`, `UserIdBooklet` inner classes from `FeatureTest`
3. If `FeatureTest` is still used by Phase 2 tests (`RegularTransactionComputerTest`), keep it as a minimal base or mark helpers as `@Deprecated`
4. Remove unused imports from `FeatureTest`
5. Run `./gradlew :domain:test` — full green
6. Verify no `launchWithUserId`, `launchWithNewUserId`, or `generateTransaction` references remain in `domain/port/` tests

**Validation criterion:**
> `BUILD SUCCESSFUL` with 0 failures. No port test file extends `FeatureTest` except indirectly via Phase 2 files. All port tests use `TestScenario` + fixtures + `given/act/then`.

---

## Recommended Execution Order

```
Step 0   (baseline)
   │
   ▼
Step 1   (pilot — ExcludeVirtualTransactionFeatureTest)
   │
   ├──► Step 2  (SessionManagerTest)         ─┐
   ├──► Step 3  (AdminFeatureTest)            │ non-blocking
   ├──► Step 4  (UserFeatureTest)             │ between each other
   ├──► Step 5  (TagFeatureTest)              │
   ├──► Step 6  (TransactionFeatureTest)      │
   ├──► Step 7  (FileImportExportFeatureTest) │
   ├──► Step 8  (RegularTransactionFT)        │
   ├──► Step 9  (StatsFeatureTest)           ─┘
   │
   ▼
Step 10  (BookletFeatureTest part 1) ──► Step 11 (part 2)
   │
   ▼
Step 12  (final cleanup)
```

Steps 2–9 are independent of each other (no shared state). BookletFeatureTest is split into 2 sequential sub-steps due to its size (1993 lines, 53 tests).

---

## Risks and Mitigations

| Risk | Severity | Mitigation |
|------|----------|------------|
| `generateTransaction()` assigns default tag from repo; `TransactionFixture` creates inline tag | Low | `Tag.Default` returns `isDefault = true` regardless; tests checking tag identity will be verified |
| `launchWithUserId` password is `"test"`; `UserFixture` default is `"test-password"` | Medium | Override password explicitly in tests that depend on it (login, session tests) |
| BookletFeatureTest mixes registered user + ephemeral user patterns | Medium | Handle each pattern separately (Steps 10/11); registered user → `UserFixture` + repo init |
| FeatureTest removal may break Phase 2 tests | Low | Keep FeatureTest as deprecated until Phase 2 completes |
