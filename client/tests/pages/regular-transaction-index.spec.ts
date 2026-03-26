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

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })
  return { promise, resolve, reject }
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
    bookletIds: ['booklet-1'],
  }
}

function mountPage(options?: {
  saveReject?: any
  deleteReject?: any
  bulkDeleteReject?: any
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
  const deleteRegularTransactions = options?.bulkDeleteReject
    ? vi.fn().mockRejectedValue(options.bulkDeleteReject)
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
    deleteRegularTransactions,
  }))
  vi.stubGlobal('useJToast', () => ({ success, errorAxios }))
  vi.stubGlobal('useConfirm', () => ({ require }))

  const wrapper = shallowMount(RegularTransactionPage, {
    global: {
      stubs: {
        DataTable: { template: '<div><slot /><slot name="empty" /></div>' },
        Column: { template: '<div><slot :data="{}" /></div>' },
        Button: { props: ['label'], template: '<button>{{ label }}<slot /></button>' },
        Tag: { template: '<span><slot /></span>' },
        ConfirmDialog: true,
        RegularTransactionCreationDialog: { name: 'RegularTransactionCreationDialog', props: ['loading'], template: '<div />' },
        RegularTransactionDialogCard: { name: 'RegularTransactionDialogCard', template: '<div />' },
      },
    },
  })

  return {
    wrapper,
    mocks: {
      saveMonthlyTransaction,
      updateRegularTransaction,
      deleteRegularTransaction,
      deleteRegularTransactions,
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

  it('toggles creation dialog loading while save is in progress', async () => {
    const deferred = createDeferred<RegularTransactionDTO>()
    const { wrapper, mocks } = mountPage()
    mocks.saveMonthlyTransaction.mockReturnValueOnce(deferred.promise)

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

    expect(wrapper.findComponent({ name: 'RegularTransactionCreationDialog' }).props('loading')).toBe(true)

    deferred.resolve(createRegularTransaction('rt-late'))
    await flushPromises()

    expect(wrapper.findComponent({ name: 'RegularTransactionCreationDialog' }).props('loading')).toBe(false)
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

  it('adds linked booklet count in delete confirmation message', async () => {
    const { wrapper, mocks } = mountPage()
    await flushPromises()

    wrapper.findComponent({ name: 'RegularTransactionDialogCard' }).vm.$emit('delete', 'rt-1')
    const confirmConfig = mocks.require.mock.calls[0]?.[0]

    expect(confirmConfig.message).toContain('liée à 1 livret(s)')
  })

  it('preserves booklet ids when submitting edit', async () => {
    const { wrapper, mocks } = mountPage()

    wrapper.findComponent({ name: 'RegularTransactionDialogCard' }).vm.$emit('save', createRegularTransaction('rt-1'))
    await flushPromises()

    expect(mocks.updateRegularTransaction).toHaveBeenCalledWith(
      expect.objectContaining({
        bookletIds: ['booklet-1'],
      }),
    )
    expect(mocks.success).toHaveBeenCalledWith('Transaction régulière mise à jour avec succès')
  })

  it('forwards changed booklet ids when editing a regular transaction', async () => {
    const { wrapper, mocks } = mountPage()

    const updated = createRegularTransaction('rt-1')
    updated.bookletIds = ['booklet-2']

    wrapper.findComponent({ name: 'RegularTransactionDialogCard' }).vm.$emit('save', updated)
    await flushPromises()

    expect(mocks.updateRegularTransaction).toHaveBeenCalledWith(
      expect.objectContaining({
        bookletIds: ['booklet-2'],
      }),
    )
  })

  it('calls bulk delete endpoint when bulk deletion is confirmed', async () => {
    const { wrapper, mocks } = mountPage()
    await flushPromises()

    wrapper.vm.selectedTransactions = [createRegularTransaction('rt-1')]
    await flushPromises()

    const bulkDeleteButton = wrapper.findAll('button').find(button => button.text().includes('Supprimer la sélection'))
    expect(bulkDeleteButton).toBeTruthy()

    await bulkDeleteButton!.trigger('click')
    const confirmConfig = mocks.require.mock.calls[0]?.[0]
    await confirmConfig.accept()

    expect(mocks.deleteRegularTransactions).toHaveBeenCalledWith({ transactionIds: ['rt-1'] })
    expect(mocks.success).toHaveBeenCalledWith('1 transaction(s) régulière(s) supprimée(s) avec succès')
  })

  it('calls error toast when bulk deletion fails', async () => {
    const bulkDeleteError = new Error('bulk delete failed')
    const { wrapper, mocks } = mountPage({ bulkDeleteReject: bulkDeleteError })
    await flushPromises()

    wrapper.vm.selectedTransactions = [createRegularTransaction('rt-1')]
    await flushPromises()

    const bulkDeleteButton = wrapper.findAll('button').find(button => button.text().includes('Supprimer la sélection'))
    expect(bulkDeleteButton).toBeTruthy()

    await bulkDeleteButton!.trigger('click')
    const confirmConfig = mocks.require.mock.calls[0]?.[0]
    await confirmConfig.accept()

    expect(mocks.errorAxios).toHaveBeenCalledWith(bulkDeleteError)
  })
})
