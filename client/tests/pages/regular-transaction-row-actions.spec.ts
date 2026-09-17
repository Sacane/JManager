import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import RegularTransactionPage from '../../pages/regular-transaction/index.vue'

vi.mock('~/composables/useDate', () => ({
  default: () => ({ frequencyToString: (value: string) => value }),
}))

function createRegularTransaction(id: string, label: string) {
  return {
    id,
    label,
    value: 900,
    isIncome: false,
    regularity: 'MONTHLY',
    startDate: '2026-03-01',
    frequencyProperty: { type: 'FOREVER', untilDate: undefined, times: undefined },
    tagDTO: { tagId: 'tag-1', label: 'Maison', colorDTO: { red: 100, green: 120, blue: 240 }, isDefault: false },
    bookletIds: ['booklet-1'],
  }
}

/** Renders the row-level slots a real table would render, so the actions are actually exercised. */
const AppTableStub = {
  props: ['rows', 'columns', 'selection'],
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

function mountPage(options?: { viewportWidth?: number }) {
  window.innerWidth = options?.viewportWidth ?? 1280

  const getRegularTransaction = vi.fn().mockResolvedValue({
    content: [createRegularTransaction('rt-1', 'Loyer'), createRegularTransaction('rt-2', 'Netflix')],
    pageNumber: 0,
    pageSize: 10,
    totalElements: 2,
    totalPages: 1,
  })
  const getRegularTransactionById = vi.fn().mockResolvedValue(createRegularTransaction('rt-1', 'Loyer'))
  const deleteRegularTransaction = vi.fn().mockResolvedValue(undefined)
  const deleteRegularTransactions = vi.fn().mockResolvedValue(undefined)
  const require = vi.fn()

  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useBooklet', () => ({ fetch: vi.fn().mockResolvedValue([]) }))
  vi.stubGlobal('useRegularTransaction', () => ({
    getRegularTransaction,
    saveMonthlyTransaction: vi.fn(),
    getRegularTransactionById,
    updateRegularTransaction: vi.fn(),
    deleteRegularTransaction,
    deleteRegularTransactions,
    linkRegularTransactionToBooklet: vi.fn(),
    unlinkRegularTransactionFromBooklet: vi.fn(),
  }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), errorAxios: vi.fn(), warn: vi.fn() }))
  vi.stubGlobal('useConfirm', () => ({ require }))

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

  return { wrapper, mocks: { getRegularTransactionById, deleteRegularTransaction, deleteRegularTransactions, require } }
}

async function settle() {
  await flushPromises()
  await nextTick()
}

describe('pages/regular-transaction/index row actions', () => {
  beforeEach(() => vi.clearAllMocks())

  it('shows an edit and a delete action on every desktop row', async () => {
    const { wrapper } = mountPage()
    await settle()

    expect(wrapper.findAll('[data-test="rt-edit"]')).toHaveLength(2)
    expect(wrapper.findAll('[data-test="rt-delete"]')).toHaveLength(2)
  })

  it('opens the editor from the row action rather than from a hidden gesture', async () => {
    const { wrapper, mocks } = mountPage()
    await settle()

    await wrapper.findAll('[data-test="rt-edit"]')[0].trigger('click')
    await settle()

    expect(mocks.getRegularTransactionById).toHaveBeenCalledWith('rt-1')
    expect((wrapper.vm as any).isEditDialogVisible).toBe(true)
  })

  // A confirmation that does not name what it destroys is not a confirmation.
  it('names the transaction in the deletion confirmation', async () => {
    const { wrapper, mocks } = mountPage()
    await settle()

    await wrapper.findAll('[data-test="rt-delete"]')[1].trigger('click')

    expect(mocks.require).toHaveBeenCalledTimes(1)
    expect(mocks.require.mock.calls[0][0].message).toContain('Netflix')
  })

  it('deletes only once the confirmation is accepted', async () => {
    const { wrapper, mocks } = mountPage()
    await settle()

    await wrapper.findAll('[data-test="rt-delete"]')[0].trigger('click')
    expect(mocks.deleteRegularTransaction).not.toHaveBeenCalled()

    await mocks.require.mock.calls[0][0].accept()
    await settle()

    expect(mocks.deleteRegularTransaction).toHaveBeenCalledWith('rt-1')
  })

  it('shows the same actions on a mobile card', async () => {
    const { wrapper } = mountPage({ viewportWidth: 375 })
    await settle()

    expect(wrapper.findAll('[data-test="rt-edit-mobile"]')).toHaveLength(2)
    expect(wrapper.findAll('[data-test="rt-delete-mobile"]')).toHaveLength(2)
  })

  // The bulk delete control was rendered behind `v-if="!isMobile"`, so a phone could select
  // nothing and delete nothing in bulk.
  it('offers bulk deletion on mobile once a selection exists', async () => {
    const { wrapper } = mountPage({ viewportWidth: 375 })
    await settle()

    expect(wrapper.find('[data-test="rt-bulk-delete"]').exists()).toBe(false)

    ;(wrapper.vm as any).selectedTransactions = [createRegularTransaction('rt-1', 'Loyer')]
    await nextTick()

    expect(wrapper.find('[data-test="rt-bulk-delete"]').exists()).toBe(true)
  })
})
