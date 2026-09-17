import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import BookletDetailsPage from '../../pages/booklet/[id].vue'
import RegularTransactionPage from '../../pages/regular-transaction/index.vue'

vi.mock('~/composables/useDate', () => ({
  default: () => ({ frequencyToString: (value: string) => value }),
}))

vi.mock('primevue/useconfirm', () => ({ useConfirm: () => ({ require: vi.fn() }) }))

function createRegularTransaction(id: string, label: string) {
  return {
    id,
    label,
    value: 900,
    isIncome: false,
    regularity: 'MONTHLY',
    startDate: '2026-03-01',
    frequencyProperty: { type: 'FOREVER', untilDate: undefined, times: undefined },
    tagDTO: { tagId: 'tag-1', label: 'Maison', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
    bookletIds: ['booklet-1'],
  }
}

const AppTableStub = {
  props: ['rows', 'columns', 'selection'],
  emits: ['rowDblclick'],
  template: `<div><div v-for="row in rows" :key="row.id" class="app-table-row"><slot name="body-actions" :data="row" /></div></div>`,
}

const ButtonStub = {
  props: ['label', 'icon', 'disabled'],
  emits: ['click'],
  template: `<button :disabled="disabled" @click="!disabled && $emit('click', $event)">{{ label }}<slot /></button>`,
}

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0))
}

async function settle() {
  await flushPromises()
  await nextTick()
}

function mountRegularTransactions(viewportWidth: number) {
  window.innerWidth = viewportWidth

  const getRegularTransactionById = vi.fn().mockResolvedValue(createRegularTransaction('rt-1', 'Loyer'))

  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useBooklet', () => ({ fetch: vi.fn().mockResolvedValue([]) }))
  vi.stubGlobal('useRegularTransaction', () => ({
    getRegularTransaction: vi.fn().mockResolvedValue({
      content: [createRegularTransaction('rt-1', 'Loyer'), createRegularTransaction('rt-2', 'Netflix')],
      pageNumber: 0,
      pageSize: 10,
      totalElements: 2,
      totalPages: 1,
    }),
    saveMonthlyTransaction: vi.fn(),
    getRegularTransactionById,
    updateRegularTransaction: vi.fn(),
    deleteRegularTransaction: vi.fn(),
    deleteRegularTransactions: vi.fn(),
    linkRegularTransactionToBooklet: vi.fn(),
    unlinkRegularTransactionFromBooklet: vi.fn(),
  }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn() }))
  vi.stubGlobal('useConfirm', () => ({ require: vi.fn() }))

  const wrapper = shallowMount(RegularTransactionPage, {
    global: {
      directives: { tooltip: {} },
      stubs: {
        AppTable: AppTableStub,
        Button: ButtonStub,
        Tag: { template: '<span><slot /></span>' },
        Select: true,
        ConfirmDialog: true,
        Paginator: true,
        RegularTransactionCreationDialog: { name: 'RegularTransactionCreationDialog', template: '<div />' },
        RegularTransactionDialogCard: { name: 'RegularTransactionDialogCard', template: '<div />' },
      },
    },
  })

  return { wrapper, getRegularTransactionById }
}

describe('pages/regular-transaction/index edit interaction', () => {
  beforeEach(() => vi.clearAllMocks())

  // Tapping a card used to open the editor, so a phone could never build a selection.
  it('selects the row instead of opening the editor when the card body is tapped', async () => {
    const { wrapper, getRegularTransactionById } = mountRegularTransactions(375)
    await settle()

    await wrapper.findAll('[data-test="rt-card"]')[0].trigger('click')
    await nextTick()

    expect(getRegularTransactionById).not.toHaveBeenCalled()
    expect((wrapper.vm as any).isEditDialogVisible).toBe(false)
    expect((wrapper.vm as any).selectedTransactions).toHaveLength(1)
  })

  it('deselects the row when the card body is tapped again', async () => {
    const { wrapper } = mountRegularTransactions(375)
    await settle()

    await wrapper.findAll('[data-test="rt-card"]')[0].trigger('click')
    await wrapper.findAll('[data-test="rt-card"]')[0].trigger('click')
    await nextTick()

    expect((wrapper.vm as any).selectedTransactions).toHaveLength(0)
  })

  it('keeps the double click shortcut on desktop', async () => {
    const { wrapper, getRegularTransactionById } = mountRegularTransactions(1280)
    await settle()

    wrapper.findComponent(AppTableStub).vm.$emit('rowDblclick', { data: createRegularTransaction('rt-1', 'Loyer') })
    await settle()

    expect(getRegularTransactionById).toHaveBeenCalledWith('rt-1')
  })

  // The handler was called handleRowDoubleClick while also serving a single mobile tap.
  it('names the open action after what it does, not after one of its triggers', () => {
    const { wrapper } = mountRegularTransactions(1280)

    expect(typeof (wrapper.vm as any).openEditDialog).toBe('function')
    expect((wrapper.vm as any).handleRowDoubleClick).toBeUndefined()
  })
})

// The issue claimed the booklet page offered no obvious way to open a transaction on mobile.
// Reading it showed an explicit edit control on both layouts and a tap that selects, so these
// pin the existing behaviour rather than change it.
describe('pages/booklet/[id] edit interaction (regression)', () => {
  const defaultTag = { tagId: 'tag-1', label: 'Aucune', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: true }

  function mountBooklet() {
    window.innerWidth = 375

    vi.stubGlobal('definePageMeta', vi.fn())
    vi.stubGlobal('useRoute', () => ({ params: { id: 'booklet-1' } }))
    vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn(), error: vi.fn() }))
    vi.stubGlobal('useLoading', () => ({
      isScopeLoading: () => false,
      withLoading: async <T>(action: () => Promise<T>) => action(),
    }))
    vi.stubGlobal('useBooklet', () => ({
      findBalancesByIdMonthAndYear: vi.fn().mockResolvedValue({ label: 'Livret A', realSold: '100.00', previewSold: '120.00' }),
      findTransactionsByIdMonthAndYear: vi.fn().mockResolvedValue({
        transactions: [{ id: 't-1', label: 'Courses', value: 20, isIncome: false, date: '2026-03-05', tagDTO: defaultTag }],
        totalElements: 1,
        totalPages: 1,
        hasRegenerableTransactions: false,
      }),
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

    return shallowMount(BookletDetailsPage, {
      global: {
        stubs: {
          ConfirmDialog: true,
          ProgressSpinner: true,
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

  beforeEach(() => vi.clearAllMocks())

  it('toggles the selection when a mobile row body is tapped', async () => {
    const wrapper = mountBooklet()
    await settle()

    const vm = wrapper.vm as any
    const transaction = vm.actualTransactions[0]

    vm.toggleSelection(transaction)
    expect(vm.isSelected(transaction)).toBe(true)

    vm.toggleSelection(transaction)
    expect(vm.isSelected(transaction)).toBe(false)
  })

  it('exposes an explicit open action rather than relying on a gesture', async () => {
    const wrapper = mountBooklet()
    await settle()

    expect(wrapper.html()).toContain('aria-label="Modifier"')
  })
})
