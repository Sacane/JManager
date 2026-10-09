import { flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import BookletDetailsPage from '../../pages/booklet/[id].vue'

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: vi.fn() }),
}))

const defaultTag = { tagId: 'tag-1', label: 'Aucune', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: true }

const balancesMock = vi.fn()
const transactionsMock = vi.fn()
const reportMock = vi.fn()
const regenerableMock = vi.fn()

function resetMocks() {
  balancesMock.mockResolvedValue({ label: 'Livret A', realSold: '100.00', previewSold: '120.00' })
  transactionsMock.mockResolvedValue({
    transactions: [],
    totalElements: 0,
    totalPages: 0,
    hasRegenerableTransactions: true,
  })
  reportMock.mockResolvedValue({ transactions: [] })
  regenerableMock.mockResolvedValue([])
}

function mountPage() {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn(), error: vi.fn() }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: () => false,
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))
  vi.stubGlobal('useBooklet', () => ({
    findBalancesByIdMonthAndYear: balancesMock,
    findTransactionsByIdMonthAndYear: transactionsMock,
    findByIdMonthAndYear: reportMock,
    findRegenerableTransactions: regenerableMock,
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

  return shallowMount(BookletDetailsPage, {
    global: {
      stubs: {
        ConfirmDialog: true,
        ProgressSpinner: true,
        DataTable: { template: '<div><slot /></div>' },
        AppTable: true,
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
}

async function settle() {
  await flushPromises()
  await flushPromises()
  await nextTick()
}

/** Search argument of the last call for the transactions. */
function lastSearch() {
  return transactionsMock.mock.calls.at(-1)?.[8]
}

const logementTag = { tagId: 'tag-logement', label: 'Logement', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false }

function aTransaction(label: string, tag = defaultTag) {
  return { id: label, label, amount: '10.00', date: '2026-01-05', isIncome: false, isPreview: false, tagDTO: tag }
}

describe('pages/booklet/[id] search', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetMocks()
    vi.useFakeTimers()
  })

  afterEach(() => vi.useRealTimers())

  async function typeSearch(wrapper: ReturnType<typeof mountPage>, text: string) {
    ;(wrapper.vm as any).search = text
    await nextTick()
  }

  it('searches the displayed period once typing pauses, from the first page', async () => {
    const wrapper = mountPage()
    await settle()
    ;(wrapper.vm as any).currentPage = 2
    const callsBefore = transactionsMock.mock.calls.length

    await typeSearch(wrapper, 'l')
    await typeSearch(wrapper, 'lo')
    await typeSearch(wrapper, 'loyer')
    expect(transactionsMock.mock.calls.length).toBe(callsBefore)

    await vi.advanceTimersByTimeAsync(300)
    await settle()

    expect(transactionsMock.mock.calls.length).toBe(callsBefore + 1)
    expect(lastSearch()).toBe('loyer')
    expect(transactionsMock.mock.calls.at(-1)?.[4]).toBe(0)
  })

  it('shows the search as an active filter', async () => {
    const wrapper = mountPage()
    await settle()

    await typeSearch(wrapper, 'loyer')
    await vi.advanceTimersByTimeAsync(300)
    await settle()

    expect((wrapper.vm as any).activeFilters).toEqual([{ key: 'search', label: 'Recherche : « loyer »' }])
  })

  it('clears the search and the tag filters in one action', async () => {
    const wrapper = mountPage()
    await settle()
    const vm = wrapper.vm as any
    await typeSearch(wrapper, 'loyer')
    await vi.advanceTimersByTimeAsync(300)
    vm.selectedTagFilter = 'tag-1'
    await settle()

    await vm.clearAllFilters()
    await vi.advanceTimersByTimeAsync(300)
    await settle()

    expect(vm.search).toBe('')
    expect(vm.selectedTagFilter).toBe('')
    expect(vm.activeFilters).toEqual([])
    expect(lastSearch()).toBe('')
  })

  // "Tout le mois" already holds the whole period: it filters what it has instead of asking again.
  it('filters the whole period it holds when showing all of the month', async () => {
    reportMock.mockResolvedValue({ transactions: [aTransaction('Loyer janvier'), aTransaction('Courses')] })
    const wrapper = mountPage()
    await settle()
    const vm = wrapper.vm as any
    await vm.onGlobalFilterChange('all')
    await settle()
    const reportCalls = reportMock.mock.calls.length

    await typeSearch(wrapper, 'LOYER')
    await vi.advanceTimersByTimeAsync(300)
    await settle()

    expect(vm.filteredTransactions.map((t: { label: string }) => t.label)).toEqual(['Loyer janvier'])
    expect(reportMock.mock.calls.length).toBe(reportCalls)
  })

  it('tells a filtered empty list apart from an empty period', async () => {
    const wrapper = mountPage()
    await settle()
    const vm = wrapper.vm as any
    expect(vm.isFiltered).toBe(false)

    await typeSearch(wrapper, 'introuvable')
    await vi.advanceTimersByTimeAsync(300)
    await settle()

    expect(vm.isFiltered).toBe(true)
  })

  it('names the tag in its active filter', async () => {
    const wrapper = mountPage()
    await settle()
    const vm = wrapper.vm as any
    vm.tags = [defaultTag, logementTag]
    vm.selectedTagFilter = 'tag-logement'
    await settle()

    expect(vm.activeFilters).toEqual([{ key: 'tag', label: 'Tag : Logement' }])
  })
})
