import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import RegularTransactionPage from '../../pages/regular-transaction/index.vue'

vi.mock('~/composables/useDate', () => ({
  default: () => ({
    frequencyToString: (value: string) => value,
  }),
}))

function flushPromises() {
  return new Promise(resolve => setTimeout(resolve, 0))
}

function createRegularTransaction(id: string) {
  return {
    id,
    label: 'Loyer',
    value: 900,
    isIncome: false,
    regularity: 'MONTHLY',
    startDate: '2026-03-01',
    frequencyProperty: { type: 'FOREVER', untilDate: undefined, times: undefined },
    tagDTO: {
      tagId: 'tag-1',
      label: 'Maison',
      colorDTO: { red: 100, green: 120, blue: 240 },
      isDefault: false,
    },
  }
}

function mountPage(options?: {
  saveReject?: any
  deleteReject?: any
}) {
  const fetch = vi.fn().mockResolvedValue([])
  const getRegularTransaction = vi.fn().mockResolvedValue([createRegularTransaction('rt-1')])
  const saveMonthlyTransaction = options?.saveReject
    ? vi.fn().mockRejectedValue(options.saveReject)
    : vi.fn().mockResolvedValue(createRegularTransaction('rt-new'))
  const getRegularTransactionById = vi.fn().mockResolvedValue(createRegularTransaction('rt-1'))
  const updateRegularTransaction = vi.fn().mockResolvedValue(createRegularTransaction('rt-1'))
  const deleteRegularTransaction = options?.deleteReject
    ? vi.fn().mockRejectedValue(options.deleteReject)
    : vi.fn().mockResolvedValue(undefined)

  const success = vi.fn()
  const errorAxios = vi.fn()
  const require = vi.fn()

  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useBooklet', () => ({ fetch }))
  vi.stubGlobal('useRegularTransaction', () => ({
    getRegularTransaction,
    saveMonthlyTransaction,
    getRegularTransactionById,
    updateRegularTransaction,
    deleteRegularTransaction,
  }))
  vi.stubGlobal('useJToast', () => ({ success, errorAxios }))
  vi.stubGlobal('useConfirm', () => ({ require }))

  const wrapper = shallowMount(RegularTransactionPage, {
    global: {
      stubs: {
        DataTable: { template: '<div><slot /><slot name="empty" /></div>' },
        Column: { template: '<div><slot :data="{}" /></div>' },
        Button: { template: '<button><slot /></button>' },
        Tag: { template: '<span><slot /></span>' },
        ConfirmDialog: true,
        RegularTransactionCreationDialog: { name: 'RegularTransactionCreationDialog', template: '<div />' },
        RegularTransactionDialogCard: { name: 'RegularTransactionDialogCard', template: '<div />' },
      },
    },
  })

  return {
    wrapper,
    mocks: {
      saveMonthlyTransaction,
      deleteRegularTransaction,
      success,
      errorAxios,
      require,
    },
  }
}

describe('pages/regular-transaction/index', () => {
  it('calls success toast when monthly transaction save succeeds', async () => {
    const { wrapper, mocks } = mountPage()
    const payload = {
      label: 'Netflix',
      value: 10,
      isIncome: false,
      startDate: '2026-03-13',
      tagDTO: { tagId: 'tag-1', label: 'Abonnement', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
      frequencyProperty: { type: 'FOREVER', untilDate: undefined, times: undefined },
      repeatDay: null,
      bookletIds: [],
    }

    wrapper.findComponent({ name: 'RegularTransactionCreationDialog' }).vm.$emit('create-transaction', payload)
    await flushPromises()

    expect(mocks.saveMonthlyTransaction).toHaveBeenCalledWith(payload)
    expect(mocks.success).toHaveBeenCalledWith('La transaction mensuel a bien été généré')
  })

  it('calls error toast when monthly transaction save fails', async () => {
    const saveError = new Error('save failed')
    const { wrapper, mocks } = mountPage({ saveReject: saveError })
    const payload = {
      label: 'Netflix',
      value: 10,
      isIncome: false,
      startDate: '2026-03-13',
      tagDTO: { tagId: 'tag-1', label: 'Abonnement', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
      frequencyProperty: { type: 'FOREVER', untilDate: undefined, times: undefined },
      repeatDay: null,
      bookletIds: [],
    }

    wrapper.findComponent({ name: 'RegularTransactionCreationDialog' }).vm.$emit('create-transaction', payload)
    await flushPromises()

    expect(mocks.errorAxios).toHaveBeenCalledWith(saveError)
  })

  it('calls success toast when delete is confirmed and succeeds', async () => {
    const { wrapper, mocks } = mountPage()

    wrapper.findComponent({ name: 'RegularTransactionDialogCard' }).vm.$emit('delete', 'rt-1')
    const confirmConfig = mocks.require.mock.calls[0]?.[0]
    await confirmConfig.accept()

    expect(mocks.deleteRegularTransaction).toHaveBeenCalledWith('rt-1')
    expect(mocks.success).toHaveBeenCalledWith('Transaction régulière supprimée avec succès')
  })

  it('calls error toast when delete is confirmed and fails', async () => {
    const deleteError = new Error('delete failed')
    const { wrapper, mocks } = mountPage({ deleteReject: deleteError })

    wrapper.findComponent({ name: 'RegularTransactionDialogCard' }).vm.$emit('delete', 'rt-1')
    const confirmConfig = mocks.require.mock.calls[0]?.[0]
    await confirmConfig.accept()

    expect(mocks.errorAxios).toHaveBeenCalledWith(deleteError)
  })
})
