import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import BookletDetailsPage from '../../pages/booklet/[id].vue'

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: vi.fn() }),
}))

const defaultTag = { tagId: 'tag-1', label: 'Aucune', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: true }

const MONTHS = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER']

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

function mountPage(query: Record<string, string> = {}) {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' }, query }))
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
    months: MONTHS,
    englishMonth: (v: string) => v,
    translate: (v: string) => v,
    monthFromNumber: (n: number) => MONTHS[n - 1],
    numberFromMonth: (m: string) => (MONTHS.includes(m) ? MONTHS.indexOf(m) + 1 : undefined),
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

/** Month and year of the first request for the transactions. */
function firstPeriod() {
  const call = transactionsMock.mock.calls[0]!
  return { month: call[1], year: call[2] }
}

// The global search opens a booklet at the month of the transaction found.
describe('pages/booklet/[id] opened at a given month', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetMocks()
  })

  it('opens at the month and year it is given', async () => {
    mountPage({ month: '2', year: '2025' })
    await settle()

    expect(firstPeriod()).toEqual({ month: 2, year: 2025 })
  })

  it('opens at the current month when none is given', async () => {
    mountPage()
    await settle()

    const today = new Date()
    expect(firstPeriod()).toEqual({ month: today.getMonth() + 1, year: today.getFullYear() })
  })

  it.each([
    [{ month: '13', year: '2025' }],
    [{ month: 'abc', year: '2025' }],
    [{ month: '2', year: 'next' }],
  ])('ignores a month it cannot read: %o', async (query) => {
    mountPage(query)
    await settle()

    const today = new Date()
    expect(firstPeriod()).toEqual({ month: today.getMonth() + 1, year: today.getFullYear() })
  })
})
