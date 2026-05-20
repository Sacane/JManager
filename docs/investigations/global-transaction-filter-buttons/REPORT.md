# Investigation Report — Global Transaction Filter Buttons

**Date:** 2026-05-19
**Status:** Draft

---

## 1. Problem Statement

The current transaction filter buttons ("Tout", "Confirmées", "Prévisionnelles") operate only on the **currently loaded page** of transactions. When pagination is active (e.g., page size = 10), clicking "Prévisionnelles" shows only the previsional transactions among those 10 rows — not all previsional transactions for the month.

The goal is to add two global filter buttons that:
1. **Show all transactions** for the current month without pagination constraints.
2. **Show all previsional transactions** for the current month without pagination constraints.

---

## 2. Context

### Current pagination architecture

The backend endpoint `GET /api/booklet/{bookletID}/transactions` supports `page` and `size` query params. Internally, the `LoadTransactionsForBookletForAMonthService` loads **all** transactions for the requested period into memory, then calls `Paginator.paginate()` to slice. The DB query itself already fetches the full set — pagination is purely in-memory post-processing.

This means: **sending a large page size (or `Int.MAX_VALUE`) returns all transactions at zero additional DB cost.**

### Existing equivalent: the report endpoint

`GET /api/booklet/report/{bookletID}` already calls the same use case with `pageSize = Int.MAX_VALUE` (see `BookletController.findBookletReportByIdMonthAndYear`). The `findByIdMonthAndYear()` composable in `useBooklet.ts` already wraps this endpoint. The response format is `BookletReport { label, transactions, realSold, previewSold }` — the same `TransactionResult` DTOs as the transactions endpoint.

### Current frontend state

In `pages/booklet/[id].vue`:
- `transactionFilter: ref<'all' | 'preview' | 'confirmed'>` — local display filter applied on `actualTransactions` (the current page).
- `filteredTransactions` computed — client-side filter on the page content.
- `currentPage`, `pageSize`, `totalElements`, `totalPages` — pagination state.
- `BookletFilterActionBar` and the sidebar filter buttons emit `update:transactionFilter`, which only affects local rendering.

The counts displayed on buttons (`transactionsCount`, `previewTransactionsCount`) are derived from `actualTransactions.length` and `actualTransactions.filter(t => t.isPreview).length` — **page-local counts, not total counts**.

### What doesn't need changing

The per-page filter buttons are useful for quickly inspecting the current page. They should be kept. The two new buttons complement them with a global scope.

---

## 3. Impact Analysis

### Domain Layer

Not applicable — no domain rule or invariant is involved. The use case already supports unpaged loading via `pageSize = Int.MAX_VALUE`. No new port or domain change is needed.

### Infrastructure Layer

Not applicable — the DB query (`TransactionQueryRepository.findByBookletIdAndDateBetween`) already returns the full set. Pagination is in-memory. No new query method is needed.

### Application Layer

Not applicable if reusing the existing report endpoint or the transactions endpoint with a large page size. No new REST endpoint, no new DTO, no controller change is required.

If a dedicated `unpaged` flag were added for semantic clarity (Approach B), a minor controller parameter and query object change would be needed — but this is optional.

### Client Layer

Primary change surface. Affected files:

| File | Change |
|---|---|
| `pages/booklet/[id].vue` | New `globalFilter` state, new load function, conditional paginator display |
| `components/booklet/BookletFilterActionBar.vue` | Two new button slots or props |
| `composables/useBooklet.ts` | Possibly reuse `findByIdMonthAndYear` (already exists) |

New state needed in the page:
- `globalFilter: ref<'none' | 'all' | 'preview'>` — distinguishes between paginated mode and global mode.
- `allTransactions: ref<DisplayTransaction[]>` — holds the full unpaged dataset when in global mode.
- The existing `actualTransactions` and pagination state remain untouched for normal mode.

`filteredTransactions` computed would branch: in global mode, apply the preview/all filter to `allTransactions`; in normal mode, keep current behavior.

### Cross-Cutting Concerns

**Performance:** Since the domain already loads all transactions into memory before paginating, there is no extra DB cost to fetching an unpaged response. The only difference is the HTTP payload size. For typical booklets (tens to low hundreds of transactions per month), this is negligible. For pathological cases (thousands of monthly entries), it could be slow — but this is a pre-existing architectural choice, not a regression.

**Security:** No new surface. Same auth, same booklet ownership check, same endpoint.

**Backward compatibility:** Additive change only. Existing paginated behavior is unchanged.

**Testability:** The new state branches are easily unit-tested in Vitest. The global load path reuses the same composable functions already covered by existing specs.

---

## 4. Solution Approaches

### Approach A — Frontend-only: Reuse the report endpoint

| Attribute | Content |
|---|---|
| **Name** | Frontend global mode via report endpoint |
| **Summary** | When a global filter button is activated, call the existing `findByIdMonthAndYear()` (report endpoint) instead of the paginated one. Store the full result in `allTransactions`. `filteredTransactions` switches its source based on `globalFilter`. The paginator is hidden while in global mode. |
| **Layers touched** | Client only |
| **Pros** | Zero backend changes. The report endpoint is already tested, already used elsewhere, already returns all transactions. Reuses `findByIdMonthAndYear` from `useBooklet.ts` which already exists. Clean semantic separation: "paginated browsing" vs "full report view". |
| **Cons / Risks** | The report endpoint response (`BookletReport`) does not include `hasRegenerableTransactions`. This is not a problem if the regenerate state is already loaded from the last paginated call and kept. Minor: the UX must clearly communicate that pagination is disabled in global mode (counts, paginator visibility). |
| **Fit for this project** | Excellent. No new code on the backend side. Stays within the hexagonal contract already in place. |

---

### Approach B — Backend: Add explicit `unpaged` support

| Attribute | Content |
|---|---|
| **Name** | Backend `unpaged` flag on the transactions endpoint |
| **Summary** | Add an optional `unpaged=true` query param to `GET /api/booklet/{bookletID}/transactions`. When set, the controller passes `pageSize = Int.MAX_VALUE` and `pageNumber = 0`. Optionally, a `typeFilter=preview` param allows server-side filtering of the type before returning. The frontend calls the same endpoint with these params. |
| **Layers touched** | Application (controller, query object), Client (composable call) |
| **Pros** | Explicit, self-documenting API contract. Avoids magic `Int.MAX_VALUE` numbers in the frontend. If `typeFilter` is added, the HTTP payload is reduced for the "preview only" case (fewer bytes transferred). |
| **Cons / Risks** | Backend changes for marginal gain. Since the domain already loads all data into memory before paginating, server-side type filtering would only reduce payload size, not DB load. The added surface requires new controller tests. Over-engineers what is effectively a display concern. |
| **Fit for this project** | Acceptable but unnecessary given the domain's existing in-memory model. Adds backend work with no DB-level benefit. |

---

### Approach C — Frontend-only: Large page size on the transactions endpoint

| Attribute | Content |
|---|---|
| **Name** | Frontend global mode with `size=MAX_SAFE_INTEGER` |
| **Summary** | Same as Approach A, but instead of calling the report endpoint, call `findTransactionsByIdMonthAndYear(..., 0, Number.MAX_SAFE_INTEGER)`. The transactions endpoint returns all items, the frontend disables pagination. |
| **Layers touched** | Client only |
| **Pros** | Single endpoint call path. `hasRegenerableTransactions` is preserved in the response (unlike Approach A). Slightly simpler code path — one composable function instead of two. |
| **Cons / Risks** | `Number.MAX_SAFE_INTEGER` as a page size is semantically odd and could behave unexpectedly if the backend ever adds an upper bound validation. The paginator will show "page 1 of 1" with totalElements = N, which is harmless but needs to be hidden for a clean UX. If the backend later switches to real DB-level pagination (e.g., Spring Data Pageable), this would break. |
| **Fit for this project** | Works today, but is less robust than Approach A if the backend pagination architecture ever changes. |

---

## 5. Recommended Approach

**Approach A — Frontend global mode via report endpoint**, with one small tweak: preserve `hasRegenerableTransactions` from the most recent paginated load (already in `ref<boolean>`) so the report-endpoint mode has no missing data.

**Justification:**

- Zero backend work. The feature is a display concern — the data contract already exists.
- The report endpoint is semantically aligned: "give me everything for this period" is exactly what a report does.
- The `findByIdMonthAndYear()` function already exists in `useBooklet.ts` — no new composable code needed.
- The domain's in-memory pagination means there is no performance regression relative to the current paginated path.
- The implementation stays on the client layer, which is the correct layer for a view-mode toggle.

**Concrete UX change:** Add two buttons (or extend `BookletFilterActionBar`) that set `globalFilter` to `'all'` or `'preview'`. When `globalFilter !== 'none'`, trigger a report fetch, store results in `allTransactions`, and hide the paginator. `filteredTransactions` returns `allTransactions` (possibly filtered to preview). Returning to paginated mode resets `globalFilter = 'none'` and restores `currentPage`/`pageSize` state.

**Key trade-off to accept:** In global mode, counts shown in the existing per-page filter buttons become meaningless (they show counts of all transactions, not a page subset). This is actually the desired behaviour — the global filter gives correct totals.

---

## 6. Open Questions

1. **UX placement:** Should the two global buttons replace the existing filter buttons when active, or sit alongside them as a separate "mode" toggle? A toggle group (paginated ↔ all ↔ all-preview) would be cleanest.
2. **Sorting in global mode:** Should the global view maintain the same date-descending sort as the paginated view, or offer additional sort options? (No backend implication either way — client-side sort on the full array.)
3. **Interaction with selection:** If the user has selected transactions in paginated mode and then switches to global mode, should the selection be preserved or cleared? Clearing is safer and simpler.

---

## 7. Next Steps

- Translate this into a feature issue (→ `/create-issue`) to define the exact UX behaviour and acceptance criteria.
- Implementation is purely frontend: `pages/booklet/[id].vue`, `BookletFilterActionBar.vue`, and no other files need changing outside of tests.
