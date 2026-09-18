import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import DashboardPage from '../../pages/index.vue'

vi.mock('@vueuse/core', () => ({ useIntersectionObserver: vi.fn() }))

vi.mock('vue-chartjs', () => ({
  Bar: { name: 'Bar', props: ['data', 'options'], template: '<div />' },
  Doughnut: { name: 'Doughnut', props: ['data', 'options'], template: '<div />' },
  Line: { name: 'Line', props: ['data', 'options'], template: '<div />' },
}))

vi.mock('@/composables/useAuth', () => ({ default: () => ({ user: { username: 'johan' } }) }))

vi.mock('~/composables/useStats', () => ({
  default: () => ({
    getCategoryDistribution: vi.fn().mockResolvedValue({ categories: [], totalExpenses: '0.00' }),
    getTrendStats: vi.fn().mockResolvedValue({ monthlyTrends: [] }),
    getPrevisionalTransactions: vi.fn().mockResolvedValue({
      transactions: [],
      groupedByBooklet: {},
      totalAmount: '0.00',
      totalIncome: '0.00',
      totalExpenses: '0.00',
      regularTransactions: [],
      nonRegularTransactions: [],
      totalRegularAmount: '0.00',
      totalNonRegularAmount: '0.00',
      startDate: new Date(),
      endDate: new Date(),
    }),
    getDailyTrendStats: vi.fn().mockResolvedValue({ dailyTrends: [] }),
  }),
}))

vi.mock('~/composables/useUserSettings', () => ({
  default: () => ({ getSettings: vi.fn().mockResolvedValue({ projectionWindowDays: 15, bookletCycles: [] }) }),
}))

const BOOKLET = { id: 'b-1', label: 'Livret A', amount: 1200, transactions: [] }

const fetchBookletsMock = vi.fn()
const saveTransactionMock = vi.fn()
const saveMonthlyTransactionMock = vi.fn()
const openCsvMock = vi.fn()
const navigateToMock = vi.fn()
const toastSuccess = vi.fn()
const toastErrorAxios = vi.fn()

const TransactionDialogStub = {
  name: 'TransactionCreationDialog',
  props: ['visible', 'loading', 'title', 'digitPlaceholder', 'transactionPlaceholder'],
  template: '<div />',
}

const RegularDialogStub = {
  name: 'RegularTransactionCreationDialog',
  props: ['visible', 'booklets', 'loading'],
  template: '<div />',
}

/** Exposes openDialog like the real one, which the page opens through a template ref. */
const CsvDialogStub = {
  name: 'CsvImportDialog',
  props: ['bookletId', 'month', 'year'],
  emits: ['importSuccess'],
  setup(_: unknown, { expose }: { expose: (exposed: Record<string, unknown>) => void }) {
    expose({ openDialog: openCsvMock })
    return {}
  },
  template: '<div />',
}

/** Scopes in flight, reactive so the page's `computed(() => isScopeLoading(...))` re-evaluates. */
const activeScopes = ref<string[]>([])

function deferred<T>() {
  let resolve!: (value: T) => void
  const promise = new Promise<T>((res) => {
    resolve = res
  })
  return { promise, resolve }
}

function flushPromises() {
  return new Promise<void>(resolve => queueMicrotask(resolve))
}

async function settle() {
  for (let i = 0; i < 10; i++) await flushPromises()
  await nextTick()
}

function mountDashboard() {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('navigateTo', navigateToMock)
  vi.stubGlobal('useBooklet', () => ({ createBooklet: vi.fn(), fetch: fetchBookletsMock }))
  vi.stubGlobal('useTransaction', () => ({ saveTransaction: saveTransactionMock }))
  vi.stubGlobal('useRegularTransaction', () => ({
    getRegularTransaction: vi.fn().mockResolvedValue([]),
    saveMonthlyTransaction: saveMonthlyTransactionMock,
  }))
  vi.stubGlobal('useTag', () => ({ getAllTags: vi.fn().mockResolvedValue([]) }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: (scope: string) => activeScopes.value.includes(scope),
    withLoading: async <T>(action: () => Promise<T>, scope: string) => {
      activeScopes.value = [...activeScopes.value, scope]
      try {
        return await action()
      } finally {
        activeScopes.value = activeScopes.value.filter(active => active !== scope)
      }
    },
  }))
  vi.stubGlobal('useJToast', () => ({ success: toastSuccess, error: vi.fn(), errorAxios: toastErrorAxios }))
  vi.stubGlobal('rgbToHex', () => '#ffffff')
  vi.stubGlobal('useLocalStorage', (_key: string, defaultValue: unknown) => ref(defaultValue))

  return shallowMount(DashboardPage, {
    global: {
      mocks: { capitalizeFirst: (value: string) => value },
      stubs: {
        BookletBookingDialog: true,
        TransactionCreationDialog: TransactionDialogStub,
        RegularTransactionCreationDialog: RegularDialogStub,
        CsvImportDialog: CsvDialogStub,
      },
    },
  })
}

const VALID_TRANSACTION = {
  id: null,
  label: 'Courses',
  value: 42,
  isIncome: false,
  date: new Date(2026, 8, 18),
  tagDTO: { tagId: undefined, label: '', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
  isPreview: false,
}

describe('pages/index quick actions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    activeScopes.value = []
    fetchBookletsMock.mockResolvedValue([BOOKLET])
    saveTransactionMock.mockResolvedValue({})
    saveMonthlyTransactionMock.mockResolvedValue({})
  })

  // The three former buttons only navigated to pages the sidebar already links to.
  it('no longer offers plain links to pages the sidebar already has', async () => {
    const wrapper = mountDashboard()
    await settle()

    const text = wrapper.text()
    expect(text).not.toContain('Voir mes comptes')
    expect(text).not.toContain('Ajuster les régulières')
    expect(text).not.toContain('Revoir mes tags')
  })

  // UX-41 removed a "which account is this about?" confusion; an unnamed action would bring it back.
  it('names the booklet the single-booklet actions apply to', async () => {
    const wrapper = mountDashboard()
    await settle()

    expect(wrapper.find('[data-test="quick-add-transaction"]').text()).toContain('Livret A')
    expect(wrapper.find('[data-test="quick-import-csv"]').text()).toContain('Livret A')
  })

  it('opens the transaction dialog without leaving the dashboard', async () => {
    const wrapper = mountDashboard()
    await settle()

    await wrapper.find('[data-test="quick-add-transaction"]').trigger('click')

    expect(wrapper.findComponent(TransactionDialogStub).props('visible')).toBe(true)
    expect(navigateToMock).not.toHaveBeenCalled()
  })

  it('records the transaction on the selected booklet and refreshes the dashboard', async () => {
    const wrapper = mountDashboard()
    await settle()
    const loadsBefore = fetchBookletsMock.mock.calls.length

    await wrapper.find('[data-test="quick-add-transaction"]').trigger('click')
    wrapper.findComponent(TransactionDialogStub).vm.$emit('createTransaction', VALID_TRANSACTION)
    await settle()

    expect(saveTransactionMock).toHaveBeenCalledWith('Livret A', VALID_TRANSACTION)
    expect(fetchBookletsMock.mock.calls.length).toBeGreaterThan(loadsBefore)
    expect(wrapper.findComponent(TransactionDialogStub).props('visible')).toBe(false)
    expect(toastSuccess).toHaveBeenCalled()
  })

  // Closing on failure would throw away what the user typed.
  it('reports a failed save and keeps the dialog open', async () => {
    saveTransactionMock.mockRejectedValue(new Error('rejected'))
    const wrapper = mountDashboard()
    await settle()

    await wrapper.find('[data-test="quick-add-transaction"]').trigger('click')
    wrapper.findComponent(TransactionDialogStub).vm.$emit('createTransaction', VALID_TRANSACTION)
    await settle()

    expect(toastErrorAxios).toHaveBeenCalled()
    expect(wrapper.findComponent(TransactionDialogStub).props('visible')).toBe(true)
  })

  // The reload runs under the same scope as the first load, which drew the full-page skeleton:
  // refreshing after a quick action would have wiped the whole dashboard. UX-26's rule is that a
  // reload over content already on screen keeps it.
  it('keeps the dashboard on screen while it refreshes after a quick action', async () => {
    const wrapper = mountDashboard()
    await settle()

    const reload = deferred<typeof BOOKLET[]>()
    fetchBookletsMock.mockImplementationOnce(() => reload.promise)

    await wrapper.find('[data-test="quick-add-transaction"]').trigger('click')
    wrapper.findComponent(TransactionDialogStub).vm.$emit('createTransaction', VALID_TRANSACTION)
    await settle()

    // The reload is genuinely in flight, or the absence below would prove nothing.
    expect((wrapper.vm as any).isLoading).toBe(true)
    expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="quick-add-transaction"]').exists()).toBe(true)

    reload.resolve([BOOKLET])
    await settle()
  })

  it('still draws the skeleton on the very first load', () => {
    fetchBookletsMock.mockImplementation(() => new Promise(() => {}))
    const wrapper = mountDashboard()

    return settle().then(() => {
      expect(wrapper.find('[data-test="page-skeleton"]').exists()).toBe(true)
    })
  })

  it('opens the CSV import for the selected booklet', async () => {
    const wrapper = mountDashboard()
    await settle()

    await wrapper.find('[data-test="quick-import-csv"]').trigger('click')

    expect(openCsvMock).toHaveBeenCalled()
    expect(wrapper.findComponent(CsvDialogStub).props('bookletId')).toBe('b-1')
  })

  it('refreshes the dashboard once an import succeeds', async () => {
    const wrapper = mountDashboard()
    await settle()
    const loadsBefore = fetchBookletsMock.mock.calls.length

    wrapper.findComponent(CsvDialogStub).vm.$emit('importSuccess')
    await settle()

    expect(fetchBookletsMock.mock.calls.length).toBeGreaterThan(loadsBefore)
  })

  it('creates a recurring entry from the dashboard and refreshes it', async () => {
    const wrapper = mountDashboard()
    await settle()
    const loadsBefore = fetchBookletsMock.mock.calls.length

    await wrapper.find('[data-test="quick-add-regular"]').trigger('click')
    expect(wrapper.findComponent(RegularDialogStub).props('visible')).toBe(true)

    const entry = { label: 'Loyer', value: 900, isIncome: false, bookletIds: ['b-1'] }
    wrapper.findComponent(RegularDialogStub).vm.$emit('createTransaction', entry)
    await settle()

    expect(saveMonthlyTransactionMock).toHaveBeenCalledWith(entry)
    expect(fetchBookletsMock.mock.calls.length).toBeGreaterThan(loadsBefore)
    expect(wrapper.findComponent(RegularDialogStub).props('visible')).toBe(false)
  })
})
