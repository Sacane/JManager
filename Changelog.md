# Changelog

## 2026-04-09
- **Fix: incorrect monthly expenses/income with a custom monthly cycle**: On the dashboard, the "Monthly expenses" and "Monthly income" KPIs were including transactions outside the selected date range when a custom monthly cycle (start day ≠ 1st of month) was configured.
  - **Root cause**: `TrendCalculatorImpl.calculateTrend` filtered transactions by calendar month only (year/month), ignoring the exact day boundaries of `startDate`/`endDate`. A range 28/02–27/03 would therefore include all transactions from the entire February and the entire March.
  - **Domain fix**: Added `(startDate == null || !transaction.date.isBefore(startDate)) && (endDate == null || !transaction.date.isAfter(endDate))` to the transaction filter in `TrendCalculatorImpl`, aligning its behaviour with the already-correct `CategoryDistributionCalculatorImpl`.
  - **Domain tests**: Added two tests to `TrendCalculatorTest`: one for transactions before `startDate` (must be excluded) and one for transactions after `endDate` (must be excluded).

## 2026-04-08
- **Fix: tag colors in "dépenses par catégorie" chart**: The pie/doughnut chart on the dashboard was displaying hardcoded colors instead of the actual colors saved by the user for each tag.
    - **Domain**: Added `tagColor: java.awt.Color` field to `CategoryData` model. Updated `CategoryDistributionCalculatorImpl` to group transactions by `Triple(label, id, color)` and propagate the tag color into each `CategoryData` instance.
    - **Domain tests**: Added `assertEquals(Color.RED/GREEN, categories[i].tagColor)` assertions to the single-category and multi-category distribution tests.
    - **Infra DTO**: Added `colorDTO: ColorDTO` field to `CategoryDataDTO`; updated the `CategoryData.toDTO()` mapper to call `tagColor.toDTO()`.
    - **Infra API tests**: Updated `GetCategoryDistributionEndpointTest` to assert that `categories[0].colorDTO` is not null.
    - **Frontend types**: Added `colorDTO: { red: number, green: number, blue: number }` to `CategoryDataDTO` interface.
    - **Frontend**: Replaced the 6 hardcoded `backgroundColor` values in `categoryExpensesData` computed with `rgb(colorDTO.red, colorDTO.green, colorDTO.blue)` derived from the tag's actual color. Full test suite green.

- **Regenerate deleted previsional transactions**: Added the ability to regenerate previsional transactions that were previously deleted by the user for a given month/year, for all regular transactions linked to a booklet.
  - **Domain SPI**: Added `unmarkMonthAsExcluded(regularTransactionId, bookletId, year, month)` to `RegularTransactionTrackerRepository` port.
  - **Domain fake**: Implemented `unmarkMonthAsExcluded` in `InMemoryRegularTrackerRepository`.
  - **Domain feature**: Added `regenerateDeletedPrevisionalTransactions(token, bookletId, month, year)` to `BookletFeature` interface and implemented in `BookletFeatureImpl` (un-marks the exclusion for each tracker where the target month was excluded, then re-runs the previsional transaction generator).
  - **Bug fix**: `RegularTransactionComputer.generateMissingPrevisionalTransactions` was erasing `excludedMonths` when upserting the tracker (defaulting to `emptySet()`); fixed to preserve the existing `excludedMonths` set.
  - **Infra SPI**: Implemented `unmarkMonthAsExcluded` in `RegularTransactionTrackerRepositoryAdapter`.
  - **Infra API**: Added `POST /api/booklet/{bookletID}/transactions/regenerate?month=X&year=Y` endpoint in `BookletController`, returning the list of regenerated transactions.
  - **Frontend**: Added `regenerateDeletedPrevisionalTransactions` in `useBooklet.ts`; added `hasRegenerableTransactions: boolean` to `BookletTransactionsDTO`; added `regenerate` loading scope to `LOADING_SCOPES`; added "Régénérer" button in the booklet detail page action bar (icon-only desktop, labelled mobile), visible only when `hasRegenerableTransactions == true` (i.e. at least one regular transaction has the current month excluded).
  - Full test suite (domain 44 tests + infra 4 new API tests + frontend 83 tests) green after all changes; also fixed a kotlinx-serialization `encodeDefaults = false` issue by removing the default value from `BookletTransactionsResponse.hasRegenerableTransactions`.

## 2026-04-07

- **Tag deletion warning with forced reassignment**: Deleting a personal tag that is assigned to transactions or regular transactions now shows a confirmation dialog informing the user. If the user confirms, all affected transactions are reassigned to the default tag (`Aucune`) and the tag is deleted. The backend returns 409 Conflict when the tag is in use (without `force=true`); a second call with `?force=true` performs the reassignment and deletion.
  - **Domain**: Added `TAG_IN_USE(2005)` to `ResultState`; extended `TransactionRepository` and `RegularTransactionRepository` SPI ports with `isPersonalTagUsed(tagId)` and `replacePersonalTagByDefault(tagId, defaultTag)`; updated `TagFeature.deleteTag` signature to accept `force: Boolean = false` and injected both transaction repos into `TagFeatureImpl`; guard logic: TAG_IN_USE when in use and force=false, reassign before deletion when force=true.
  - **Domain tests**: Added two new `DeleteTagFeatureTest` cases — "used tag without force → TAG_IN_USE" and "used tag with force → success + reassigned to default tag".
  - **Infra SPI**: Added `existsByPersonalTagId` and `replacePersonalTagByDefaultId` native-SQL queries to `TransactionJpaRepository` and `RegularTransactionResourceJpaRepository`; implemented new port methods in `TransactionRepositoryJpaAdapter` and `RegularTransactionRepositoryDataJpaAdapter`.
  - **Infra API**: Added `ConflictException`; mapped `TAG_IN_USE` → HTTP 409 in `ApiMappingExtensions` and added `@ExceptionHandler(ConflictException)` in `ProblemDetailHandler`; updated `TagController.deleteTag` to accept `?force=true` request param.
  - **Infra API tests**: Added two new `DeleteTagEndpointTest` cases — "used tag without force → 409" and "used tag with force → 200 + transaction reassigned to default".
  - **Frontend**: `useTag.deleteTag` now accepts `force: boolean = false` and correctly propagates errors (removed silent `.catch` that was swallowing 409 responses). `onDeleteClick` in `tag/index.vue` refactored into `performDeleteTag`: on 409 it shows a second PrimeVue confirm dialog explaining the tag is in use, and on confirm retries with `force=true`; error code `2005` added to `errorCodeMap.ts`.
  - Full test suite (domain + infra + frontend) green after all changes.

- **Regular Transaction Link/Unlink feature**: Added dedicated endpoints and UI to link or unlink a regular transaction to/from a booklet, replacing the previous booklet management inside the update modal.
  - **Domain**: Added `linkBooklet` / `unlinkBooklet` to `RegularTransactionRepository` SPI port; added `deleteTrackerByPair` to `RegularTransactionTrackerRepository` SPI port; added `linkRegularTransactionToBooklet` / `unlinkRegularTransactionFromBooklet` to `RegularTransactionFeature` port and implemented in `RegularTransactionFeatureImpl` (guards: already-linked → 400, not-linked → 400, not-found → 404). Unlink automatically deletes the tracker entry for the pair to stop virtual/preview transaction generation for that booklet.
  - **Infra SPI**: Added `deleteByRegularTransactionIdAndBookletId` JPA method to `JpaRegularTransactionTrackerRepository`; added `link` / `unlink` transactional operations to `RegularTransactionOperator`; implemented `linkBooklet` / `unlinkBooklet` in `RegularTransactionRepositoryDataJpaAdapter`.
  - **Infra API**: Added `POST /api/transaction/regular/{transactionId}/link/{bookletId}` and `DELETE /api/transaction/regular/{transactionId}/link/{bookletId}` endpoints in `TransactionController`.
  - **Frontend**: Added `linkRegularTransactionToBooklet` / `unlinkRegularTransactionFromBooklet` in `useRegularTransaction.ts`; removed the booklet multi-select from the update modal (`RegularTransactionDialogCard`); added Link and Unlink action buttons per row in the regular transaction DataTable with dedicated dialog modals.
  - Full test suite (domain + infra) green after all changes.

## 2026-04-06

- Fixed bug where editing the tag of a regular transaction had no effect (200 OK returned but tag reverted to original value): `tag` and `personalTag` fields were declared `val` (immutable) in `AbstractRegularTransactionResource` and `RegularTransactionEntity`, making mutation impossible; changed both to `var`. Added full tag-mapping logic in `RegularTransactionOperator.update()` mirroring the existing `save()` behaviour (DefaultTag / PersonalTag / fallback to unknown tag), so the correct tag is now persisted on every update.
- Booklet detail page (desktop): moved transaction filter buttons (All / Confirmed / Forecasted) from the bottom bar into the header, next to the month/year pickers, as compact icon-only buttons with tooltip counts. Bottom bar now shows filters only on mobile. Year picker compacted to free horizontal space.
- Booklet detail page: delete action when transactions are selected now shows as an icon-only button aligned with the other action icons; selection count and computed amount appear as a pill to the left of the action group (desktop), or inline above the table (mobile) — no more separate labelled "Supprimer" button.

## 2026-04-05

- Completed the full "account → booklet" rename across every remaining layer and file.
- **Database**: Added migration `V16__rename_account_fk_to_booklet.sql` (`sheet.account_id_booklet`→`booklet_id_booklet`), `V17__rename_account_amount_to_booklet_amount.sql` (`sheet.account_amount`→`booklet_amount`), `V18__rename_regular_transaction_booklet_fk_column.sql` (`regular_transaction_booklet.id_account`→`id_booklet`).
- **Domain source**: Renamed parameter `account: String`→`bookletLabel: String` in `TransactionFeature.retrieveTransactionsByMonthAndYear` (interface + implementation); renamed local variable `registeredAccount`→`registeredBooklet` in `TransactionFeature.editTransaction`; updated all KDoc comments across `TransactionFeature`, `BookletFeature`, `StatsFeature`, `UserFeature`, `BookletRepository`, `TransactionRepository`, `PrevisionalTransactionFilter` and `TrendCalculator`.
- **Domain tests**: Renamed all local variables, inner class names, test data labels and test names from account to booklet across `BookletFeatureTest`, `TransactionFeatureTest`, `StatsFeatureTest`, `RegularTransactionFeatureTest`, `FileImportExportFeatureTest`, `UserFeatureTest`, `FeatureTest`, `PrevisionalTransactionFilterTest`, `TrendCalculatorTest` and `BookletTest`.
- **Infra tests**: Renamed all local variables, test data strings, test names and method calls from account to booklet across `TransactionRepositoryJpaAdapterTest`, `RegularTransactionRepositoryDataJpaAdapterTest`, `RegularTransactionStateForTestAdapter`, `InfraUserTest`, `SpaControllerTest`, `TransactionAdapterSqlTest`, `BookletJpaRepositoryAdapterTest`, `DatasourceMapperTest`, `BookletControllerTest` and `TransactionControllerTest`.
- Full test suite passes across all layers after all migrations and renames.

## 2026-04-04

- Updated `AGENTS.md` with a new mandatory implementation strategy section defining the required execution order for all features/fixes: strict TDD cycle (red/green/refactor with incremental steps), domain-first design (entity/port/contracts and domain tests with in-memory fakes), then infra SPI/database implementation and tests, then infra API implementation and API behavior tests, and mandatory final full-stack validation (API + Domain + Database).

## 2026-04-04

- Completed comprehensive rename of "account" to "booklet" across all layers (domain, infra, client).
- **Domain**: Renamed `AccountMonthlyCycleSetting`→`BookletMonthlyCycleSetting`, `AccountMonthlyCycleUpdate`→`BookletMonthlyCycleUpdate`, `accountCycles`→`bookletCycles` in `UserSettings`; renamed `AccountRepository` port to `BookletRepository` with all methods; renamed `UserRepository.findUserByIdWithAccounts`→`findUserByIdWithBooklets`; updated `Booklet` constructor param `labelAccount`→`label`; added `MonthlyAccountStatsOutput` field renames `accountId`→`bookletId`, `accountLabel`→`bookletLabel`; updated `TransactionRepository` SPI param names `accountLabel`→`bookletLabel`, `accountId`→`bookletId`.
- **Database**: Added migration `V15__rename_account_to_booklet.sql` to rename the `account` table to `booklet`, its primary key column `id_account`→`id_booklet`, and the foreign key column `sheet.account_id_account`→`sheet.account_id_booklet`.
- **Infra**: Renamed all JPA entities, repositories, adapters, mappers, DTOs and API controllers to use booklet naming; updated API URLs from `/api/account`→`/api/booklet` and stats endpoint path param `{accountId}`→`{bookletId}`; renamed session cycle DTOs `AccountMonthlyCycle*`→`BookletMonthlyCycle*`.
- **Client**: Renamed page files and routes from `account` to `booklet`; updated all composables, types, components and test files to use booklet naming consistently.
- Full test suite passes across all layers (462 domain tests, 272 infra integration tests).

## 2026-04-03

- Fixed ghost virtual transaction appearing after confirming a forecasted (preview) transaction with a changed date: `calculateVirtualTransactions` now deduplicates confirmed transactions by `(regularTransactionId, YearMonth)` rather than exact date, consistently with `generateMissingPrevisionalTransactions`, so a confirmed occurrence covers its full month regardless of the date it was moved to.
- Added domain regression test covering the "confirm preview with date change" scenario in `RegularTransactionComputerTest`.

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
