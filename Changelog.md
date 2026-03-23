# Changelog

## 2026-03-23

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
