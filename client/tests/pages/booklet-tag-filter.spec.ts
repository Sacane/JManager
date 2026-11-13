import { flushPromises, shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import BookletDetailsPage from '../../pages/booklet/[id].vue'

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: vi.fn() }),
}))

const color = { red: 0, green: 0, blue: 0 }
const defaultTag = { tagId: 'tag-1', label: 'Aucune', colorDTO: color, isDefault: true }
const food = { tagId: 'tag-food', label: 'Alimentation', colorDTO: color, isDefault: false }
const restaurant = { tagId: 'tag-resto', label: 'Restaurant', colorDTO: color, isDefault: false, parentId: 'tag-food' }
const transport = { tagId: 'tag-transport', label: 'Transport', colorDTO: color, isDefault: false }

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
    getAllTags: vi.fn().mockResolvedValue([defaultTag, food, restaurant, transport]),
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

/** Tag argument of the last call for the transactions. */
function lastTagId() {
  return transactionsMock.mock.calls.at(-1)?.[9]
}

function aTransaction(label: string, tag: { tagId: string } = defaultTag) {
  return { id: label, label, amount: '10.00', date: '2026-01-05', isIncome: false, isPreview: false, tagDTO: tag }
}

const optionValues = (options: Array<{ value: string }>) => options.map(option => option.value).filter(Boolean)

// The list is paginated: a filter run in the browser only saw the page shown (and offered only its tags).
describe('pages/booklet/[id] tag filter', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    resetMocks()
  })

  afterEach(() => vi.useRealTimers())

  it('asks the server for the transactions of the picked tag, from the first page', async () => {
    const wrapper = mountPage()
    await settle()
    ;(wrapper.vm as any).currentPage = 2

    ;(wrapper.vm as any).selectedTagFilter = 'tag-food'
    await settle()

    expect(lastTagId()).toBe('tag-food')
    expect(transactionsMock.mock.calls.at(-1)?.[4]).toBe(0)
  })

  it('asks for the sub-tag instead once one is picked', async () => {
    const wrapper = mountPage()
    await settle()
    ;(wrapper.vm as any).selectedTagFilter = 'tag-food'
    await settle()

    ;(wrapper.vm as any).selectedSubTagFilter = 'tag-resto'
    await settle()

    expect(lastTagId()).toBe('tag-resto')
    expect((wrapper.vm as any).selectedTagFilter).toBe('')
  })

  it('asks for every tag again once the filter is cleared', async () => {
    const wrapper = mountPage()
    await settle()
    ;(wrapper.vm as any).selectedTagFilter = 'tag-food'
    await settle()

    ;(wrapper.vm as any).clearAllFilters()
    await settle()

    expect(lastTagId()).toBe('')
  })

  it('shows the page the server filtered as it is', async () => {
    transactionsMock.mockResolvedValue({
      transactions: [aTransaction('Courses', food), aTransaction('Pizzeria', restaurant)],
      totalElements: 2,
      totalPages: 1,
      hasRegenerableTransactions: false,
      tagIds: ['tag-food', 'tag-resto'],
    })
    const wrapper = mountPage()
    await settle()

    ;(wrapper.vm as any).selectedSubTagFilter = 'tag-resto'
    await settle()

    expect((wrapper.vm as any).filteredTransactions.map((t: { label: string }) => t.label)).toEqual(['Courses', 'Pizzeria'])
  })

  it('offers the tags used over the whole period, not only the ones of the page shown', async () => {
    transactionsMock.mockResolvedValue({
      transactions: [aTransaction('Courses', food)],
      totalElements: 30,
      totalPages: 3,
      hasRegenerableTransactions: false,
      tagIds: ['tag-food', 'tag-resto', 'tag-transport'],
    })
    const wrapper = mountPage()
    await settle()

    expect(optionValues((wrapper.vm as any).tagFilterOptions)).toEqual(['tag-food', 'tag-transport'])
    expect(optionValues((wrapper.vm as any).subTagFilterOptions)).toEqual(['tag-resto'])
  })

  // "Tout le mois" holds the whole period already: it filters it in the browser, and offers its tags.
  it('filters the whole period in the browser in the "Tout le mois" view', async () => {
    reportMock.mockResolvedValue({
      transactions: [aTransaction('Courses', food), aTransaction('Pizzeria', restaurant), aTransaction('Essence', transport)],
    })
    const wrapper = mountPage()
    await settle()
    await (wrapper.vm as any).onGlobalFilterChange('all')
    await settle()

    ;(wrapper.vm as any).selectedTagFilter = 'tag-food'
    await settle()

    expect((wrapper.vm as any).filteredTransactions.map((t: { label: string }) => t.label)).toEqual(['Courses', 'Pizzeria'])
    expect(optionValues((wrapper.vm as any).tagFilterOptions)).toEqual(['tag-food', 'tag-transport'])
  })
})
