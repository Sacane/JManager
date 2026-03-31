# Changelog

## 2026-03-31

- Fixed day-anchor drift in monthly recurring transactions: `calculateNextOccurrence` now reapplies the configured `dayOfMonth` with month-end clamping after `plusMonths(1)`, so a rule anchored on day 29 or 31 no longer shifts to an earlier day after crossing a short month (e.g. Monthly(29) Jan→Feb(28)→Mar was 28/03, now correctly 29/03).
- Fixed physical previsional generation for the current period when a custom date range (startDate/endDate) is provided: `loadTransactionsForBookletForAMonth` now generates physical previsional transactions for every calendar month covered by the custom range when today falls inside the range, ensuring the correct occurrence date is used and not a stale calendar-month occurrence persisted from a prior standard-range visit.
- Added domain regression tests: day-anchor preservation for Monthly(29) across a non-leap February, Monthly(31) across April, and custom-range virtual transaction dates for the "Aqua" scenario (Monthly(28) starting 2026-02-28 appearing as 28/02 in the March custom view and 28/03 in the April custom view).

## 2026-03-30

- Upgraded frontend dependency `happy-dom` from `20.8.4` to `20.8.9` to fix known security vulnerabilities (fetch credential origin handling and ECMAScript module export-name code injection).
- Added per-account optional monthly period end day configuration (`monthlyPeriodEndDay`) across domain models, settings contracts, infrastructure API DTOs, and persistence adapters.
- Added database migration `V14__add_account_monthly_period_end_day.sql` to store `monthly_period_end_day` with nullable semantics and strict day-range validation (`1..31` when provided).
- Extended user settings update validation to reject invalid end-day values with a dedicated semantic error key (`domain.user.settings.invalid_monthly_period_end_day`).
- Updated settings API read/write flows so account cycles now persist and return both `monthlyPeriodStartDay` and optional `monthlyPeriodEndDay`.
- Updated frontend settings page to configure both monthly start and optional next-month end day, including an explicit default mode that preserves legacy behavior.
- Extended shared frontend monthly range utility to apply explicit end-day boundaries when configured while keeping default fallback behavior (`next-month start day - 1`).
- Updated dashboard and account details pages to consume the optional end-day configuration for period range computation and account-scoped data queries.
- Added and updated regression tests across layers (domain, infra API/repository, and frontend pages) to cover explicit end-day behavior, fallback behavior, validation failures, and persistence/readback.

## 2026-03-29

- Fixed account month targeting regression where default cycle settings could resolve to the previous month range, causing virtual transactions from M-1 to appear when viewing month M.
- Added a shared frontend utility for cycle range computation and ISO date serialization (`client/utils/monthlyCycleRange.ts`) and reused it from dashboard and account pages to keep a single source of truth.
- Updated dashboard monthly period calls and assertions to use the new cycle-window semantics, including explicit regression coverage for “end bound = next-month cycle day minus one”.
- Added dedicated frontend unit regression coverage for monthly range resolution to assert both rules: default cycle keeps calendar month boundaries, and cycle 28->27 resolves month M as 28/M-1 to 27/M.
- Updated account details loading flow to retrieve the configured account cycle from user settings and query balances/transactions with explicit `startDate`/`endDate` date ranges.
- Extended frontend account API composable contracts to support optional date-range parameters on report/balances/transactions queries while preserving existing month/year compatibility.
- Extended backend account and transaction HTTP endpoints to accept optional `startDate`/`endDate` query parameters with strict validation (`both-or-none`, `start <= end`) and forward them to domain services.
- Extended `BookletFeature` domain contracts to support explicit date ranges for transactions and balances loading, including day-level filtering and previsional balance computations bounded to the effective range.
- Preserved legacy month/year behavior for current-month generation and previsional computations when explicit date ranges are not provided.
- Added domain regression tests for explicit range inclusions/exclusions and range-bounded previsional balances.
- Added infrastructure API tests for explicit date-range success and invalid-range failures on account report and transaction listing endpoints.
- Updated English and French performance documentation to describe optional date-range query support and cycle-aware frontend usage examples.

## 2026-03-26

- Started dashboard V1 implementation for account-first analytics by adding optional `accountId` and period filters (`startDate`, `endDate`) to stats domain/API flows (`category-distribution`, `trends`, `previsional`) with backward-compatible defaults.
- Refactored stats domain orchestration to validate partial/invalid date ranges consistently and to scope data access to a selected account while preserving ownership/authorization checks.
- Extended trend and category use cases to support period-aware computations instead of fixed all-time/12-month-only behavior when a period is provided.
- Updated frontend stats composable and dashboard page wiring to send account + period filters, add account/period controls (month/quarter/year), and load scoped dashboard data.
- Extended previsional stats payloads with explicit regular/non-regular split (`regularTransactions`, `nonRegularTransactions`) and dedicated totals to support a clearer “Arrive dans 15 jours” dashboard UX.
- Exposed `regularTransactionId` in stats transaction DTOs so frontend rendering can reliably distinguish recurring vs non-recurring upcoming items.
- Updated dashboard upcoming-transactions card to display separate regular/non-regular sections with distinct labels and totals on the selected account and period window.
- Improved dashboard category section UX by adding top tags insight cards (amount, share, and variation versus previous period) while keeping the simple period-scoped doughnut chart as primary visual.
- Added frontend page test coverage for dashboard tags insights rendering (`client/tests/pages/dashboard-index.spec.ts`).
- Polished dashboard header UX with contextual chips (active date range, 15-day upcoming volume, short-term projected net) to surface critical information faster.
- Updated dashboard wording and KPI labels to be period-aware (`mois` vs `période`) for better clarity in month/quarter/year modes.
- Replaced the previous artificial weekly comparison chart with a real period-expense comparison (`période active` vs `période précédente`) based on scoped category totals.
- Fixed a critical regular transaction API bug where `startDate` from create/update requests was ignored and replaced by `LocalDate.now()`, which could prevent expected previsional generation for the selected month.
- Extended update regular transaction contract to include `startDate` and propagated this field from frontend edit payloads.
- Added integration-test assertion in `RegularTransactionControllerTest` to verify future `startDate` is persisted as requested.
- Added domain regression tests in `StatsFeatureTest` covering selected-account scoping and partial-period validation failures for category and trend stats, plus account scoping for previsional transactions.
- Added domain regression coverage asserting previsional split between regular and non-regular transactions.
- Verified implementation slice with green test runs: `./gradlew :domain:test --tests "fr.sacane.jmanager.domain.port.StatsFeatureTest"`, `./gradlew :domain:test --tests "fr.sacane.jmanager.domain.usecase.TrendCalculatorTest" --tests "fr.sacane.jmanager.domain.usecase.CategoryDistributionCalculatorTest"`, and `pnpm -C client test -- --run`.

## 2026-03-23

- Completed regular transaction deletion cleanup across backend layers: deleting a regular transaction now also removes all related tracker rows and cleanly detaches linked booklets before deletion.
- Added bulk regular transaction deletion support (`DELETE /api/transaction/regular`) with atomic validation: empty selection is rejected, missing IDs fail the operation, and valid selections delete all targeted regular transactions with tracker/link cleanup.
- Preserved historical generated sheets when deleting a regular transaction, relying on existing database referential behavior (`sheet.regular_transaction_id` set to `NULL`) to keep accounting history intact.
- Extended regular transaction API payloads to expose linked booklet identifiers and propagated these identifiers to frontend types and UI state.
- Improved regular transaction edit/delete UX: edit flow now preserves existing booklet links, single deletion confirmation warns when linked booklets are impacted, bulk selection deletion is available from the list view, and the dialog now displays linked booklets.
- Added and updated domain/infra/frontend tests, including an end-to-end API scenario for deleting a regular transaction linked to multiple booklets while verifying tracker cleanup, link cleanup, and generated sheet preservation.
- Added domain, API integration, and frontend page tests for bulk regular transaction deletion flows (success + error cases).
- Added editable booklet association management in regular transaction detail modal: users can now view, add, and remove linked booklets directly from the same dialog.
- Extended regular transaction update backend flow to persist booklet association changes (`bookletIds`) consistently across domain, API, and JPA layers.
- Added regression tests to validate booklet association updates through domain feature tests, infrastructure adapter/controller tests, and frontend modal/page tests.
- Executed full test phase successfully: Gradle backend suites (`./gradlew test`) and frontend Vitest suites (`pnpm test`) are green.

- Improved user action feedback by ensuring key operations display a short, consistent loading state, making the interface feel clearer and more reassuring during fast requests.
- Improved creation dialogs so users now see a dedicated loading screen while a transaction is being saved, preventing accidental repeated actions and clarifying that the request is being processed.

## 2026-03-17

- Started implementation of the unified error-handling refactor with a centralized backend error code catalog (`ErrorCatalog`) in the domain layer.
- Refactored backend API error responses to include a stable `errorKey` alongside `code` in `ProblemDetail` payloads.
- Replaced hardcoded API handler codes with centralized catalog constants for validation/type/date/internal/currency/UUID-not-found cases.
- Improved internal server error handling by introducing a dedicated `InternalServerErrorException` handler that preserves the propagated error code.
- Added and updated infrastructure tests for `ProblemDetailHandler` to validate `code` and `errorKey` behavior.
- Added frontend code-to-message mapping utility (`client/utils/errorCodeMap.ts`) and switched toast error rendering to use API error codes instead of backend message strings.
- Upgraded frontend loading composable to support scoped loading states and a `withLoading` helper for reliable start/stop behavior.
- Added frontend unit tests for error-code parsing/mapping, toast behavior, and scoped loading state management.
- Integrated the new error system with the homemade domain `Result` monad by introducing typed domain error metadata (`code`, `key`, `detail`) while preserving backward-compatible `message` access.
- Propagated `Result` typed error metadata through API exception mapping so `ProblemDetail` can preserve explicit domain `errorKey` values when provided.
- Added domain regression tests for `Result` typed-error behavior (`failure`, `map`, `flatMap`) and infrastructure coverage for explicit `errorKey` propagation.
- Integrated explicit `DomainError` keys in `TransactionFeature` business failure paths (booking, edition, retrieval, delete, preview confirmation) so domain use-cases emit semantic error keys through the `Result` monad.
- Added domain regression coverage to assert `TransactionFeature` keeps expected status and now exposes stable semantic error keys on failure.
- Integrated explicit `DomainError` keys in `BookletFeature` failure paths (find/edit/delete/save/load) so booklet business errors now flow through the `Result` monad with stable semantic keys.
- Added domain regression coverage in `BookletFeatureTest` to assert semantic key propagation on a representative business failure (`BOOKLET_MAXIMUM_SIZE_REACHED`).
- Integrated explicit `DomainError` keys in `UserFeature` failure paths (`login`, `register`, `createAdminIfNotExists`) so authentication and registration errors now carry stable semantic keys through the `Result` monad.
- Integrated explicit `DomainError` keys in `TagFeature` failure paths (`addTag`, `deleteTag`, `editTag`) so tag business errors now expose stable semantic keys.
- Added domain regression assertions in `UserFeatureTest` and `TagFeatureTest` to verify semantic `errorInfo.key` propagation for representative failure scenarios.
- Integrated explicit `DomainError` keys in `StatsFeature` failure paths (missing booklet, forbidden account access, invalid previsional date range).
- Integrated explicit `DomainError` keys in `RegularTransactionFeature` failure paths (`getRegularTransactionById`, `updateRegularTransaction`, `deleteRegularTransaction`).
- Integrated explicit `DomainError` keys in `FileImportExportFeature` failure paths (booklet ownership checks, validation/import/export internal errors, CSV validation blocking import).
- Added domain regression assertions in `StatsFeatureTest`, `RegularTransactionFeatureTest`, and `FileImportExportFeatureTest` to verify semantic `errorInfo.key` propagation for representative failures.
- Refactored frontend `useAdmin` to use the scoped loading manager (`useLoading.withLoading`) instead of ad hoc local loading state, aligning admin fetch UX with the global loading architecture.
- Added frontend unit tests for `useAdmin` to validate pagination population, error callback behavior, and loading reset guarantees.
- Integrated dashboard initial data load with scoped loading (`dashboard.initial`) via `useLoading.withLoading`, replacing the page-local loading flag for consistent loading orchestration.
- Improved admin mobile UX by adding an explicit loading spinner state while users are fetched and disabling pagination interactions during active loading.
- Refactored admin user-creation flow to use scoped loading (`admin.createUser`) via `useLoading.withLoading`, removing the remaining page-local creation loading flag and unifying loading behavior.
- Integrated scoped loading in account details page (`account/[id]`) for critical actions (`loadBookletData`, transaction create/edit/fetch/delete, preview confirmation, CSV export) and wired button loading/disabled states to prevent concurrent operations.
- Added an inline account-level loading indicator while transactions are being refreshed, improving feedback during month/year changes and post-mutation reloads.
- Integrated scoped loading in tags page (`tag/index`) for load/create/edit/delete operations with action-level button loading/disabled states and inline loading feedback during list refresh.
- Integrated scoped loading in booklets page (`account/index`) for initial load/create/delete operations, with loading indicators and interaction guards to prevent concurrent actions.
- Added page-level frontend tests for `tag/index` and `account/index` to validate loading-state rendering and interaction guards introduced by scoped loading integration.
- Added page-level frontend tests for `account/[id]` to validate scoped loading feedback on transaction refresh and export action loading guards.
- Refactored frontend loading scope strings into a shared constant catalog (`client/constants/loadingScopes.ts`) and wired `useAdmin`, `admin/index`, `dashboard/index`, `account/index`, `account/[id]`, and `tag/index` to consume centralized scope keys.

## 2026-03-15

- Fixed `release/*` deploy dispatch metadata to send a direct GitHub Release `.jar` URL instead of an Actions artifact ZIP URL, preventing Deploy from saving a ZIP payload as `Jmanager-<version>.jar` and failing at runtime with an invalid/corrupt JAR.
- Updated deployment documentation to clarify that release payloads now provide a direct `.jar` download URL.
- Added CI integrity checks for generated JARs on `master` and `release/*`: validate local JAR readability (`jar tf`), compute size/SHA256, re-download uploaded artifact in the same workflow, and fail the job if size or hash differs.
- Improved preview transaction confirmation dialog readability in light mode by adding dedicated field and summary styles, making the amount/date inputs clearly visible and visually distinct while preserving dark mode contrast.

## 2026-03-09

- Added `.gitignore` rule for `infra/src/main/resources/static/` to ignore frontend build artifacts generated by the pnpm build.
- Added automated semantic versioning on `master` based on commit types:
  - `fix` bumps patch.
  - `feat`, `chore`, `patch` bump minor.
  - `release` bumps major.
- Added CI integration to bump `gradle.properties`, commit the new version, and build a versioned JAR.
- Updated Gradle configuration so all modules use root project version from `gradle.properties`.
- Replaced direct VPS copy deployment with release-and-dispatch flow:
  - publish `Jmanager-<version>.jar` to GitHub Releases,
  - send `repository_dispatch` payload (`jar_version`, `jar_download_url`, `jar_file_name`) to deploy repository.
- Updated `README.md` with versioning and deployment CI behavior.

## 2026-03-11

- Updated CI version bump flow to avoid committing/pushing on `master` during build.
- Version computation now uses the latest Git tag (`vX.Y.Z`) as primary source of truth, with `gradle.properties` as fallback.
- Build now injects the computed version via Gradle property (`-Pversion=...`) for release JAR generation.

## 2026-03-12

- Added support for changing the date when confirming a preview transaction.
- Extended the confirmation API contract (`PATCH /api/transaction/confirm`) with an optional `newDate` field.
- Updated domain transaction confirmation use case to apply optional `newDate` and `newAmount` in a single confirmation flow.
- Updated account preview confirmation dialog to allow users to edit both amount and date before validating.
- Added and updated domain and infrastructure tests to cover confirmation with date override and preserve existing confirmation behavior.

## 2026-03-14

- Clarified duplicate-detection logic in `RegularTransactionComputer` by replacing inline lambda-label flow (`return@any`) with a dedicated typed predicate helper, improving readability without changing behavior.
- Kept the duplicate matching rules unchanged (real transaction match by `regularTransactionId`, then preview date/id checks, then legacy label+amount fallback).
- Fixed GitHub Actions CI on self-hosted runners by replacing `pnpm/action-setup@v4` (requires `runs.using: node24`) with a Corepack-based pnpm setup compatible with Node 20.
- Changed GitHub Actions release strategy: pushes to `master` now build and deploy without creating a GitHub Release.
- Added a dedicated `release/*` CI path to publish GitHub Releases and attach the versioned JAR only from release branches.
- Updated `release/*` CI flow to also trigger deploy dispatch, so both `master` and `release/*` deploy.
- Updated CI payload behavior on `master` to use workflow artifact download URLs for deploy dispatch.
- Updated deploy dispatch payloads (`master` and `release/*`) to include `jar_download_token` (from `JMANAGER_ARTIFACT_TOKEN` when available, otherwise `github.token`) so the Deploy repository can authenticate artifact ZIP downloads.
- Clarified deployment documentation for `JMANAGER_ARTIFACT_TOKEN` with recommended token type and minimum scopes (`Actions: Read-only`, `Metadata: Read-only`).

## 2026-03-13

- Cleaned duplication in CSV usecases by refactoring shared tag lookup and day-only date parsing paths in `CsvValidationUtils`.
- Removed unnecessary inline comments from CSV validation internals in `CsvFileValidator` while keeping behavior unchanged.
- Added CSV usecase regression test for case-insensitive default tag resolution in `CsvValidationUtilsTest`.
- Cleaned duplicated backend logic in `BookletFeature` by factoring month-range and in-range transaction filtering helpers.
- Removed internal inline comments from `BookletFeature` methods to keep implementation concise while preserving existing behavior.
- Added a domain regression test to verify balances computation includes preview transactions across a current-to-target month range.
- Refactored backend exception handling in `ProblemDetailHandler` to remove duplicated ProblemDetail construction while preserving existing HTTP status codes, titles, and error codes.
- Cleaned backend mapper code in `DatasourceMapper` by removing inline function comments and centralizing RegularTransactionId string-to-UUID conversion.
- Added infrastructure regression tests for RegularTransactionId mapping (valid UUID and malformed value) and for IllegalArgumentException handling without message.
- Added frontend testing setup in `client` with Vitest, Vue Test Utils, and Happy DOM.
- Added unit tests for utility functions (`utils/util.ts`) and date composable behavior (`composables/useDate.ts`).
- Added component tests for `TitleCard.vue` rendering and click callback behavior.
- Added component tests for `DarkModeToggle.vue` and `monthPicker.vue` interactions with Nuxt auto-import mocks.
- Added component tests for recurring transaction frequency selectors (`FrequencySelector.vue` and `MonthlyRepeatSelector.vue`) covering state transitions and validation behavior.
- Added component tests for `TransactionCreationDialog.vue` with composable mocks (`useTag`, `useJToast`) covering validation warning, successful submit, and cancel flow emissions.
- Added component tests for `RegularTransactionDialogCard.vue` with composable mocks (`useTag`) covering tags loading, save, delete, and cancel flows.
- Added page-level tests for `pages/regular-transaction/index.vue` with mocked `useRegularTransaction`, `useJToast`, and `useConfirm` to cover API success/failure flows and user feedback toasts for create/delete actions.
- Added test scripts (`test`, `test:watch`, `test:coverage`) to standardize local frontend test execution.
- Updated SonarQube integration to include frontend analysis and frontend coverage by generating `client/coverage/lcov.info` in CI and wiring it into Gradle Sonar properties.
- Updated Vitest coverage reporters to include `lcov` so SonarQube can import frontend coverage data.
- Fixed GitHub Actions Node setup for frontend/Sonar pipeline by removing pnpm cache initialization from `actions/setup-node` (which required `pnpm` before installation).
- Fixed SonarQube duplicate indexing by scoping root Sonar `sources/tests` to frontend paths only (`client`, `client/tests`) and leaving backend modules to Gradle auto-detection.
