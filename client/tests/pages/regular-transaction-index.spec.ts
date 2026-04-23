import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import RegularTransactionPage from '../../pages/regular-transaction/index.vue'

vi.mock('~/composables/useDate', () => ({
  default: () => ({
    frequencyToString: (value: string) => value,
  }),
}))

const ClickableButtonStub = {
  props: ['label', 'disabled', 'loading'],
  emits: ['click'],
  template: '<button :data-label="label" :disabled="disabled" @click="!disabled && $emit(\'click\')">{{ label }}<slot /></button>',
}

const DialogWithFooterStub = {
  props: ['visible'],
  template: '<div v-if="visible"><slot /><slot name="footer" /></div>',
}

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
  const getRegularTransaction = vi.fn().mockResolvedValue({
    content: [createRegularTransaction('rt-1')],
    pageNumber: 0,
    pageSize: 10,
    totalElements: 1,
    totalPages: 1,
  })
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

  const linkRegularTransactionToBooklet = vi.fn().mockResolvedValue(createRegularTransaction('rt-1'))
  const unlinkRegularTransactionFromBooklet = vi.fn().mockResolvedValue(createRegularTransaction('rt-1'))

  const success = vi.fn()
  const errorAxios = vi.fn()
  const warn = vi.fn()
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
    linkRegularTransactionToBooklet,
    unlinkRegularTransactionFromBooklet,
  }))
  vi.stubGlobal('useJToast', () => ({ success, errorAxios, warn }))
  vi.stubGlobal('useConfirm', () => ({ require }))

  const wrapper = shallowMount(RegularTransactionPage, {
    global: {
      stubs: {
        AppTable: { template: '<div><slot name="empty" /></div>' },
        Button: { props: ['label'], template: '<button>{{ label }}<slot /></button>' },
        Tag: { template: '<span><slot /></span>' },
        ConfirmDialog: true,
        Paginator: true,
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
      linkRegularTransactionToBooklet,
      unlinkRegularTransactionFromBooklet,
      success,
      errorAxios,
      warn,
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
      bookletIds: ['booklet-1'],
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
      bookletIds: ['booklet-1'],
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
      bookletIds: ['booklet-1'],
    }

    wrapper.findComponent({ name: 'RegularTransactionCreationDialog' }).vm.$emit('create-transaction', payload)
    await flushPromises()

    expect(wrapper.findComponent({ name: 'RegularTransactionCreationDialog' }).props('loading')).toBe(true)

    deferred.resolve(createRegularTransaction('rt-late'))
    await flushPromises()

    expect(wrapper.findComponent({ name: 'RegularTransactionCreationDialog' }).props('loading')).toBe(false)
  })

  it('does not call save endpoint when no booklet is selected', async () => {
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

    expect(mocks.saveMonthlyTransaction).not.toHaveBeenCalled()
    expect(mocks.warn).toHaveBeenCalledWith('Veuillez sélectionner au moins un compte')
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

function mountPageForDialogTests(options?: { linkReject?: any, unlinkReject?: any }) {
  const activeBooklet = { id: 'booklet-1', amount: 200, label: 'Compte principal', currency: 'EUR' }
  const transactionAllLinked = createRegularTransaction('rt-all-linked') // bookletIds: ['booklet-1']
  const transactionNoneLinked = { ...createRegularTransaction('rt-none-linked'), bookletIds: [] as string[] }

  const fetch = vi.fn().mockResolvedValue([activeBooklet])
  const getRegularTransaction = vi.fn().mockResolvedValue({
    content: [transactionAllLinked, transactionNoneLinked],
    pageNumber: 0,
    pageSize: 10,
    totalElements: 2,
    totalPages: 1,
  })
  const linkRegularTransactionToBooklet = options?.linkReject
    ? vi.fn().mockRejectedValue(options.linkReject)
    : vi.fn().mockResolvedValue({ ...transactionAllLinked })
  const unlinkRegularTransactionFromBooklet = options?.unlinkReject
    ? vi.fn().mockRejectedValue(options.unlinkReject)
    : vi.fn().mockResolvedValue({ ...transactionNoneLinked })

  const success = vi.fn()
  const errorAxios = vi.fn()

  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('useBooklet', () => ({ fetch }))
  vi.stubGlobal('useRegularTransaction', () => ({
    getRegularTransaction,
    saveMonthlyTransaction: vi.fn(),
    getRegularTransactionById: vi.fn(),
    updateRegularTransaction: vi.fn(),
    deleteRegularTransaction: vi.fn(),
    deleteRegularTransactions: vi.fn(),
    linkRegularTransactionToBooklet,
    unlinkRegularTransactionFromBooklet,
  }))
  vi.stubGlobal('useJToast', () => ({ success, errorAxios, warn: vi.fn() }))
  vi.stubGlobal('useConfirm', () => ({ require: vi.fn() }))

  const wrapper = shallowMount(RegularTransactionPage, {
    global: {
      stubs: {
        // Rend le slot body-actions avec les deux transactions de test
        AppTable: {
          template: `<div>
            <slot name="body-actions" :data="{ id: 'rt-all-linked', bookletIds: ['booklet-1'] }" />
            <slot name="body-actions" :data="{ id: 'rt-none-linked', bookletIds: [] }" />
          </div>`,
        },
        Button: ClickableButtonStub,
        Tag: { template: '<span><slot /></span>' },
        Dialog: DialogWithFooterStub,
        Select: { props: ['modelValue'], emits: ['update:modelValue'], template: '<select />' },
        ConfirmDialog: true,
        RegularTransactionCreationDialog: { name: 'RegularTransactionCreationDialog', props: ['loading'], template: '<div />' },
        RegularTransactionDialogCard: { name: 'RegularTransactionDialogCard', template: '<div />' },
      },
    },
  })

  return { wrapper, mocks: { linkRegularTransactionToBooklet, unlinkRegularTransactionFromBooklet, success, errorAxios } }
}

describe('pages/regular-transaction/index – link/unlink button conditions', () => {
  it('link button is disabled when all active booklets are already linked', async () => {
    const { wrapper } = mountPageForDialogTests()
    await flushPromises()

    const linkButtons = wrapper.findAll('[data-test="btn-link"]')
    linkButtons.find(b => b.attributes('data-label') === undefined
      && b.element.closest()
      && wrapper.findAll('[data-test="btn-link"]')[0] === b)
    // Premier btn-link correspond à rt-all-linked (bookletIds=['booklet-1'] == tous les livrets actifs)
    expect(linkButtons[0].attributes('disabled')).toBeDefined()
  })

  it('link button is enabled when at least one active booklet is not yet linked', async () => {
    const { wrapper } = mountPageForDialogTests()
    await flushPromises()

    const linkButtons = wrapper.findAll('[data-test="btn-link"]')
    // Deuxième btn-link correspond à rt-none-linked (bookletIds=[] → booklet-1 pas encore lié)
    expect(linkButtons[1].attributes('disabled')).toBeUndefined()
  })

  it('unlink button is enabled when at least one active booklet is linked', async () => {
    const { wrapper } = mountPageForDialogTests()
    await flushPromises()

    const unlinkButtons = wrapper.findAll('[data-test="btn-unlink"]')
    // Premier btn-unlink correspond à rt-all-linked (bookletIds=['booklet-1'] → 1 livret actif lié)
    expect(unlinkButtons[0].attributes('disabled')).toBeUndefined()
  })

  it('unlink button is disabled when no active booklet is linked', async () => {
    const { wrapper } = mountPageForDialogTests()
    await flushPromises()

    const unlinkButtons = wrapper.findAll('[data-test="btn-unlink"]')
    // Deuxième btn-unlink correspond à rt-none-linked (bookletIds=[] → aucun livret actif lié)
    expect(unlinkButtons[1].attributes('disabled')).toBeDefined()
  })
})
