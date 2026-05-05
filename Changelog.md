# Changelog

## 2026-05-06

- **Feature: Booklet transaction page — action buttons layout and visual differentiation**
  - `client/pages/booklet/[id].vue`: In sidebar mode (`isSidebarMode`), moved the `BookletActionButtons` sidebar panel from the **left** to the **right** side of the table by reordering the flex children (table div first, sidebar div second). The `isSidebarMode` condition itself (`window.innerHeight < 768`) is unchanged.
  - `client/pages/booklet/[id].vue`: Changed the sidebar container background from the neutral card background (`bg-[var(--card-bg)]`) to the primary gradient (`bg-gradient-to-b from-[var(--primary)] to-[var(--primary-2)]`), visually differentiating functional action buttons from the filter buttons in `BookletPageHeader`.
  - `client/components/booklet/BookletActionButtons.vue`: In `orientation === 'vertical'` mode, all button classes are switched to white variants (`!border-white/60 !text-white hover:!bg-white/15`) for readability against the colored background. The "add transaction" button uses a semi-transparent white fill (`!bg-white/20 !border-white/50 !text-white`). In `orientation === 'horizontal'` mode, all original colored styles are preserved unchanged. The selection count chip in vertical mode is also adapted (`bg-white/20 border-white/30` with white text/icon).



- **Bug fix: Regular transaction income displayed as expense after page reload**
  - **Root cause**: `JacksonConfig.jacksonCustomizer()` called `builder.modules(module, JavaTimeModule())` which internally **replaces** the entire module list and disables well-known module auto-detection (`findWellKnownModules = false`). This evicted the `KotlinModule` auto-configured by Spring Boot. Without `KotlinModule`, Jackson uses Java bean introspection on `val isIncome: Boolean` — the generated getter `isIncome()` follows the Java boolean convention and strips the `is` prefix, serialising the field as `"income"` instead of `"isIncome"`. The frontend `data.isIncome` evaluated to `undefined` → `false` → "Dépense" on every GET reload.
  - **Why creation appeared correct**: the transaction was pushed from the POST response which, due to a different execution context (Kotlin object still in memory), may have preserved the correct value before the serialised round-trip on the subsequent GET.
  - **Fix (`JacksonConfig.kt`)**: replaced `builder.modules(module, JavaTimeModule())` with `builder.modulesToInstall(module)`. `modulesToInstall` appends to the existing module list without replacing it, preserving the auto-configured `KotlinModule` and `JavaTimeModule`. The explicit `JavaTimeModule()` registration is removed as Spring Boot auto-configures it via `jackson-datatype-jsr310`.
  - **Tests added**:
    - Backend: `Get all regular transactions must return correct isIncome for each transaction` in `RegularTransactionControllerTest.GetAllRegularTransactionsEndpointTest` — verifies `content[0].isIncome == true` on the GET all endpoint.
    - Frontend: `passes isIncome: true to AppTable rows after loading an income transaction from API` in `regular-transaction-index.spec.ts` — verifies that `filteredTransactions` correctly propagates `isIncome: true` to the `AppTable` rows prop after an API load.
  - All 141 frontend tests and all backend tests pass.


- **Bug fix: Sub-tag visibility in booklet table and category distribution**
  - **Domain — `CategoryDistributionCalculatorImpl`**: Fixed sub-tag promotion logic. When a parent tag has no transactions tagged directly (all transactions belong to its sub-tags), each sub-tag now appears as its own top-level entry in the distribution instead of being grouped under an invisible parent. Percentages are computed against the global total. When a parent has at least one direct transaction, the previous grouping behaviour (sub-tags in `subCategories`) is preserved.
  - Added 3 new domain tests in `CategoryDistributionCalculatorTest` covering the promotion scenarios and percentage correctness.
  - **Client — `booklet/[id].vue`**: Added a dedicated "Sous tag" column to the transactions table. The "Tag" column now displays the parent tag (resolved from `tagDTO.parentId`) when the transaction uses a sub-tag; the "Sous tag" column shows the sub-tag label. A `resolveDisplayTag` helper function resolves the effective display tag from the loaded `tags` ref.
  - Added 6 new frontend tests in `booklet-id.spec.ts` covering `resolveDisplayTag` (parent resolution, fallback, null handling) and the two-column tag filter behaviour (parent filter includes sub-tags; sub-tag filter is exact match).

- **Documentation Cleanup — Post-Implementation**
  - Established cleanup workflow for `docs/refactoring/` and `docs/technical/` folders after feature completion.
  - Removed completed refactoring plans: `command-query-bus/`, `extract-auth-from-domain/`, `tag-sealed-class/`, `usecase-handle-refactoring/`.
  - Removed completed technical analysis reports: `caching/2026-05-02-cache-strategy.md` (implementation details captured in Changelog and code).
  - Preserved `.gitkeep` files to maintain folder structure for future work.
  - Updated `AGENTS.md` with "Documentation Cleanup" section describing workflow, preservation strategy, and timing.

- **Cache: Phase 1 implementation (Caffeine local cache)**
  - Added `spring-boot-starter-cache` and `caffeine:3.2.0` dependencies to `application` and `infrastructure` modules.
  - Created `CacheConfig.kt` with 4 named caches: `defaultTag`, `allTags`, `allBooklets`, `userSettings`.
  - `TagRepositoryJpaAdapter`: `@Cacheable` on `defaultTag()` and `getAllDefault()`; `@CacheEvict` on `save()`, `deleteById()`, `patch()`.
  - `BookletJpaRepositoryAdapter`: `@CacheEvict("allBooklets")` on `save()` and `deleteBookletById()`.
  - `UserRepositoryJpaAdapter`: `@Cacheable("allBooklets")` on `findUserByIdWithBooklets()`; `@CacheEvict` on `updateProjectionWindowDays()`.
  - Fixed SpEL key expressions for Kotlin `value class UserId` (inlined at runtime as `UUID`).

- **Refactoring Steps 5-10: Extract authentication from all remaining domain use cases**
  - **Step 5 (Regular Transactions)**: Migrated 8 use cases (`GetAllRegularTransactions`, `BookRegularTransaction`, `GetRegularTransactionById`, `CreateRegularTransaction`, `UpdateRegularTransaction`, `DeleteRegularTransaction`, `PauseRegularTransaction`, `ResumeRegularTransaction`).
  - **Step 6 (Stats)**: Migrated 5 use cases (`GetCategoryDistribution`, `GetGlobalCategoryDistribution`, `GetGlobalFinancialOverview`, `GetDailyEvolution`, `GetMonthlyOverview`).
  - **Step 7 (CSV)**: Migrated 3 use cases (`ValidateCsvFile`, `ImportTransactionsFromCsv`, `ExportTransactionsToCsv`).
  - **Step 8 (User/Admin)**: Migrated 3 use cases (`GetUserSettings`, `UpdateUserSettings`, `GetUsers`). Removed `requiredRoles` role check from `GetUsersService` (Spring Security handles admin authorization).
  - **Step 9 (Logout)**: `LogoutService` no longer wraps body in `session.authenticate()`. Keeps `SessionManager` for `removeSession`/`blacklistRefreshToken`. `LogoutCommand` now carries both `userId: UserId` and `token: SessionToken`.
  - **Step 10 (Cleanup SessionManager)**: Removed `authenticate()` method from `SessionManager` interface and `InMemorySessionManager` implementation. Removed `AuthenticationTest` interface (no longer needed). Updated `SessionManagerTest` to use `findSessionByToken` instead of `authenticate`.
  - All domain services (except Login, Register, RefreshSession, Logout) no longer inject `SessionManager`.
  - All Commands/Queries use `val userId: UserId` (except `LogoutCommand` which retains `token` for session identification).
  - `FeatureTest.launchWithUserId` now registers user in `userState` for settings tests.
  - Full domain test suite green (529 tests). Application compiles clean.

## 2026-05-02

- **Refactoring Step 2: Extract authentication from all 8 remaining booklet use cases**
  - Migrated `DeleteBookletByIdUseCase`, `FindBookletByIdUseCase`, `FindAllRegisteredBookletsUseCase`, `FindByLabelAndUserIdUseCase`, `SaveBookletUseCase`, `RegenerateDeletedPrevisionalTransactionsUseCase`, `LoadTransactionsForBookletForAMonthUseCase`, `LoadBalancesForBookletForAMonthUseCase`.
  - All Commands/Queries now use `val userId: UserId` instead of `val token: SessionToken`.
  - All corresponding services no longer inject `SessionManager` and no longer call `session.authenticate(...)`.
  - `BookletController` and `TransactionController` updated to pass `UserId(currentUser.id)` instead of `SessionToken(currentUser.token)`.
  - `FakeFactory` updated: removed `sessionManager` as first constructor argument for all 8 migrated service instances.
  - `BookletFeatureTest` updated: removed `BookletFeatureAuthTest` inner class (auth no longer validated in domain), removed companion `tokenValue`/`session`/`connectUser` members, all test calls now use `user.id` / `userId` directly. Rewritten the duplicate-label test to validate business rule `BOOKLET_LABEL_EXIST`.
  - All domain tests pass (53/53 for `BookletFeatureTest`, full domain suite green). Application compile clean.

## 2026-05-01

- **Feature: Dashboard charts — mouse wheel Y-axis scale adjustment**
  - Scrolling the mouse wheel over the "Évolution des finances" (Line) or "Comparaison de période" (Bar) chart now adjusts the Y-axis visible range interactively.
  - Scrolling forward (toward the screen) zooms in: the range narrows and shifts down, making detail more visible.
  - Scrolling backward zooms out: the range expands, revealing a wider span of values.
  - Each chart maintains its own independent scale state (`lineChartYMin/Max`, `barChartYMin/Max`).
  - The scale resets to Chart.js auto-scaling whenever the selected period or booklet changes.
  - Page scroll is suppressed while the pointer is inside a chart container (`@wheel.prevent`).
  - No external library added — implemented natively with a proportional step ratio (`CHART_ZOOM_STEP_RATIO = 0.15`).

- **UX: Responsive layout — transaction table pages (booklet/[id] & regular-transaction)**
  - Both pages now use a proper flex height chain (`h-full overflow-hidden` → `flex-1 min-h-0` → `flex-1 min-h-0` on the table container) so the table always fills the viewport to the bottom without scrolling the page. Internal table overflow is handled by PrimeVue `scroll-height="flex"`, allowing the user to scroll within the table when rows exceed the visible area.
  - Removed the fragile `sticky top-0 max-h-[calc(100dvh-1rem)]` hack from `booklet/[id].vue`.
  - Applies to all non-mobile screen sizes; mobile layout is unaffected.

- **UX: Header coherence on regular-transaction page**
  - Replaced inline styles and hardcoded `purple-600/800` gradient with design-token classes (`var(--primary)`, `var(--primary-2)`, `var(--card-bg)`, `var(--card-border)`) to match the `booklet/[id]` header exactly.
  - Removed the 💰 emoji and normalised font size / spacing to align with the rest of the app.

- **UX: Regular transaction table — column improvements**
  - Added `sortable: true` to the **Type** (`isIncome`) and **Fréquence** (`regularity`) columns.
  - Removed the **Date de début** column from the table (was cluttering the view; detail is still available via the edit dialog).
  - Actions column is now always frozen to the right (was conditional on screen width).
  - Removed the unused `isSmallScreen` ref and its resize logic.



- **Refactoring: `Tag` → `sealed class` (Tag.Default / Tag.Personal)**
  - Converted `Tag` from a plain data class with an `isDefault: Boolean` flag to a sealed class with two subtypes: `Tag.Default` and `Tag.Personal`. `isDefault` is preserved as a computed property (`this is Default`).
  - The wrong-construction bug (virtual transactions always saved with the default tag) is now impossible at compile time: there is no `Tag(label, isDefault = false)` constructor to misuse.
  - **Domain**: `Tag.kt` rewritten as sealed class. `noneTag()` returns `Tag.Default("Aucune")`. `defaultTags` uses `Tag.Default(...)`. `asPersonalTag()` returns `Tag.Personal`. `ConfirmVirtualTransactionCommand.tagLabel: String?` replaced by `tag: Tag?` — the application layer is now responsible for constructing the correct subtype before dispatching the command. `EditTagUseCase` smart-cast fixed with a local `val tagId`.
  - **Infrastructure**: `DatasourceMapper` updated to dispatch on `Tag.Default`/`Tag.Personal` in both `asResource()` and `AbstractTagResource.toDomain()`. `TransactionRepositoryJpaAdapter.save()` and `persist()` rewritten with sealed `when` dispatch so the correct FK column (`tag` vs `personalTag`) is always set.
  - **Application**: `ConfirmVirtualTransactionRequest` changed from `tagLabel: String?` to `tagId: String? + tagIsDefault: Boolean`. Controller builds a typed `Tag.Default` or `Tag.Personal` from the payload before dispatching. `ApiMappingExtensions.TagDTO.toDomain()` and `TransactionResult.toModel()` updated to branch on `isDefault`.
  - **Frontend**: `confirmVirtualTransaction` in `useTransaction.ts` now sends `tagId` + `tagIsDefault` instead of `tagLabel`. Call site in `booklet/[id].vue` passes `tagDTO?.tagId` and `tagDTO?.isDefault`. Tests updated accordingly.
  - All 534 domain tests, all infrastructure tests, all application tests, and all 125 frontend tests remain green.

## 2026-04-30

- Added `confirmVirtualTransaction` function to `useTransaction` composable, calling `POST /transaction/virtual/confirm` with source month/year derived from the current booklet view.
- Updated `confirmPreview()` in `booklet/[id].vue` to use `confirmVirtualTransaction` for virtual transactions (no `id`) instead of `saveTransaction`, ensuring source-month exclusion is handled atomically by the backend.
- Persisted preview transactions (with `id`) continue to use the existing `confirmPreviewTransaction` flow unchanged.
- Added tests covering: virtual confirm calls the new endpoint, persisted preview uses the old flow, sourceMonth/sourceYear are correctly derived from the booklet view.
- **Fix: NPE in `TransactionResumeResult.toDTO()`** — `this.transaction.tag!!.toDTO()` was crashing with `NullPointerException: null` in Spring logs when confirming a virtual transaction without a specific tag (tag = null). Fixed by replacing `!!` with a null-safe call and a default `TagDTO("Aucune")` fallback. Added `TagDTO`/`ColorDTO` imports to `Controller.kt`.
- **Fix: `tagLabel` for default tags** — frontend was sending `tagLabel: 'Aucune'` (the fallback label) for transactions with no specific tag. Now sends `undefined` (omitted from JSON → `null` on backend) when `tagDTO.isDefault` is true, ensuring default-tag transactions are saved with no tag association.

- Added `ConfirmVirtualTransactionUseCase` domain use case to atomically persist a virtual transaction as a real (non-preview) transaction and mark the source month as excluded in the `RegularTransactionTracker`.
- Added `POST /api/transaction/virtual/confirm` endpoint in the application layer with `ConfirmVirtualTransactionRequest` DTO.
- Command uses `tagLabel` (optional) to create a domain `Tag` from the provided label instead of looking up by ID.
- Added domain tests covering: happy path (unchanged date), date changed to different month, booklet not found, tracker auto-creation when missing, and tag label propagation.
- Added application tests covering: HTTP 200 on success and HTTP 404 on unknown booklet.

- **Fix: Domain tests — `LoadTransactionsForBookletForAMonthService` day-of-month sensitivity**
  - Running on day 30 (or any day ≥ 29) could crash with `DateTimeException: Invalid date 'FEBRUARY 30'` because `today` was built as `LocalDate.of(currentYear, currentMonth, currentDate.dayOfMonth)` where `currentMonth` came from `startingMonth` (e.g. February) but the actual day was the real calendar day.
  - Fixed by clamping the day to the length of the target month: `currentYM.atDay(minOf(currentDate.dayOfMonth, currentYM.lengthOfMonth()))`.
  - A second flakiness issue caused generation to be skipped when the actual calendar day (e.g. April 30) exceeded the cycle end day (e.g. April 27) because the end-range check used exact-date comparison. Fixed by comparing at `YearMonth` level instead: `!YearMonth.from(today).isAfter(YearMonth.from(resolvedRangeEnd))`.
  - All 534 domain tests now pass regardless of which day the suite is run.

- **Feature: Delete virtual transaction — client-side support**
  - Added `VirtualTransactionDescriptor` type in `client/types/index.d.ts`.
  - Updated `deleteTransaction` in `useTransaction.ts` to accept an optional `virtualTransactions` parameter alongside physical `ids`, sending both in a single `DELETE /transaction` call.
  - Updated `TransactionDeletionDTO` with `excludedVirtualTransactions` field.
  - Rewrote `confirmDelete()` in `pages/booklet/[id].vue` to split selection into physical (id non-null) and virtual (id null, regularTransactionId set) transactions, build descriptors with `{ regularTransactionId, month, year }`, and send a unified payload.
  - Virtual transactions without `regularTransactionId` are skipped with a warning toast.
  - Locally removes deleted physical and excluded virtual transactions from the displayed list.
  - 4 new tests covering: virtual-only deletion, mixed selection, skipped orphan virtuals with warning, and selection of only unsuppressible virtuals.

## 2026-04-29
- **Feature: Delete virtual transaction — exclude virtual transaction occurrences**
  - Added `ExcludeVirtualTransactionUseCase` in the domain layer: marks a month as excluded in the `RegularTransactionTracker` so that virtual transactions are no longer generated for that month.
  - Extended `DELETE /api/transaction` endpoint to accept an optional `virtualTransactions` list alongside physical `transactionIds`. Both can be sent in a single HTTP call.
  - `BookletTransactionsIdRequest` now has optional `transactionIds` (defaults to empty) and `virtualTransactions: List<VirtualTransactionDescriptorDTO>`.
  - `TransactionDeletionResponse` includes a new `excludedVirtualTransactions` field listing the excluded YearMonth strings.
  - Backward compatible: existing clients sending only `transactionIds` continue to work unchanged.
  - Validation: returns 400 if both lists are empty.
  - 4 domain tests covering success, idempotency, and booklet-not-found scenarios.

- **Fix: Dashboard — monthly cycle range off by one month when anchor date falls after cycle start day**
  - `currentDateRange` in `pages/dashboard/index.vue` was passing `periodAnchorDate` directly to `resolveMonthlyCycleRangeFromAnchor`. When the anchor day (e.g. 29) was ≥ the configured cycle start day (e.g. 27), the function resolved the *next* cycle (Mar 27 – Apr 26) instead of the expected one for the displayed calendar month (Feb 27 – Mar 26).
  - Fixed by replacing the call with `resolveMonthlyCycleRangeForTargetMonth(year, month, startDay, endDay)`, which uses day 15 as a safe cycle-neutral anchor.
  - Updated two pre-existing dashboard integration tests whose expected dates reflected the old incorrect behavior.
  - Added 4 new unit tests in `monthlyCycleRange.spec.ts` covering the bug scenario and adjacent months (start=27/end=26 and start=5/end=4 cycles).

## 2026-04-28
- **Feature: Dashboard doughnut chart — slice click toggles amount/percentage center label**
  - Clicking a slice shows its amount (`X.XX €`) in a center overlay label; clicking again toggles to percentage (`XX.X%`); clicking a different slice resets to amount for the new slice.
  - Chart container changed from fixed height to horizontal rectangle layout (`aspect-[16/7]` on desktop, `h-72` on mobile) with legend on the right (desktop) or bottom (mobile).
  - Fixed tooltip callback being lost during `JSON.parse(JSON.stringify(...))` — replaced with object spread to preserve functions.
  - Added space before `€` in tooltip (`X.XX €` instead of `X.XX€`).
  - Center label resets when booklet, period, or anchor date changes.
  - 9 new tests covering all acceptance scenarios (toggle cycle, reset on different slice, initial state, empty data, tooltip format, layout classes).

## 2026-05-12
- **Refactoring: Command/Query Bus — explicit KClass type resolution**
  - Replaced reflection-based `commandType()`/`queryType()` default methods on `CommandHandler`/`QueryHandler` with explicit abstract `val commandClass: KClass<C>` / `val queryClass: KClass<Q>` properties.
  - Each of the 44 UseCase interfaces now provides a default getter (`override val commandClass get() = XxxCommand::class`). Service implementations require no changes — the property is inherited from the interface.
  - `SpringCommandBus` and `SpringQueryBus` now build their handler maps using `it.commandClass.java as Class<*>` / `it.queryClass.java as Class<*>` — zero reflection, fully static and type-safe.
  - This resolves the `Command<Nothing>` JVM Signature issue: Kotlin omits the JVM Signature attribute for `Nothing`-parameterised interfaces, causing `GenericTypeResolver` and `getGenericInterfaces()` to return raw types at runtime. The KClass property bypasses this entirely.
  - `REFACTORING_PLAN.md` updated to reflect actual implementation (all 10 steps ✅ Complete).
  - 833 tests pass across domain, application, and infrastructure modules with no regression.

## 2026-04-26
- **Refactoring: Consolidate UseCase files (Step 18-19)**
  - Merged Command/Query data classes and Service implementations into their corresponding `*UseCase.kt` files across all 8 categories (admin, user, tag, transaction, regularTransaction, stats, booklet, csv).
  - Deleted 86 orphan `*Command.kt`, `*Query.kt`, and `*Service.kt` files — each UseCase is now a single file containing input data class + port interface + `@DomainService` implementation.
  - Shared helpers (`StatsDomainHelper`, `BookletDomainHelper`, `CsvDomainHelper`) and cross-package types (`BookletLoadingResult`, `TransactionDeletionResult`) remain in separate files.
  - Full test suite passes across domain, application, and infrastructure modules with no regression.

## 2026-05-01
- **Feature: Server-side pagination for transactions (domain + application)**
  - Added `pageNumber: Int = 0` and `pageSize: Int = 10` parameters to `BookletFeature.loadTransactionsForBookletForAMonth` interface and implementation.
  - Added `pageNumber`, `pageSize`, `totalElements`, `totalPages` fields to `BookletLoadingResult` data class.
  - Added `Paginator` dependency to `BookletFeatureImpl` — uses `paginator.paginate()` to slice the combined `(currentTransactions + previsionalTransactions)` list before returning; balances (`realSold`, `previsionalSold`) are still computed over all transactions regardless of page.
  - Changed `RegularTransactionFeature.getAllRegularTransactions` return type from `Result<List<RegularTransaction>>` to `Result<Page<RegularTransaction>>`; added `pageNumber: Int = 0` and `pageSize: Int = 10` parameters.
  - Added `Paginator` dependency to `RegularTransactionFeatureImpl`.
  - Updated `FakeFactory` to inject `PaginatorImpl` into both `BookletFeatureImpl` and `RegularTransactionFeatureImpl`.
  - Updated `application` layer:
    - `GET /api/transaction` — added optional `page` and `size` query params; `TransactionListResponse` DTO now includes `pageNumber`, `pageSize`, `totalElements`, `totalPages`.
    - `GET /api/booklet/{id}/transactions` — added optional `page` and `size` query params; `BookletTransactionsResponse` DTO now includes pagination metadata.
    - `GET /api/booklet/report/{id}` — passes `pageSize = Int.MAX_VALUE` to return all transactions (report use-case).
    - `GET /api/transaction/regular` — added optional `page` and `size` query params; endpoint now returns `Page<RegularTransactionDTO>` instead of `List<RegularTransactionDTO>`.
  - Added 5 new domain tests in `LoadTransactionsWithPaginationTest` (BookletFeature).
  - Added 4 new domain tests in `GetAllRegularTransactionsPaginatedTest` (RegularTransactionFeature).
  - Updated `RegularTransactionControllerTest` assertions to match paginated response shape (`content.label`, `content.size()`).

## 2026-04-22
- **Feature: Reusable `AppTable` component (client)**
  - Created `client/components/AppTable.vue`: a generic, typed wrapper around PrimeVue `DataTable` that consolidates all shared DataTable configuration and CSS overrides.
  - Supports props: `columns: AppTableColumn[]`, `rows`, `dataKey`, `selectable`, `rowClass`, `scrollable`, `scrollHeight`, `loading`.
  - Supports events: `row-dblclick`, `update:selection` (fully compatible with `v-model:selection`).
  - Supports dynamic named slots: `#empty`, `#loading`, `#body-{slotName}`, `#header-{headerSlotName}`.
  - Exported `AppTableColumn` TypeScript interface with: `field`, `header`, `sortable`, `style`, `headerStyle`, `slotName`, `headerSlotName`, `selectionMode`, `frozen`, `alignFrozen`, `exportable`.
  - Bundles all shared `:deep()` CSS overrides (header gradient, zebra striping, hover, cell padding, action button styles, preview-row amber highlight).
  - Added 15 Vitest unit tests in `client/tests/components/AppTable.spec.ts` covering: column rendering, empty state slot, selection toggle (enable/disable), selection column visibility, `row-dblclick` event, custom body/header slots, and `rowClass` application.
  - Migrated `client/pages/booklet/[id].vue` to use `AppTable` — removed 80+ lines of duplicated DataTable template and CSS.
  - Migrated `client/pages/admin/index.vue` to use `AppTable` — removed `.users-datatable` DataTable CSS overrides.
  - Migrated `client/pages/regular-transaction/index.vue` to use `AppTable` with a computed `regularTransactionColumns` that handles responsive frozen column ordering — removed duplicated DataTable CSS.
  - Updated `client/tests/pages/regular-transaction-index.spec.ts` to stub `AppTable` instead of `DataTable`/`Column`.
  - All 102 client tests pass green.

## 2026-04-19
- **Fix: Dashboard doughnut chart — display all tags, not just top 5/6**
  - Removed `.slice(0, 6)` from `categoryExpensesData` computed in `client/pages/dashboard/index.vue`: the chart now renders one slice per tag regardless of count.
  - Removed `.slice(0, 5)` from `topTagsInsights` computed: the "Top tags de la période" list now shows all tags for the period sorted by descending expense amount.
  - Added 4 new Vitest component tests covering all Gherkin scenarios from the issue: ≤ 6 tags (no regression), > 6 tags (all shown), sort order preserved, empty state.
  - Introduced `settleDashboard()` helper in `dashboard-index.spec.ts` to reliably drain nested `Promise.all` chains in the loading sequence.

## 2026-04-15
- **Refactor: Split infrastructure layer into `application/` and `infrastructure/` modules**
  - Extracted the **application layer** (`application/`) from the monolithic `infra/` module: REST controllers, DTOs, mappers, security config, session management, Spring Boot entry point, and all API-related configuration.
  - Extracted the **infrastructure layer** (`infrastructure/`) with SPI adapters, JPA entities, Spring Data repositories, Flyway migrations, and datasource configuration.
  - Created **`build-logic/`** module with Gradle convention plugins centralizing Kotlin 2.0.0 / Java 21 / JaCoCo / test configuration across all modules.
  - Updated dependency graph: `application → domain + infrastructure`, `infrastructure → domain`.
  - Ensured cross-module JPA entity/repository discovery works with the new module split via the application/infrastructure configuration setup.
  - Added infrastructure exception handler in `ProblemDetailHandler` to correctly map `infrastructure.spi.NotFoundException` to HTTP 404.
  - Changed `internal` visibility to `public` on `DatasourceMapper` extension functions for cross-module accessibility.
  - All 217 application tests, 76 infrastructure tests, and domain tests pass green.
  - Updated CI workflow (`build.yml`) to run tests for all three backend modules.
  - Updated `AGENTS.md` architecture documentation.

## 2026-05-17
- **Refactor: `SessionToken` value class — eliminate raw String tokens from domain port contracts**
  - **New domain model**: introduced `SessionToken(@JvmInline value class)` in `domain.models`, wrapping the raw JWT string with a domain-meaningful type at zero runtime cost.
  - **`SessionManager` port**: all interface methods (`authenticate`, `removeSession`, `findSessionByToken`, `getSession`) now accept `SessionToken` instead of `String`. The raw string is unwrapped only inside `InMemorySessionManager` where JWT parsing is needed (`token.value`).
  - **All 8 domain features** (Booklet, Transaction, User, Admin, Tag, RegularTransaction, Stats, FileImportExport): interface signatures and `@DomainService` implementation overrides updated to `SessionToken`.
  - **All 7 infra controllers** (session, booklet, admin, tag, stats, transaction, csv): conversion boundary is now explicit — `SessionToken(currentUser.token)` at each call site; infra `UserDetail.token: String` unchanged.
  - **All domain and infra test files** updated to pass `SessionToken(...)` at feature/port call sites. `FeatureTest.BookletTokenUserId.tokenValue` and `TokenUserId.tokenValue` changed to `SessionToken`.

- **Tests: explicit sliding lifetime coverage for `AccessToken`**
  - Added `domain/src/test/.../models/AccessTokenTest.kt` with 5 business-rule tests:
    - fresh token is not expired
    - past-expiration token is expired
    - `updateLifetime()` extends expiry to `now + TOKEN_LIFETIME_IN_MINUTES` and revives an expired token (sliding window)
    - `updateTokenLifetime()` extends refresh token lifetime by `REFRESH_TOKEN_LIFETIME_IN_DAYS`

## 2026-04-11
- **Refactor: replace legacy `Sheet` naming by `Transaction` across layers**
  - **Domain**: renamed legacy APIs and methods (`deleteSheetsByIds`→`deleteTransactionsByIds`, `deleteAllSheetsById`→`deleteAllTransactionsById`, `findBookletByLabelWithSheets`→`findBookletByLabelWithTransactions`, `retrieveSheetSurroundAndSortedByDate`→`retrieveTransactionsSortedByDate`, `sheets()`→`transactionsSnapshot()`).
  - **Infrastructure Kotlin/JPA**: renamed persistence field names (`idSheet`→`idTransaction`, `BookletResource.sheets`→`BookletResource.transactions`) and aligned repository/adapters method names accordingly.
  - **Database**: added Flyway migration `V20__rename_sheet_to_transactions.sql` to rename table `sheet`→`transactions`, columns `id_sheet`→`id_transaction`, `label_sheet`→`label_transaction`, linked-table foreign key columns, and related index/constraint names.
  - **Frontend**: renamed legacy client symbols (`SheetDTO`→`TransactionDTO`, `SheetAverageDTO`→`TransactionAverageDTO`, `selectedSheets`/`actualSheets`→`selectedTransactions`/`actualTransactions`) for coherent naming with backend/domain.

- **Refactor start: session/auth multi-client foundation (cookie + bearer)**
  - **Infra auth filter**: `JwtCookieAuthenticationFilter` now extracts the token from either `Authorization: Bearer <token>` or the `token` cookie (cookie fallback preserved). This enables non-browser clients without breaking the existing web flow.
  - **Domain auth use-case**: added refresh-token driven rotation in `UserFeatureImpl` (`UserFeature.refresh(refreshToken: UUID)`), with refresh blacklist of used tokens and new refresh-token issuance.
  - **Session API**: `POST /api/user/auth/refresh/{userId}` now authenticates with a dedicated refresh token (`refresh_token` cookie or header), no longer requiring an authenticated access token.
  - **Non-browser compatibility**: refresh endpoint also supports explicit `X-Refresh-Token` header transport, and login/refresh responses now expose `refreshToken` in `UserStorageDTO` for clients that do not rely on cookies.
  - **Cookies**: login/refresh now emit both `token` and `refresh_token` HttpOnly cookies (`refresh_token` max-age 7 days); logout clears both cookies.
  - **Security**: `/api/user/auth/refresh/**` is now explicitly `permitAll` to allow refresh when access token is expired.
  - **Infra tests**: added integration coverage for bearer authentication (`SessionControllerTest`, `AdminControllerTest`) and refresh endpoint behavior (cookie, bearer, and mismatched userId unauthorized case).
  - **Frontend auth alignment**: updated `useAuth.ts` + `useQuery.ts` to consume the refresh endpoint contract (`response.data.token`), refresh persisted user state, and trigger refresh on domain timeout code `1` before logout.
  - **Redis-ready session contract for refresh tokens** (without Redis implementation):
    - `SessionManager` now exposes refresh-token lifecycle operations (`saveRefreshToken`, `authenticateRefreshToken`, `blacklistRefreshToken`, `findSessionByToken`) to keep refresh persistence behind a port.
    - `InMemorySessionManager` now persists refresh tokens and a refresh-token blacklist in dedicated stores, including purge of expired refresh and blacklist entries.
    - `UserFeatureImpl` now uses only `SessionManager` for refresh-token persistence/rotation/revocation on login, refresh, and logout.
    - Added domain regression tests in `SessionManagerTest` and `UserFeatureTest` for refresh-token save/auth/blacklist/rotation behavior.

- **Frontend auth: remove localStorage dependency (cookie-first bootstrap)**
  - Added `POST /api/user/auth/refresh` (without `userId`) so browser clients can bootstrap/restore auth state from HttpOnly cookies only.
  - `useAuth.ts` no longer reads/writes `localStorage` for user or refresh token; auth state is now held in Nuxt in-memory state and initialized through refresh bootstrap.
  - `useQuery.ts` no longer mutates `localStorage` on 401/403 and delegates session teardown to `useAuth.logout()`.
  - Added infra integration test coverage for `/api/user/auth/refresh` success path.
  - Preserved Nuxt-native `defineNuxtRouteMiddleware` implementation in `client/middleware/*` and adapted Vitest setup with a macro stub for test runtime compatibility.

- **Security: String length enforcement across all layers**
  - **DTO fix**: `BookletBookingRequest.label` `@Size(max=100)` changed to `@Size(max=30)` to match the database `VARCHAR(30)` constraint. Prevented potential data truncation errors.
  - **JPA entities**: Added explicit `@Column(length=)` annotations to `UserResource` (username=100, password=255, email=255), `TransactionResource` (label=255), and `AbstractTagResource` (name=50) for database-level defense in depth.
  - **Frontend**: Added `maxlength` attributes on all user-facing text inputs — login username/password (100), booklet booking label (30), transaction label (100), regular transaction label (100), tag create/edit labels (50). Prevents oversized input from reaching the backend.

## 2026-04-10
- **Fix: unexpected logout on page reload (frontend auth race condition)**
  - **Root cause**: concurrent session initialization paths (`useAuth` eager init + route middleware init) could trigger a refresh call overlap where one path observed `isAuthenticated=false` while another refresh was still in-flight, causing an unintended redirect to `/login`.
  - **Frontend fix**: `useAuth.tryRefresh()` now serializes concurrent refresh attempts through a shared in-flight promise and all callers await the same result.
  - **Frontend fix**: removed eager auto-initialization side effect in `useAuth`; session bootstrap remains explicit via route middleware (`initializeSession`) to avoid duplicate startup refresh calls.

- **Security: HikariCP connection pooling (B3-05)**
  - Replaced `DriverManagerDataSource` with `HikariDataSource` in `DatasourceConfig`. Enables connection pooling with configurable pool size, connection timeout, idle timeout, and leak detection threshold.
- **Security: Restrict Content-Type on JSON endpoints (B1-09)**
  - Added `consumes = [MediaType.APPLICATION_JSON_VALUE]` to all `@PostMapping`, `@PutMapping`, `@PatchMapping`, and `@DeleteMapping` endpoints that accept `@RequestBody` across session, booklet, tag, transaction, and CSV controllers. Prevents unexpected content-type deserialization.
- **Security: Validate year parameter in StatsController (B1-07)**
  - Added `@Min(1900) @Max(2100)` validation on the `year` `@PathVariable` in `getMonthlyBookletStats()`. Added `@Validated` on `StatsController`.
- **Security: Limit HTTP POST body size (B1-10)**
  - Added `server.tomcat.max-http-post-size=2097152` (2 MB) to `application.properties`. Limits the maximum size of JSON request bodies accepted by Tomcat.
- **Security: Generic login error message (F-03)**
  - Changed the login failure message from a specific "Le nom d'utilisateur et le mot de passe ne correspondent pas" to the generic "Identifiants incorrects" to prevent user enumeration.
- **Security: CSV formula injection protection (F-05)**
  - `CsvTransactionExporter.escapeCsvField()` now sanitizes fields starting with `=`, `+`, `-`, `@`, `\t`, or `\r` by prepending a single quote. Prevents CSV formula injection when exported files are opened in spreadsheet applications.
- **Security: Bean Validation on all API input DTOs (B1-01)**
  - Added `jakarta.validation` annotations (`@NotBlank`, `@Size`, `@Min`, `@Max`, `@Positive`, `@NotEmpty`, `@Valid`) to all DTOs used as `@RequestBody` inputs: `UserPasswordDTO`, `RegisteredUserDTO`, `UserSettingsUpdateDTO`, `BookletMonthlyCycleUpdateDTO`, `BookletBookingRequest`, `UserTagRequest`, `ColorDTO`, `TagDTO`, `TransactionResult`, `UserBookletIdsTransactionRequest`, `BookletTransactionsIdRequest`, `MonthlyRegularTransactionRequest`, `UpdateRegularTransactionRequest`, `RegularTransactionsDeletionRequest`, `CsvExportRequestDTO`, `ConfirmPreviewRequest`.
  - Added `@Valid` annotation to all `@RequestBody` parameters across all controllers (session, booklet, transaction, tag, csv).
  - Added `spring-boot-starter-validation` dependency to `infra/build.gradle.kts`.
- **Security: Hide internal exception details from API responses (B1-04)**
  - `ProblemDetailHandler`: replaced `ex.message` in generic error responses with safe static messages (`"An unexpected internal error occurred"`, `"Invalid type for the provided parameter"`, `"The provided currency is not supported"`, `"Invalid argument provided"`). Internal details remain logged server-side.
  - `MethodArgumentNotValidException` handler now returns field-level validation errors (`field: message`) instead of raw exception messages.
  - Updated all corresponding `ProblemDetailHandlerTest` assertions.
- **Security: Ownership verification on booklet operations (B3-01/B3-02)**
  - Added `userOwnsBooklet()` helper in `BookletFeatureImpl` to verify that a booklet belongs to the requesting user before allowing `deleteBookletById` or `editBooklet`. Returns `FORBIDDEN` with a domain error key if ownership check fails.
  - Added domain tests: `Deleting a booklet owned by another user should be forbidden`, `Editing a booklet owned by another user should be forbidden`. Fixed pre-existing test that was using wrong user/token pairing.
- **Security: Login button loading state (F-01)**
  - Added `:loading` and `:disabled` binding to the login button in `login.vue`. The button shows a spinner while the login request is in flight and is disabled when credentials are empty or a request is already running.
- **Security: Rate limiting on login endpoint (B2-01)**
  - Implemented an in-memory `LoginRateLimiter` component that blocks an IP after 5 failed login attempts within a 15-minute sliding window. Returns HTTP 429 when rate-limited. Attempts are cleared on successful login.
- **Security: JWT filter returns 401 instead of 404 (B2-05)**
  - `JwtCookieAuthenticationFilter` now returns HTTP 401 Unauthorized (instead of 404 Not Found) when the token references a non-existent user or when an authentication error occurs. Prevents leaking user existence information.
- **Security: Mask token in logs (B2-06)**
  - `JwtTokenGenerator.readToken()` no longer logs the raw token value on error. Replaced `println` with a proper `Logger` that only logs the exception type.
- **Security: SameSite cookie attribute (B2-04)**
  - Login and logout cookies now use Spring `ResponseCookie` with `SameSite=Strict`, `Secure` (configurable per profile), and `HttpOnly` attributes. Defends against CSRF attacks.
- **Security: Validate admin pagination parameters (B1-03)**
  - Added `@Min(0)` on `page` and `@Min(1) @Max(100)` on `size` in `AdminController.getCreatedUsers()`. Added `@Validated` on the controller and a `ConstraintViolationException` handler in `ProblemDetailHandler`.
- **Security: Replace localStorage with sessionStorage (F-02)**
  - User authentication data (`id`, `username`, `roles`) is now stored in `sessionStorage` instead of `localStorage` in `useAuth.ts` and `useQuery.ts`. Reduces XSS exposure: data is tab-scoped and cleared when the tab closes.
- **Security: Optimistic locking on booklet and transaction entities (B3-03/B3-04)**
  - Added `@Version` column to `BookletResource` and `TransactionResource` JPA entities. Flyway migration `V19` adds `version` columns. Prevents silent data loss from concurrent balance updates (race condition). Added `StaleObjectStateException` handler in `ProblemDetailHandler` returning HTTP 409 Conflict.
- **Fix: dashboard quarter/year period uses future months instead of rolling past window**
  - **Root cause**: `currentDateRange` for `quarter` used `startOfQuarter` / `endOfQuarter` (calendar quarter, e.g. Apr–Jun for April), and for `year` used `startOfYear` / `endOfYear` (Jan–Dec). Both included future months instead of showing the most recent completed period.
  - **Frontend fix**: Changed `currentDateRange` to rolling windows: quarter = 3 months ending at the anchor month (`startOfMonth(anchor - 2 months)` → `endOfMonth(anchor)`); year = 12 months ending at the anchor month (`startOfMonth(anchor - 11 months)` → `endOfMonth(anchor)`).
  - **Frontend fix**: Updated `previousDateRange` to match the rolling logic: previous quarter = months −5 to −3; previous year = months −23 to −12.
  - **Frontend fix**: Updated `selectedPeriodLabel` to display the actual rolling range (e.g. "fév - avr 2026" for quarter, "mai 2025 - avr 2026" for year) instead of calendar-based labels ("T2 2026", "2026").
  - **Domain tests** (rolling-window, 6 new): added to `TrendCalculatorTest` — covers exact month boundaries for 3-month window, for 12-month window, KPI aggregation over 3 months, and exclusion of transactions before/after each window. Full domain test suite: 18 tests, all green.
- **Fix: intermittent/partial transaction selection in monthly booklet view**: Selecting all transactions (or selecting multiple rows) could behave inconsistently when some transactions had no persistent `id` (virtual/preview rows).
    - **Root cause**: Selection logic relied on `id` equality only. Rows with `id = null` were treated as the same item, causing incomplete or unstable multi-selection behavior.
    - **Frontend fix**: Added a deterministic per-row `selectionKey` for display rows, switched desktop `DataTable` selection to `data-key="selectionKey"`, and updated mobile selection helpers (`toggleSelection`, `isSelected`) to compare rows by `selectionKey` instead of raw `id`.
    - **Stability improvement**: Selection is now reconciled after list reloads so stale selected rows are dropped if they are no longer present in the refreshed dataset.
    - **Frontend tests**: Added regression coverage in `client/tests/pages/booklet-id.spec.ts` to verify that multiple preview transactions with `id = null` can be selected independently.

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
