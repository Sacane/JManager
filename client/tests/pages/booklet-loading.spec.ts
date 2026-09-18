import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import BookletDetailsPage from '../../pages/booklet/[id].vue'

vi.mock('primevue/useconfirm', () => ({ useConfirm: () => ({ require: vi.fn() }) }))

const defaultTag = { tagId: 'tag-1', label: 'Aucune', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: true }

function deferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
}

/** Renders the table's loading and empty slots the way PrimeVue's DataTable would. */
const AppTableStub = {
  props: ['rows', 'loading'],
  template: `<div class="app-table">
    <div v-if="loading" class="table-loading"><slot name="loading" /></div>
    <div v-if="!rows || rows.length === 0" class="table-empty"><slot name="empty" /></div>
  </div>`,
}

function flushPromises() {
  return new Promise<void>(resolve => queueMicrotask(() => resolve()))
}

async function settle() {
  for (let i = 0; i < 6; i++) await flushPromises()
  await nextTick()
}

function mountPage(options: { viewportWidth?: number, transactions?: ReturnType<typeof deferred<any>> } = {}) {
  window.innerWidth = options.viewportWidth ?? 1280
  const transactions = options.transactions ?? deferred<any>()
  // One shared mock: `useBooklet()` builds a new object on every call, so a test changing the
  // implementation must reach the very function the page captured.
  const findTransactions = vi.fn(() => transactions.promise)
  // Reactive, so the page's `computed(() => isScopeLoading(...))` re-evaluates as it does in the app.
  const scopeLoading = ref(false)

  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn(), error: vi.fn() }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: () => scopeLoading.value,
    withLoading: async <T>(action: () => Promise<T>) => {
      scopeLoading.value = true
      try {
        return await action()
      } finally {
        scopeLoading.value = false
      }
    },
  }))
  vi.stubGlobal('useBooklet', () => ({
    findBalancesByIdMonthAndYear: vi.fn().mockResolvedValue({ label: 'Livret A', realSold: '100.00', previewSold: '120.00' }),
    findTransactionsByIdMonthAndYear: findTransactions,
    findByIdMonthAndYear: vi.fn().mockResolvedValue({ transactions: [] }),
    findRegenerableTransactions: vi.fn().mockResolvedValue([]),
    regenerateDeletedPrevisionalTransactions: vi.fn(),
  }))
  vi.stubGlobal('useTag', () => ({
    getAllTags: vi.fn().mockResolvedValue([defaultTag]),
    getDefaultTag: vi.fn().mockResolvedValue(defaultTag),
  }))
  vi.stubGlobal('useDate', () => ({
    months: ['JANUARY', 'FEBRUARY', 'MARCH'],
    englishMonth: (v: string) => v,
    translate: (v: string) => v,
    monthFromNumber: (n: number) => ['JANUARY', 'FEBRUARY', 'MARCH'][n - 1] || 'JANUARY',
    numberFromMonth: (m: string) => ({ JANUARY: 1, FEBRUARY: 2, MARCH: 3 }[m] ?? 1),
  }))
  vi.stubGlobal('useConfirm', () => ({ require: vi.fn() }))
  vi.stubGlobal('navigateTo', vi.fn())

  const wrapper = shallowMount(BookletDetailsPage, {
    global: {
      stubs: {
        AppTable: AppTableStub,
        ConfirmDialog: true,
        ProgressSpinner: true,
        Paginator: true,
        Select: true,
        DatePicker: true,
        Button: true,
        Checkbox: true,
        Tag: true,
        BookletPageHeader: true,
        BookletFilterActionBar: true,
        BookletActionButtons: true,
        BookletCsvMobileMenu: true,
        BookletConfirmPreviewDialog: true,
        BookletRegenerateTransactionsDialog: true,
        TransactionCreationDialog: true,
        CsvImportDialog: true,
      },
    },
  })

  return { wrapper, transactions, findTransactions }
}

const EMPTY_PAGE = { transactions: [], totalElements: 0, totalPages: 0, hasRegenerableTransactions: false }

describe('pages/booklet/[id] loading', () => {
  beforeEach(() => vi.clearAllMocks())

  // Before the first answer the table held no rows, so the empty message showed through the
  // loading overlay: the page announced an empty booklet while it was still loading it.
  it('shows the shape of the list, not the empty message, before the first load answers', async () => {
    const { wrapper } = mountPage()
    await settle()

    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('Commencez par créer votre première transaction')
  })

  it('shows the empty message once a load has answered with nothing', async () => {
    const { wrapper, transactions } = mountPage()
    transactions.resolve(EMPTY_PAGE)
    await settle()

    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Commencez par créer votre première transaction')
  })

  // A reload happens over content already on screen: replacing it with empty shapes would be a step
  // backwards, so the overlay keeps its spinner.
  it('keeps the spinner overlay for a reload after the first load', async () => {
    const { wrapper, transactions, findTransactions } = mountPage()
    transactions.resolve(EMPTY_PAGE)
    await settle()

    const reload = deferred<any>()
    findTransactions.mockImplementation(() => reload.promise)

    const pending = (wrapper.vm as any).loadBookletData()
    await nextTick()

    // The reload is genuinely in flight — otherwise the absence below would prove nothing.
    expect(wrapper.find('.table-loading').exists()).toBe(true)
    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(false)

    reload.resolve(EMPTY_PAGE)
    await pending
  })

  it('does not leave the skeleton up forever when the first load fails', async () => {
    const { wrapper, transactions } = mountPage()
    transactions.reject(new Error('network down'))
    await settle()

    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(false)
  })

  // On a phone there was no loading indicator at all: the empty state rendered immediately.
  it('shows the shape of the list on mobile before the first load answers', async () => {
    const { wrapper } = mountPage({ viewportWidth: 375 })
    await settle()

    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('Aucune transaction')
  })

  it('shows the empty state on mobile once a load has answered with nothing', async () => {
    const { wrapper, transactions } = mountPage({ viewportWidth: 375 })
    transactions.resolve(EMPTY_PAGE)
    await settle()

    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('Aucune transaction')
  })
})
