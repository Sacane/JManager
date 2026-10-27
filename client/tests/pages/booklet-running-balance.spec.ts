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

function aTransaction(label: string, balanceAfter: string | null, isPreview = false) {
  return { id: `id-${label}`, label, value: '10.00', date: '2026-01-05', isIncome: false, isPreview, tagDTO: defaultTag, balanceAfter }
}

// UX-25: the balance each transaction leaves, as on a bank statement.
describe('pages/booklet/[id] running balance', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetMocks()
    transactionsMock.mockResolvedValue({
      transactions: [aTransaction('Courses', '2940.00'), aTransaction('Loyer', null, true), aTransaction('Découvert', '-12.50')],
      totalElements: 3,
      totalPages: 1,
      hasRegenerableTransactions: false,
    })
  })

  it('shows a balance column while the list follows the dates', async () => {
    const wrapper = mountPage()
    await settle()

    const columns = (wrapper.vm as any).bookletTransactionColumns as Array<{ header?: string }>
    expect(columns.map(column => column.header)).toContain('Solde')
    expect(wrapper.find('[data-test="running-balance-hidden"]').exists()).toBe(false)
  })

  // A balance only follows from the lines before it in date order: any other order would make it read wrong.
  it('hides the balance and says why when the list is sorted otherwise', async () => {
    const wrapper = mountPage()
    await settle()
    const vm = wrapper.vm as any

    vm.activeSort = { field: 'LABEL', direction: 'ASCENDING' }
    await settle()

    expect((vm.bookletTransactionColumns as Array<{ header?: string }>).map(column => column.header)).not.toContain('Solde')
    expect(wrapper.find('[data-test="running-balance-hidden"]').text()).toContain('triée par date')
  })

  it('shows the balance under each confirmed line on a phone, and none under a previsional one', async () => {
    const wrapper = mountPage()
    await settle()
    const vm = wrapper.vm as any

    vm.isMobile = true
    await settle()

    const balances = wrapper.findAll('[data-test="mobile-running-balance"]').map(balance => balance.text())
    expect(balances).toEqual(['Solde : 2 940,00 €', 'Solde : -12,50 €'])
  })
})
