import { shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import BookletDetailsPage from '../../pages/booklet/[id].vue'

const deleteTransactionMock = vi.fn().mockResolvedValue({ deletedIds: [], excludedVirtualTransactions: [] })

vi.mock('~/composables/useTransaction', () => ({
  default: () => ({
    deleteTransaction: deleteTransactionMock,
    confirmPreviewTransaction: vi.fn(),
    saveTransaction: vi.fn(),
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

    const exportButton = wrapper.findAll('button').find(btn => btn.attributes('aria-label') === 'Exporter CSV')
    expect(exportButton).toBeDefined()
    expect(exportButton?.attributes('data-loading')).toBe('true')
    expect(exportButton?.attributes('disabled')).toBeDefined()
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
    )
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
