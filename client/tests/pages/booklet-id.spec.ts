import { shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import BookletDetailsPage from '../../pages/booklet/[id].vue'

const deleteTransactionMock = vi.fn().mockResolvedValue({ deletedIds: [], excludedVirtualTransactions: [] })
const confirmVirtualTransactionMock = vi.fn().mockResolvedValue({
  id: 'new-tx-1',
  label: 'Confirmed',
  value: 100,
  isIncome: false,
  date: '2026-03-15',
  color: { red: 255, green: 255, blue: 255 },
  tagDTO: { tagId: 'default-tag', label: 'Aucune', colorDTO: { red: 255, green: 255, blue: 255 }, isDefault: true },
  isPreview: false,
  bookletAmount: '1400.00',
})
const confirmPreviewTransactionMock = vi.fn().mockResolvedValue({
  id: 'tx-1',
  label: 'Confirmed',
  value: 100,
  isIncome: false,
  date: '2026-03-15',
  color: { red: 255, green: 255, blue: 255 },
  tagDTO: { tagId: 'default-tag', label: 'Aucune', colorDTO: { red: 255, green: 255, blue: 255 }, isDefault: true },
  isPreview: false,
  bookletAmount: '1400.00',
})
const saveTransactionMock = vi.fn()

vi.mock('~/composables/useTransaction', () => ({
  default: () => ({
    deleteTransaction: deleteTransactionMock,
    confirmPreviewTransaction: confirmPreviewTransactionMock,
    confirmVirtualTransaction: confirmVirtualTransactionMock,
    saveTransaction: saveTransactionMock,
    editTransaction: vi.fn(),
    findTransactionById: vi.fn(),
  }),
}))

vi.mock('~/composables/useCsvImport', () => ({
  default: () => ({
    downloadCsvExport: vi.fn().mockResolvedValue(undefined),
  }),
}))

vi.mock('~/composables/useDate', () => ({
  default: () => ({
    months: ['JANUARY', 'FEBRUARY', 'MARCH'],
    englishMonth: (v: string) => v,
    translate: (v: string) => v,
    monthFromNumber: (n: number) => ['JANUARY', 'FEBRUARY', 'MARCH'][n - 1] || 'JANUARY',
    numberFromMonth: (m: string) => ({ JANUARY: 1, FEBRUARY: 2, MARCH: 3 }[m] ?? 1),
  }),
}))

vi.mock('primevue/useconfirm', () => ({
  useConfirm: () => ({ require: vi.fn() }),
}))

const defaultTag = {
  tagId: 'default-tag',
  label: 'Aucune',
  colorDTO: { red: 255, green: 255, blue: 255 },
  isDefault: true,
}

const findBalancesByIdMonthAndYearMock = vi.fn().mockResolvedValue({
  label: 'compte courant',
  realSold: '1500.00',
  previewSold: '1700.00',
})

const findTransactionsByIdMonthAndYearMock = vi.fn().mockResolvedValue({
  transactions: [],
  hasRegenerableTransactions: false,
  pageNumber: 0,
  pageSize: 10,
  totalElements: 0,
  totalPages: 1,
})

const findByIdMonthAndYearMock = vi.fn().mockResolvedValue({
  label: 'compte courant',
  transactions: [],
  realSold: '1500.00',
  previewSold: '1700.00',
})

function createTransaction(overrides: Partial<TransactionResultDTO> = {}): TransactionResultDTO {
  return {
    id: 'tx-1',
    label: 'Transaction',
    value: 10.00,
    isIncome: false,
    date: '2026-03-20',
    color: { red: 255, green: 255, blue: 255 },
    tagDTO: defaultTag,
    isPreview: false,
    bookletAmount: '1500.00',
    ...overrides,
  }
}

function flushPromises() {
  return new Promise<void>(resolve => queueMicrotask(resolve))
}

function mountPage(activeScopes: string[] = []) {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
  vi.stubGlobal('useJToast', () => ({
    success: vi.fn(),
    errorAxios: vi.fn(),
    warn: vi.fn(),
  }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: (scope: string) => activeScopes.includes(scope),
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))
  vi.stubGlobal('useBooklet', () => ({
    findBalancesByIdMonthAndYear: findBalancesByIdMonthAndYearMock,
    findTransactionsByIdMonthAndYear: findTransactionsByIdMonthAndYearMock,
    findByIdMonthAndYear: findByIdMonthAndYearMock,
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
  vi.stubGlobal('navigateTo', vi.fn())

  const wrapper = shallowMount(BookletDetailsPage, {
    global: {
      mocks: {
        useDate: () => ({
          months: ['JANUARY', 'FEBRUARY', 'MARCH'],
        }),
      },
      stubs: {
        ConfirmDialog: true,
        ProgressSpinner: { template: '<div class="spinner" />' },
        DataTable: { template: '<div><slot /><slot name="empty" /></div>' },
        Column: { template: '<div><slot :data="{}" /></div>' },
        TransactionCreationDialog: true,
        Dialog: { template: '<div><slot /></div>' },
        CsvImportDialog: true,
        Select: true,
        DatePicker: true,
        Tag: true,
        Checkbox: true,
        Paginator: true,
        Button: {
          props: ['label', 'disabled', 'loading'],
          template: '<button :data-label="label" :data-loading="String(loading)" :disabled="disabled"><slot /></button>',
        },
      },
    },
  })

  return { wrapper }
}

describe('pages/booklet/[id] loading states', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('shows export button loading state when csv export scope is active', () => {
    const { wrapper } = mountPage(['booklet.exportCsv'])

    const vm = wrapper.vm as any
    expect(vm.isExportCsvLoading).toBe(true)
    expect(vm.isAnyActionLoading).toBe(true)
  })

  it('queries account balances and transactions for calendar month', async () => {
    mountPage()

    await flushPromises()
    await flushPromises()

    expect(findBalancesByIdMonthAndYearMock).toHaveBeenCalledWith(
      'booklet-1',
      3,
      2026,
    )
    expect(findTransactionsByIdMonthAndYearMock).toHaveBeenCalledWith(
      'booklet-1',
      3,
      2026,
      {},
      0,
      10,
      'DESCENDING',
    )
  })
})

describe('pages/booklet/[id] date sorting', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('requests the most recent transactions first by default', async () => {
    mountPage()
    await flushPromises()
    await flushPromises()

    expect(findTransactionsByIdMonthAndYearMock).toHaveBeenCalledWith(
      'booklet-1',
      3,
      2026,
      {},
      0,
      10,
      'DESCENDING',
    )
  })

  it('reloads from the first page with the opposite direction when the date header is toggled', async () => {
    const { wrapper } = mountPage()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.currentPage = 3
    await nextTick()

    vm.toggleDateSort()
    await flushPromises()
    await flushPromises()

    expect(vm.sortDirection).toBe('ASCENDING')
    expect(vm.currentPage).toBe(0)
    expect(findTransactionsByIdMonthAndYearMock).toHaveBeenLastCalledWith(
      'booklet-1',
      3,
      2026,
      {},
      0,
      10,
      'ASCENDING',
    )
  })

  it('keeps the selected direction when navigating to another page', async () => {
    const { wrapper } = mountPage()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.toggleDateSort()
    await flushPromises()
    await flushPromises()

    vm.onPageChange({ page: 2 })
    await flushPromises()
    await flushPromises()

    expect(findTransactionsByIdMonthAndYearMock).toHaveBeenLastCalledWith(
      'booklet-1',
      3,
      2026,
      {},
      2,
      10,
      'ASCENDING',
    )
  })

  it('preserves the order returned by the backend instead of re-sorting the loaded page', async () => {
    findTransactionsByIdMonthAndYearMock.mockResolvedValueOnce({
      transactions: [
        createTransaction({ id: 'tx-1', label: 'Newest', date: '2026-03-20' }),
        createTransaction({ id: 'tx-2', label: 'Middle', date: '2026-03-10', isPreview: true }),
        createTransaction({ id: 'tx-3', label: 'Oldest', date: '2026-03-01' }),
      ],
      hasRegenerableTransactions: false,
      pageNumber: 0,
      pageSize: 10,
      totalElements: 25,
      totalPages: 3,
    })

    const { wrapper } = mountPage()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.filteredTransactions.map((t: any) => t.label)).toEqual(['Newest', 'Middle', 'Oldest'])
  })
})

describe('pages/booklet/[id] selection behavior', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('selects multiple preview transactions even when id is null', async () => {
    findTransactionsByIdMonthAndYearMock.mockResolvedValueOnce({
      transactions: [
        createTransaction({ id: null, label: 'Loyer', value: 800.00, isPreview: true, date: '2026-03-05' }),
        createTransaction({ id: null, label: 'Abonnement', value: 15.99, isPreview: true, date: '2026-03-10' }),
      ],
      hasRegenerableTransactions: true,
      pageNumber: 0,
      pageSize: 10,
      totalElements: 2,
      totalPages: 1,
    })

    const { wrapper } = mountPage()

    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const first = vm.filteredTransactions[0]
    const second = vm.filteredTransactions[1]

    vm.toggleSelection(first)
    vm.toggleSelection(second)

    expect(vm.selectedTransactions).toHaveLength(2)
    expect(vm.isSelected(first)).toBe(true)
    expect(vm.isSelected(second)).toBe(true)
  })

  it('selectedTransactionsAmountLabel shows negative total for expense transactions', async () => {
    findTransactionsByIdMonthAndYearMock.mockResolvedValueOnce({
      transactions: [
        createTransaction({ id: 'tx-1', label: 'Loyer', value: 800.00, isIncome: false }),
        createTransaction({ id: 'tx-2', label: 'Courses', value: 50.50, isIncome: false }),
      ],
      hasRegenerableTransactions: false,
      pageNumber: 0,
      pageSize: 10,
      totalElements: 2,
      totalPages: 1,
    })

    const { wrapper } = mountPage()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.toggleSelection(vm.filteredTransactions[0])
    vm.toggleSelection(vm.filteredTransactions[1])

    expect(vm.selectedTransactionsAmountLabel).toBe('-850,50 €')
  })

  it('selectedTransactionsAmountLabel shows positive total for income transactions', async () => {
    findTransactionsByIdMonthAndYearMock.mockResolvedValueOnce({
      transactions: [
        createTransaction({ id: 'tx-1', label: 'Salaire', value: 2000.00, isIncome: true }),
        createTransaction({ id: 'tx-2', label: 'Prime', value: 500.00, isIncome: true }),
      ],
      hasRegenerableTransactions: false,
      pageNumber: 0,
      pageSize: 10,
      totalElements: 2,
      totalPages: 1,
    })

    const { wrapper } = mountPage()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.toggleSelection(vm.filteredTransactions[0])
    vm.toggleSelection(vm.filteredTransactions[1])

    expect(vm.selectedTransactionsAmountLabel).toBe('+2\u202F500,00 €')
  })

  it('selectedTransactionsAmountLabel shows zero when no transactions are selected', async () => {
    findTransactionsByIdMonthAndYearMock.mockResolvedValueOnce({
      transactions: [
        createTransaction({ id: 'tx-1', label: 'Loyer', value: 800.00, isIncome: false }),
      ],
      hasRegenerableTransactions: false,
      pageNumber: 0,
      pageSize: 10,
      totalElements: 1,
      totalPages: 1,
    })

    const { wrapper } = mountPage()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any

    expect(vm.selectedTransactionsAmountLabel).toBe('0,00 €')
  })
})

describe('pages/booklet/[id] confirmDelete with virtual transactions', () => {
  let warnMock: ReturnType<typeof vi.fn>
  let successMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))

    deleteTransactionMock.mockResolvedValue({
      deletedIds: [],
      amount: '1500.00',
      excludedVirtualTransactions: [],
    })

    warnMock = vi.fn()
    successMock = vi.fn()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function mountWithDeleteMock(transactions: TransactionResultDTO[]) {
    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
    vi.stubGlobal('useJToast', () => ({
      success: successMock,
      errorAxios: vi.fn(),
      warn: warnMock,
    }))
    vi.stubGlobal('useLoading', () => ({
      isScopeLoading: () => false,
      withLoading: async <T>(action: () => Promise<T>) => action(),
    }))
    vi.stubGlobal('useBooklet', () => ({
      findBalancesByIdMonthAndYear: findBalancesByIdMonthAndYearMock,
      findTransactionsByIdMonthAndYear: findTransactionsByIdMonthAndYearMock,
      findByIdMonthAndYear: findByIdMonthAndYearMock,
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
    vi.stubGlobal('navigateTo', vi.fn())
    vi.stubGlobal('useTransaction', () => ({
      deleteTransaction: deleteTransactionMock,
      confirmPreviewTransaction: confirmPreviewTransactionMock,
      confirmVirtualTransaction: confirmVirtualTransactionMock,
      saveTransaction: saveTransactionMock,
      editTransaction: vi.fn(),
      findTransactionById: vi.fn(),
    }))

    findTransactionsByIdMonthAndYearMock.mockResolvedValue({
      transactions,
      hasRegenerableTransactions: false,
      pageNumber: 0,
      pageSize: 10,
      totalElements: transactions.length,
      totalPages: 1,
    })

    const wrapper = shallowMount(BookletDetailsPage, {
      global: {
        mocks: {
          useDate: () => ({
            months: ['JANUARY', 'FEBRUARY', 'MARCH'],
          }),
        },
        stubs: {
          ConfirmDialog: true,
          ProgressSpinner: { template: '<div class="spinner" />' },
          DataTable: { template: '<div><slot /><slot name="empty" /></div>' },
          Column: { template: '<div><slot :data="{}" /></div>' },
          TransactionCreationDialog: true,
          Dialog: { template: '<div><slot /></div>' },
          CsvImportDialog: true,
          Select: true,
          DatePicker: true,
          Tag: true,
          Checkbox: true,
          Paginator: true,
          Button: {
            props: ['label', 'disabled', 'loading'],
            template: '<button :data-label="label" :data-loading="String(loading)" :disabled="disabled"><slot /></button>',
          },
        },
      },
    })

    return wrapper
  }

  it('sends virtual descriptors when deleting only virtual transactions', async () => {
    const virtualTx = createTransaction({
      id: null as unknown as string,
      label: 'Loyer virtuel',
      value: 800.00,
      isPreview: true,
      date: '2026-03-15',
      regularTransactionId: 'reg-1',
    })

    deleteTransactionMock.mockResolvedValue({
      deletedIds: [],
      amount: '1500.00',
      excludedVirtualTransactions: ['2026-03'],
    })

    const wrapper = mountWithDeleteMock([virtualTx])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const displayed = vm.filteredTransactions[0]
    vm.toggleSelection(displayed)

    await vm.confirmDelete()
    await flushPromises()

    expect(deleteTransactionMock).toHaveBeenCalledWith(
      'booklet-1',
      [],
      [{ regularTransactionId: 'reg-1', month: 3, year: 2026 }],
    )
    expect(successMock).toHaveBeenCalled()
  })

  it('sends both physical ids and virtual descriptors for mixed selection', async () => {
    const physicalTx = createTransaction({ id: 'tx-1', label: 'Courses' })
    const virtualTx = createTransaction({
      id: null as unknown as string,
      label: 'Loyer virtuel',
      value: 800.00,
      isPreview: true,
      date: '2026-03-15',
      regularTransactionId: 'reg-1',
    })

    deleteTransactionMock.mockResolvedValue({
      deletedIds: ['tx-1'],
      amount: '700.00',
      excludedVirtualTransactions: ['2026-03'],
    })

    const wrapper = mountWithDeleteMock([physicalTx, virtualTx])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.toggleSelection(vm.filteredTransactions[0])
    vm.toggleSelection(vm.filteredTransactions[1])

    await vm.confirmDelete()
    await flushPromises()

    expect(deleteTransactionMock).toHaveBeenCalledWith(
      'booklet-1',
      expect.arrayContaining(['tx-1']),
      expect.arrayContaining([{ regularTransactionId: 'reg-1', month: 3, year: 2026 }]),
    )
    expect(successMock).toHaveBeenCalled()
  })

  it('shows warning and skips virtual transactions without regularTransactionId', async () => {
    const virtualTxNoReg = createTransaction({
      id: null as unknown as string,
      label: 'Orphan virtual',
      value: 50.00,
      isPreview: true,
      date: '2026-03-10',
      regularTransactionId: null,
    })
    const physicalTx = createTransaction({ id: 'tx-2', label: 'Essence' })

    deleteTransactionMock.mockResolvedValue({
      deletedIds: ['tx-2'],
      amount: '1450.00',
      excludedVirtualTransactions: [],
    })

    const wrapper = mountWithDeleteMock([physicalTx, virtualTxNoReg])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.toggleSelection(vm.filteredTransactions[0])
    vm.toggleSelection(vm.filteredTransactions[1])

    await vm.confirmDelete()
    await flushPromises()

    expect(warnMock).toHaveBeenCalledWith(
      expect.stringContaining('identifiant récurrent manquant'),
    )
    expect(deleteTransactionMock).toHaveBeenCalledWith(
      'booklet-1',
      ['tx-2'],
      [],
    )
    expect(successMock).toHaveBeenCalled()
  })

  it('shows warning when selection contains only unsuppressible virtual transactions', async () => {
    const virtualTxNoReg = createTransaction({
      id: null as unknown as string,
      label: 'Orphan virtual',
      value: 50.00,
      isPreview: true,
      date: '2026-03-10',
      regularTransactionId: null,
    })

    const wrapper = mountWithDeleteMock([virtualTxNoReg])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.toggleSelection(vm.filteredTransactions[0])

    await vm.confirmDelete()
    await flushPromises()

    expect(warnMock).toHaveBeenCalledWith(
      'La sélection contient uniquement des transactions virtuelles non supprimables.',
    )
    expect(deleteTransactionMock).not.toHaveBeenCalled()
  })
})

describe('pages/booklet/[id] confirmPreview with virtual transactions', () => {
  let successMock: ReturnType<typeof vi.fn>

  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function mountWithConfirmMock(transactions: TransactionResultDTO[]) {
    successMock = vi.fn()

    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
    vi.stubGlobal('useJToast', () => ({
      success: successMock,
      errorAxios: vi.fn(),
      warn: vi.fn(),
    }))
    vi.stubGlobal('useLoading', () => ({
      isScopeLoading: () => false,
      withLoading: async <T>(action: () => Promise<T>) => action(),
    }))
    vi.stubGlobal('useBooklet', () => ({
      findBalancesByIdMonthAndYear: findBalancesByIdMonthAndYearMock,
      findTransactionsByIdMonthAndYear: findTransactionsByIdMonthAndYearMock,
      findByIdMonthAndYear: findByIdMonthAndYearMock,
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
    vi.stubGlobal('navigateTo', vi.fn())
    vi.stubGlobal('useTransaction', () => ({
      deleteTransaction: deleteTransactionMock,
      confirmPreviewTransaction: confirmPreviewTransactionMock,
      confirmVirtualTransaction: confirmVirtualTransactionMock,
      saveTransaction: saveTransactionMock,
      editTransaction: vi.fn(),
      findTransactionById: vi.fn(),
    }))

    findTransactionsByIdMonthAndYearMock.mockResolvedValue({
      transactions,
      hasRegenerableTransactions: false,
      pageNumber: 0,
      pageSize: 10,
      totalElements: transactions.length,
      totalPages: 1,
    })

    const wrapper = shallowMount(BookletDetailsPage, {
      global: {
        mocks: {
          useDate: () => ({
            months: ['JANUARY', 'FEBRUARY', 'MARCH'],
          }),
        },
        stubs: {
          ConfirmDialog: true,
          ProgressSpinner: { template: '<div class="spinner" />' },
          DataTable: { template: '<div><slot /><slot name="empty" /></div>' },
          Column: { template: '<div><slot :data="{}" /></div>' },
          TransactionCreationDialog: true,
          Dialog: { template: '<div><slot /></div>' },
          CsvImportDialog: true,
          Select: true,
          DatePicker: true,
          Tag: true,
          Checkbox: true,
          Paginator: true,
          Button: {
            props: ['label', 'disabled', 'loading'],
            template: '<button :data-label="label" :data-loading="String(loading)" :disabled="disabled"><slot /></button>',
          },
        },
      },
    })

    return wrapper
  }

  it('calls confirmVirtualTransaction for virtual transaction without id', async () => {
    const virtualTx = createTransaction({
      id: null as unknown as string,
      label: 'Loyer virtuel',
      value: 800.00,
      isPreview: true,
      date: '2026-03-15',
      regularTransactionId: 'reg-1',
      tagDTO: { tagId: 'tag-1', label: 'Logement', colorDTO: { red: 100, green: 200, blue: 50 }, isDefault: false },
    })

    const wrapper = mountWithConfirmMock([virtualTx])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const displayed = vm.filteredTransactions[0]
    vm.onConfirmPreview(displayed)

    await vm.confirmPreview()
    await flushPromises()

    expect(confirmVirtualTransactionMock).toHaveBeenCalledWith(
      'booklet-1',
      'reg-1',
      3,
      2026,
      'Loyer virtuel',
      800.00,
      expect.any(Date),
      false,
      'tag-1',
      false,
    )
    expect(saveTransactionMock).not.toHaveBeenCalled()
    expect(confirmPreviewTransactionMock).not.toHaveBeenCalled()
    expect(successMock).toHaveBeenCalled()
  })

  it('calls confirmPreviewTransaction for persisted preview with id', async () => {
    const persistedPreview = createTransaction({
      id: 'tx-preview-1',
      label: 'Abonnement',
      value: 15.99,
      isPreview: true,
      date: '2026-03-10',
    })

    const wrapper = mountWithConfirmMock([persistedPreview])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const displayed = vm.filteredTransactions[0]
    vm.onConfirmPreview(displayed)

    await vm.confirmPreview()
    await flushPromises()

    expect(confirmPreviewTransactionMock).toHaveBeenCalledWith(
      'booklet-1',
      'tx-preview-1',
      null,
      expect.any(Date),
    )
    expect(confirmVirtualTransactionMock).not.toHaveBeenCalled()
    expect(saveTransactionMock).not.toHaveBeenCalled()
    expect(successMock).toHaveBeenCalled()
  })

  it('passes sourceMonth and sourceYear from the current booklet view', async () => {
    const virtualTx = createTransaction({
      id: null as unknown as string,
      label: 'Loyer virtuel',
      value: 500.00,
      isPreview: true,
      date: '2026-03-20',
      regularTransactionId: 'reg-2',
    })

    const wrapper = mountWithConfirmMock([virtualTx])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const displayed = vm.filteredTransactions[0]
    vm.onConfirmPreview(displayed)

    await vm.confirmPreview()
    await flushPromises()

    expect(confirmVirtualTransactionMock).toHaveBeenCalledWith(
      expect.anything(),
      'reg-2',
      3,
      2026,
      expect.anything(),
      expect.anything(),
      expect.anything(),
      expect.anything(),
      'default-tag',
      true,
    )
  })
})

describe('pages/booklet/[id] resolveDisplayTag', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const parentTag = { tagId: 'parent-1', label: 'Food', colorDTO: { red: 0, green: 200, blue: 0 }, isDefault: false }
  const subTag = { tagId: 'sub-1', label: 'Restaurants', colorDTO: { red: 200, green: 0, blue: 0 }, isDefault: false, parentId: 'parent-1' }

  function mountWithTags(tagsToLoad: any[]) {
    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
    vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn() }))
    vi.stubGlobal('useLoading', () => ({
      isScopeLoading: () => false,
      withLoading: async <T>(action: () => Promise<T>) => action(),
    }))
    vi.stubGlobal('useBooklet', () => ({
      findBalancesByIdMonthAndYear: findBalancesByIdMonthAndYearMock,
      findTransactionsByIdMonthAndYear: findTransactionsByIdMonthAndYearMock,
      findByIdMonthAndYear: findByIdMonthAndYearMock,
    }))
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue(tagsToLoad),
      getDefaultTag: vi.fn().mockResolvedValue(defaultTag),
    }))
    vi.stubGlobal('useDate', () => ({
      months: ['JANUARY', 'FEBRUARY', 'MARCH'],
      englishMonth: (v: string) => v,
      translate: (v: string) => v,
      monthFromNumber: (n: number) => ['JANUARY', 'FEBRUARY', 'MARCH'][n - 1] || 'JANUARY',
      numberFromMonth: (m: string) => ({ JANUARY: 1, FEBRUARY: 2, MARCH: 3 }[m] ?? 1),
    }))
    vi.stubGlobal('navigateTo', vi.fn())

    return shallowMount(BookletDetailsPage, {
      global: {
        mocks: {
          useDate: () => ({ months: ['JANUARY', 'FEBRUARY', 'MARCH'] }),
        },
        stubs: {
          ConfirmDialog: true,
          ProgressSpinner: true,
          AppTable: true,
          TransactionCreationDialog: true,
          Dialog: true,
          CsvImportDialog: true,
          Select: true,
          DatePicker: true,
          Tag: true,
          Checkbox: true,
          Paginator: true,
          Button: true,
        },
      },
    })
  }

  it('resolveDisplayTag returns the tag itself when it has no parentId', async () => {
    const wrapper = mountWithTags([parentTag, subTag])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const result = vm.resolveDisplayTag(parentTag)

    expect(result?.label).toBe('Food')
    expect(result?.tagId).toBe('parent-1')
  })

  it('resolveDisplayTag returns the parent tag when the tag has a parentId', async () => {
    const wrapper = mountWithTags([parentTag, subTag])
    await flushPromises()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const result = vm.resolveDisplayTag(subTag)

    expect(result?.label).toBe('Food')
    expect(result?.tagId).toBe('parent-1')
  })

  it('resolveDisplayTag falls back to the tag itself when parent is not found in loaded tags', async () => {
    const orphanSubTag = { tagId: 'orphan-sub', label: 'Orphan', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false, parentId: 'missing-parent' }
    const wrapper = mountWithTags([orphanSubTag])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const result = vm.resolveDisplayTag(orphanSubTag)

    expect(result?.label).toBe('Orphan')
    expect(result?.tagId).toBe('orphan-sub')
  })

  it('resolveDisplayTag returns null when tagDTO is null', async () => {
    const wrapper = mountWithTags([parentTag])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const result = vm.resolveDisplayTag(null)

    expect(result).toBeNull()
  })
})

describe('pages/booklet/[id] filteredTransactions tag filtering', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const parentTag = { tagId: 'parent-1', label: 'Food', colorDTO: { red: 0, green: 200, blue: 0 }, isDefault: false }
  const subTag = { tagId: 'sub-1', label: 'Restaurants', colorDTO: { red: 200, green: 0, blue: 0 }, isDefault: false, parentId: 'parent-1' }

  function mountWithTagsAndTransactions(transactions: TransactionResultDTO[]) {
    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
    vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn() }))
    vi.stubGlobal('useLoading', () => ({
      isScopeLoading: () => false,
      withLoading: async <T>(action: () => Promise<T>) => action(),
    }))
    vi.stubGlobal('useBooklet', () => ({
      findBalancesByIdMonthAndYear: findBalancesByIdMonthAndYearMock,
      findTransactionsByIdMonthAndYear: vi.fn().mockResolvedValue({
        transactions,
        hasRegenerableTransactions: false,
        pageNumber: 0,
        pageSize: 10,
        totalElements: transactions.length,
        totalPages: 1,
      }),
      findByIdMonthAndYear: findByIdMonthAndYearMock,
    }))
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue([parentTag, subTag]),
      getDefaultTag: vi.fn().mockResolvedValue(defaultTag),
    }))
    vi.stubGlobal('useDate', () => ({
      months: ['JANUARY', 'FEBRUARY', 'MARCH'],
      englishMonth: (v: string) => v,
      translate: (v: string) => v,
      monthFromNumber: (n: number) => ['JANUARY', 'FEBRUARY', 'MARCH'][n - 1] || 'JANUARY',
      numberFromMonth: (m: string) => ({ JANUARY: 1, FEBRUARY: 2, MARCH: 3 }[m] ?? 1),
    }))
    vi.stubGlobal('navigateTo', vi.fn())

    return shallowMount(BookletDetailsPage, {
      global: {
        mocks: {
          useDate: () => ({ months: ['JANUARY', 'FEBRUARY', 'MARCH'] }),
        },
        stubs: {
          ConfirmDialog: true,
          ProgressSpinner: true,
          AppTable: true,
          TransactionCreationDialog: true,
          Dialog: true,
          CsvImportDialog: true,
          Select: true,
          DatePicker: true,
          Tag: true,
          Checkbox: true,
          Paginator: true,
          Button: true,
        },
      },
    })
  }

  it('filtering by parent tag includes transactions of parent tag and its sub-tags', async () => {
    const txParent = createTransaction({ id: 'tx-1', tagDTO: parentTag })
    const txSub = createTransaction({ id: 'tx-2', tagDTO: subTag })
    const txOther = createTransaction({ id: 'tx-3', tagDTO: defaultTag })

    const wrapper = mountWithTagsAndTransactions([txParent, txSub, txOther])
    await flushPromises()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.selectedParentTagFilter = 'parent-1'

    const result = vm.filteredTransactions
    expect(result).toHaveLength(2)
    expect(result.map((t: any) => t.id)).toContain('tx-1')
    expect(result.map((t: any) => t.id)).toContain('tx-2')
  })

  it('tag column filter includes transactions of the tag and its sub-tags', async () => {
    const txParent = createTransaction({ id: 'tx-1', tagDTO: parentTag })
    const txSub = createTransaction({ id: 'tx-2', tagDTO: subTag })
    const txOther = createTransaction({ id: 'tx-3', tagDTO: defaultTag })

    const wrapper = mountWithTagsAndTransactions([txParent, txSub, txOther])
    await flushPromises()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.selectedTagFilter = 'parent-1'

    const result = vm.filteredTransactions
    expect(result).toHaveLength(2)
    expect(result.map((t: any) => t.id)).toContain('tx-1')
    expect(result.map((t: any) => t.id)).toContain('tx-2')
  })

  it('filtering by sub-tag includes only transactions of that specific sub-tag', async () => {
    const txParent = createTransaction({ id: 'tx-1', tagDTO: parentTag })
    const txSub = createTransaction({ id: 'tx-2', tagDTO: subTag })

    const wrapper = mountWithTagsAndTransactions([txParent, txSub])
    await flushPromises()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.selectedSubTagFilter = 'sub-1'

    const result = vm.filteredTransactions
    expect(result).toHaveLength(1)
    expect(result[0].id).toBe('tx-2')
  })

  it('setting tag filter clears the sub-tag filter', async () => {
    const wrapper = mountWithTagsAndTransactions([])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.selectedSubTagFilter = 'sub-1'
    await nextTick()
    vm.selectedTagFilter = 'parent-1'
    await nextTick()
    await nextTick()

    expect(vm.selectedSubTagFilter).toBe('')
  })

  it('setting sub-tag filter clears the tag filter', async () => {
    const wrapper = mountWithTagsAndTransactions([])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    vm.selectedTagFilter = 'parent-1'
    await nextTick()
    vm.selectedSubTagFilter = 'sub-1'
    await nextTick()
    await nextTick()

    expect(vm.selectedTagFilter).toBe('')
  })
})

describe('pages/booklet/[id] tagFilterOptions and subTagFilterOptions scoped to transactions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  const parentTagA = { tagId: 'parent-A', label: 'Alimentation', colorDTO: { red: 0, green: 200, blue: 0 }, isDefault: false }
  const subTagA = { tagId: 'sub-A', label: 'Courses', colorDTO: { red: 200, green: 100, blue: 0 }, isDefault: false, parentId: 'parent-A' }
  const parentTagB = { tagId: 'parent-B', label: 'Loisirs', colorDTO: { red: 0, green: 0, blue: 200 }, isDefault: false }
  const subTagB = { tagId: 'sub-B', label: 'Sport', colorDTO: { red: 100, green: 0, blue: 200 }, isDefault: false, parentId: 'parent-B' }

  function mountWithTagsAndTx(allTags: any[], transactions: TransactionResultDTO[]) {
    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
    vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn() }))
    vi.stubGlobal('useLoading', () => ({
      isScopeLoading: () => false,
      withLoading: async <T>(action: () => Promise<T>) => action(),
    }))
    vi.stubGlobal('useBooklet', () => ({
      findBalancesByIdMonthAndYear: findBalancesByIdMonthAndYearMock,
      findTransactionsByIdMonthAndYear: vi.fn().mockResolvedValue({
        transactions,
        hasRegenerableTransactions: false,
        pageNumber: 0,
        pageSize: 10,
        totalElements: transactions.length,
        totalPages: 1,
      }),
      findByIdMonthAndYear: findByIdMonthAndYearMock,
    }))
    vi.stubGlobal('useTag', () => ({
      getAllTags: vi.fn().mockResolvedValue(allTags),
      getDefaultTag: vi.fn().mockResolvedValue(defaultTag),
    }))
    vi.stubGlobal('useDate', () => ({
      months: ['JANUARY', 'FEBRUARY', 'MARCH'],
      englishMonth: (v: string) => v,
      translate: (v: string) => v,
      monthFromNumber: (n: number) => ['JANUARY', 'FEBRUARY', 'MARCH'][n - 1] || 'JANUARY',
      numberFromMonth: (m: string) => ({ JANUARY: 1, FEBRUARY: 2, MARCH: 3 }[m] ?? 1),
    }))
    vi.stubGlobal('navigateTo', vi.fn())

    return shallowMount(BookletDetailsPage, {
      global: {
        mocks: { useDate: () => ({ months: ['JANUARY', 'FEBRUARY', 'MARCH'] }) },
        stubs: {
          ConfirmDialog: true,
          ProgressSpinner: true,
          AppTable: true,
          TransactionCreationDialog: true,
          Dialog: true,
          CsvImportDialog: true,
          Select: true,
          DatePicker: true,
          Tag: true,
          Checkbox: true,
          Paginator: true,
          Button: true,
        },
      },
    })
  }

  it('tagFilterOptions only shows parent tags used directly in transactions', async () => {
    const txA = createTransaction({ id: 'tx-1', tagDTO: parentTagA })

    const wrapper = mountWithTagsAndTx([parentTagA, parentTagB], [txA])
    await flushPromises()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const options = vm.tagFilterOptions as { value: string }[]

    expect(options.some(o => o.value === 'parent-A')).toBe(true)
    expect(options.some(o => o.value === 'parent-B')).toBe(false)
  })

  it('tagFilterOptions shows parent tag when only a sub-tag of it is used in transactions', async () => {
    const txSub = createTransaction({ id: 'tx-1', tagDTO: subTagA })

    const wrapper = mountWithTagsAndTx([parentTagA, subTagA], [txSub])
    await flushPromises()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const options = vm.tagFilterOptions as { value: string }[]

    expect(options.some(o => o.value === 'parent-A')).toBe(true)
  })

  it('tagFilterOptions always includes the default "Tous les tags" option', async () => {
    const wrapper = mountWithTagsAndTx([parentTagA, parentTagB], [])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const options = vm.tagFilterOptions as { value: string, label: string }[]

    expect(options[0]?.label).toBe('Tous les tags')
    expect(options[0]?.value).toBe('')
  })

  it('subTagFilterOptions only shows sub-tags directly used in transactions', async () => {
    const txSub = createTransaction({ id: 'tx-1', tagDTO: subTagA })

    const wrapper = mountWithTagsAndTx([parentTagA, subTagA, parentTagB, subTagB], [txSub])
    await flushPromises()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const options = vm.subTagFilterOptions as { value: string }[]

    expect(options.some(o => o.value === 'sub-A')).toBe(true)
    expect(options.some(o => o.value === 'sub-B')).toBe(false)
  })

  it('subTagFilterOptions always includes the default "Tous les sous-tags" option', async () => {
    const wrapper = mountWithTagsAndTx([subTagA, subTagB], [])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    const options = vm.subTagFilterOptions as { value: string, label: string }[]

    expect(options[0]?.label).toBe('Tous les sous-tags')
    expect(options[0]?.value).toBe('')
  })

  it('tagFilterOptions and subTagFilterOptions both update when transactions change', async () => {
    const txParent = createTransaction({ id: 'tx-1', tagDTO: parentTagA })
    const txSub = createTransaction({ id: 'tx-2', tagDTO: subTagB })

    const wrapper = mountWithTagsAndTx([parentTagA, subTagA, parentTagB, subTagB], [txParent, txSub])
    await flushPromises()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any

    const parentOpts = vm.tagFilterOptions as { value: string }[]
    expect(parentOpts.some(o => o.value === 'parent-A')).toBe(true)
    // parentTagB appears because subTagB (child of parent-B) is used
    expect(parentOpts.some(o => o.value === 'parent-B')).toBe(true)

    const subOpts = vm.subTagFilterOptions as { value: string }[]
    expect(subOpts.some(o => o.value === 'sub-B')).toBe(true)
    // subTagA is not used in any transaction
    expect(subOpts.some(o => o.value === 'sub-A')).toBe(false)
  })
})

describe('pages/booklet/[id] global filter', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function mountForGlobal(reportTransactions: TransactionResultDTO[] = []) {
    findByIdMonthAndYearMock.mockResolvedValue({
      label: 'compte courant',
      transactions: reportTransactions,
      realSold: '1500.00',
      previewSold: '1700.00',
    })

    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
    vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn() }))
    vi.stubGlobal('useLoading', () => ({
      isScopeLoading: () => false,
      withLoading: async <T>(action: () => Promise<T>) => action(),
    }))
    vi.stubGlobal('useBooklet', () => ({
      findBalancesByIdMonthAndYear: findBalancesByIdMonthAndYearMock,
      findTransactionsByIdMonthAndYear: findTransactionsByIdMonthAndYearMock,
      findByIdMonthAndYear: findByIdMonthAndYearMock,
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
    vi.stubGlobal('navigateTo', vi.fn())

    return shallowMount(BookletDetailsPage, {
      global: {
        mocks: { useDate: () => ({ months: ['JANUARY', 'FEBRUARY', 'MARCH'] }) },
        stubs: {
          ConfirmDialog: true,
          AppTable: true,
          TransactionCreationDialog: true,
          Dialog: true,
          CsvImportDialog: true,
          Select: true,
          DatePicker: true,
          Tag: true,
          Checkbox: true,
          Paginator: true,
          Button: true,
        },
      },
    })
  }

  it('starts with globalFilter none and empty allTransactions', async () => {
    const wrapper = mountForGlobal()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.globalFilter).toBe('none')
    expect(vm.allTransactions).toHaveLength(0)
  })

  it('onGlobalFilterChange all loads report transactions and sets globalFilter to all', async () => {
    const tx = createTransaction({ id: 'global-1', label: 'Global tx' })
    const wrapper = mountForGlobal([tx])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.onGlobalFilterChange('all')
    await flushPromises()

    expect(vm.globalFilter).toBe('all')
    expect(findByIdMonthAndYearMock).toHaveBeenCalledWith('booklet-1', 3, 2026)
    expect(vm.allTransactions).toHaveLength(1)
    expect(vm.allTransactions[0].id).toBe('global-1')
  })

  it('onGlobalFilterChange preview sets globalFilter to preview', async () => {
    const preview = createTransaction({ id: 'prv-1', isPreview: true })
    const confirmed = createTransaction({ id: 'cnf-1', isPreview: false })
    const wrapper = mountForGlobal([preview, confirmed])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.onGlobalFilterChange('preview')
    await flushPromises()

    expect(vm.globalFilter).toBe('preview')
    expect(vm.allTransactions).toHaveLength(2)
  })

  it('filteredTransactions in global preview mode returns only previews', async () => {
    const preview = createTransaction({ id: 'prv-1', isPreview: true })
    const confirmed = createTransaction({ id: 'cnf-1', isPreview: false })
    const wrapper = mountForGlobal([preview, confirmed])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.onGlobalFilterChange('preview')
    await flushPromises()

    const result = vm.filteredTransactions as { id: string }[]
    expect(result).toHaveLength(1)
    expect(result[0].id).toBe('prv-1')
  })

  it('filteredTransactions in global all mode returns all transactions', async () => {
    const preview = createTransaction({ id: 'prv-1', isPreview: true })
    const confirmed = createTransaction({ id: 'cnf-1', isPreview: false })
    const wrapper = mountForGlobal([preview, confirmed])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.onGlobalFilterChange('all')
    await flushPromises()

    expect(vm.filteredTransactions).toHaveLength(2)
  })

  it('transactionsCount reflects global data when in global mode', async () => {
    const tx1 = createTransaction({ id: 'g-1' })
    const tx2 = createTransaction({ id: 'g-2' })
    findTransactionsByIdMonthAndYearMock.mockResolvedValue({
      transactions: [tx1],
      hasRegenerableTransactions: false,
      pageNumber: 0,
      pageSize: 10,
      totalElements: 1,
      totalPages: 1,
    })
    const wrapper = mountForGlobal([tx1, tx2])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    expect(vm.transactionsCount).toBe(1)

    await vm.onGlobalFilterChange('all')
    await flushPromises()

    expect(vm.transactionsCount).toBe(2)
  })

  it('onGlobalFilterChange none exits global mode and clears allTransactions', async () => {
    const tx = createTransaction({ id: 'g-1' })
    const wrapper = mountForGlobal([tx])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.onGlobalFilterChange('all')
    await flushPromises()
    expect(vm.globalFilter).toBe('all')

    await vm.onGlobalFilterChange('none')
    await flushPromises()

    expect(vm.globalFilter).toBe('none')
    expect(vm.allTransactions).toHaveLength(0)
  })

  it('switching between global all and global preview does not reload report', async () => {
    const wrapper = mountForGlobal([createTransaction({ id: 'g-1' })])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.onGlobalFilterChange('all')
    await flushPromises()
    const callCount = findByIdMonthAndYearMock.mock.calls.length

    await vm.onGlobalFilterChange('preview')
    await flushPromises()

    expect(findByIdMonthAndYearMock.mock.calls.length).toBe(callCount)
  })

  it('onMonthChange exits global mode', async () => {
    const wrapper = mountForGlobal([createTransaction({ id: 'g-1' })])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.onGlobalFilterChange('all')
    await flushPromises()
    expect(vm.globalFilter).toBe('all')

    vm.onMonthChange()
    await flushPromises()

    expect(vm.globalFilter).toBe('none')
    expect(vm.allTransactions).toHaveLength(0)
  })

  it('selecting in global mode is cleared when exiting global mode', async () => {
    const tx = createTransaction({ id: 'g-1' })
    const wrapper = mountForGlobal([tx])
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.onGlobalFilterChange('all')
    await flushPromises()

    vm.selectedTransactions = [vm.filteredTransactions[0]]
    expect(vm.selectedTransactions).toHaveLength(1)

    await vm.onGlobalFilterChange('none')
    expect(vm.selectedTransactions).toHaveLength(0)
  })
})

describe('pages/booklet/[id] selective regeneration', () => {
  const findRegenerableTransactionsMock = vi.fn()
  const regenerateMock = vi.fn()
  let successMock: ReturnType<typeof vi.fn>
  let errorAxiosMock: ReturnType<typeof vi.fn>

  const aCandidate = {
    regularTransactionId: 'rt-1',
    label: 'Loyer',
    value: '800.00',
    currency: '€',
    isIncome: false,
    date: '2026-03-05',
    tagDTO: null,
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))
    successMock = vi.fn()
    errorAxiosMock = vi.fn()
    findTransactionsByIdMonthAndYearMock.mockResolvedValue({
      transactions: [],
      hasRegenerableTransactions: true,
      pageNumber: 0,
      pageSize: 10,
      totalElements: 0,
      totalPages: 1,
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  function mountForRegeneration() {
    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
    vi.stubGlobal('useJToast', () => ({
      success: successMock,
      errorAxios: errorAxiosMock,
      warn: vi.fn(),
    }))
    vi.stubGlobal('useLoading', () => ({
      isScopeLoading: () => false,
      withLoading: async <T>(action: () => Promise<T>) => action(),
    }))
    vi.stubGlobal('useBooklet', () => ({
      findBalancesByIdMonthAndYear: findBalancesByIdMonthAndYearMock,
      findTransactionsByIdMonthAndYear: findTransactionsByIdMonthAndYearMock,
      findByIdMonthAndYear: findByIdMonthAndYearMock,
      findRegenerableTransactions: findRegenerableTransactionsMock,
      regenerateDeletedPrevisionalTransactions: regenerateMock,
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
    vi.stubGlobal('navigateTo', vi.fn())

    return shallowMount(BookletDetailsPage, {
      global: {
        stubs: {
          ConfirmDialog: true,
          ProgressSpinner: { template: '<div class="spinner" />' },
          DataTable: { template: '<div><slot /><slot name="empty" /></div>' },
          Column: { template: '<div><slot :data="{}" /></div>' },
          TransactionCreationDialog: true,
          Dialog: { template: '<div><slot /></div>' },
          CsvImportDialog: true,
          BookletRegenerateTransactionsDialog: true,
          Select: true,
          DatePicker: true,
          Tag: true,
          Checkbox: true,
          Paginator: true,
          Button: true,
        },
      },
    })
  }

  it('opens the dialog and loads the deleted occurrences', async () => {
    findRegenerableTransactionsMock.mockResolvedValue([aCandidate])
    const wrapper = mountForRegeneration()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.regenerate()
    await flushPromises()

    expect(findRegenerableTransactionsMock).toHaveBeenCalledWith('booklet-1', 3, 2026)
    expect(vm.isRegenerateDialogVisible).toBe(true)
    expect(vm.regenerableTransactions).toEqual([aCandidate])
    expect(regenerateMock).not.toHaveBeenCalled()
  })

  it('closes the dialog and reports the error when loading the candidates fails', async () => {
    findRegenerableTransactionsMock.mockRejectedValue(new Error('boom'))
    const wrapper = mountForRegeneration()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.regenerate()
    await flushPromises()

    expect(vm.isRegenerateDialogVisible).toBe(false)
    expect(errorAxiosMock).toHaveBeenCalled()
  })

  it('restores only the confirmed selection then closes and refreshes', async () => {
    findRegenerableTransactionsMock.mockResolvedValue([aCandidate])
    regenerateMock.mockResolvedValue({ transactions: [], type: 'PREVISIONAL' })
    const wrapper = mountForRegeneration()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.regenerate()
    await flushPromises()
    await vm.confirmRegeneration(['rt-1'])
    await flushPromises()

    expect(regenerateMock).toHaveBeenCalledWith('booklet-1', 3, 2026, ['rt-1'])
    expect(vm.isRegenerateDialogVisible).toBe(false)
    expect(successMock).toHaveBeenCalledWith('Transactions prévisionnelles régénérées avec succès')
  })

  it('reports the virtual restoration wording for a future month', async () => {
    findRegenerableTransactionsMock.mockResolvedValue([aCandidate])
    regenerateMock.mockResolvedValue({ transactions: [], type: 'VIRTUAL' })
    const wrapper = mountForRegeneration()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.confirmRegeneration(['rt-1'])
    await flushPromises()

    expect(successMock).toHaveBeenCalledWith('Transactions virtuelles restaurées avec succès')
  })

  it('keeps the dialog open when the regeneration fails', async () => {
    findRegenerableTransactionsMock.mockResolvedValue([aCandidate])
    regenerateMock.mockRejectedValue(new Error('boom'))
    const wrapper = mountForRegeneration()
    await flushPromises()
    await flushPromises()

    const vm = wrapper.vm as any
    await vm.regenerate()
    await flushPromises()
    await vm.confirmRegeneration(['rt-1'])
    await flushPromises()

    expect(vm.isRegenerateDialogVisible).toBe(true)
    expect(errorAxiosMock).toHaveBeenCalled()
    expect(successMock).not.toHaveBeenCalled()
  })
})
