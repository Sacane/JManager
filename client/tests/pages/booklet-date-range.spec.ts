import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
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

/** Date-range argument of the last call to a mock. */
function lastRangeOf(mock: ReturnType<typeof vi.fn>, index: number) {
  return mock.mock.calls.at(-1)?.[index]
}

describe('pages/booklet/[id] date range', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetMocks()
  })

  it('queries the calendar month when no range is set', async () => {
    const wrapper = mountPage()
    await settle()

    expect((wrapper.vm as any).hasCustomRange).toBe(false)
    expect(lastRangeOf(balancesMock, 3)).toEqual({})
    expect(lastRangeOf(transactionsMock, 3)).toEqual({})
  })

  // The point of the item: every figure on the page has to describe the same period, not just
  // the transaction list.
  it('sends the same range to the balances and to the transactions', async () => {
    const wrapper = mountPage()
    await settle()

    const vm = wrapper.vm as any
    vm.rangeStart = new Date(2026, 2, 5)
    vm.rangeEnd = new Date(2026, 3, 4)
    await vm.applyDateRange()
    await settle()

    const expected = { startDate: '2026-03-05', endDate: '2026-04-04' }
    expect(lastRangeOf(balancesMock, 3)).toEqual(expected)
    expect(lastRangeOf(transactionsMock, 3)).toEqual(expected)
  })

  it('sends the same range to the whole-period report', async () => {
    const wrapper = mountPage()
    await settle()

    const vm = wrapper.vm as any
    vm.rangeStart = new Date(2026, 2, 5)
    vm.rangeEnd = new Date(2026, 3, 4)
    await vm.applyDateRange()
    await settle()

    await vm.onGlobalFilterChange('all')
    await settle()

    expect(lastRangeOf(reportMock, 3)).toEqual({ startDate: '2026-03-05', endDate: '2026-04-04' })
  })

  it('returns to the monthly view when the range is cleared', async () => {
    const wrapper = mountPage()
    await settle()

    const vm = wrapper.vm as any
    vm.rangeStart = new Date(2026, 2, 5)
    vm.rangeEnd = new Date(2026, 3, 4)
    await vm.applyDateRange()
    await settle()

    await vm.clearDateRange()
    await settle()

    expect(vm.hasCustomRange).toBe(false)
    expect(lastRangeOf(balancesMock, 3)).toEqual({})
    expect(lastRangeOf(transactionsMock, 3)).toEqual({})
  })

  it('refuses a range whose end precedes its start, without querying', async () => {
    const wrapper = mountPage()
    await settle()

    const vm = wrapper.vm as any
    const callsBefore = transactionsMock.mock.calls.length
    vm.rangeStart = new Date(2026, 2, 10)
    vm.rangeEnd = new Date(2026, 2, 9)
    await vm.applyDateRange()
    await settle()

    expect(vm.dateRangeError).toBeTruthy()
    expect(vm.hasCustomRange).toBe(false)
    expect(transactionsMock.mock.calls.length).toBe(callsBefore)
  })

  it('clears the error once a valid range is applied', async () => {
    const wrapper = mountPage()
    await settle()

    const vm = wrapper.vm as any
    vm.rangeStart = new Date(2026, 2, 10)
    vm.rangeEnd = new Date(2026, 2, 9)
    await vm.applyDateRange()

    vm.rangeEnd = new Date(2026, 2, 20)
    await vm.applyDateRange()
    await settle()

    expect(vm.dateRangeError).toBeNull()
    expect(vm.hasCustomRange).toBe(true)
  })

  it('needs both boundaries before applying', async () => {
    const wrapper = mountPage()
    await settle()

    const vm = wrapper.vm as any
    vm.rangeStart = new Date(2026, 2, 5)
    vm.rangeEnd = null
    await vm.applyDateRange()

    expect(vm.hasCustomRange).toBe(false)
    expect(vm.dateRangeError).toBeTruthy()
  })

  // The regenerable endpoints take month and year only, so with a custom range the action would
  // work on a different period than the one on screen.
  it('disables regeneration while a custom range is active', async () => {
    const wrapper = mountPage()
    await settle()

    const vm = wrapper.vm as any
    expect(vm.hasRegenerableTransactions).toBe(true)

    vm.rangeStart = new Date(2026, 2, 5)
    vm.rangeEnd = new Date(2026, 3, 4)
    await vm.applyDateRange()
    await settle()

    expect(vm.hasRegenerableTransactions).toBe(false)
  })

  it('describes the active period rather than saying "the month"', async () => {
    const wrapper = mountPage()
    await settle()

    const vm = wrapper.vm as any
    expect(vm.periodLabel).toContain('mois')

    vm.rangeStart = new Date(2026, 2, 5)
    vm.rangeEnd = new Date(2026, 3, 4)
    await vm.applyDateRange()
    await settle()

    expect(vm.periodLabel).not.toContain('mois')
    expect(vm.periodLabel).toContain('05/03/2026')
    expect(vm.periodLabel).toContain('04/04/2026')
  })
})
