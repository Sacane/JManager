import { shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import DashboardPage from '../../pages/index.vue'

vi.mock('@vueuse/core', () => ({ useIntersectionObserver: vi.fn() }))

vi.mock('vue-chartjs', () => ({
  Bar: { name: 'Bar', props: ['data', 'options'], template: '<div />' },
  Doughnut: { name: 'Doughnut', props: ['data', 'options'], template: '<div />' },
  Line: { name: 'Line', props: ['data', 'options'], template: '<div />' },
}))

vi.mock('@/composables/useAuth', () => ({ default: () => ({ user: { username: 'johan' } }) }))

const getPrevisionalTransactionsMock = vi.fn()
const getDailyTrendStatsMock = vi.fn()

vi.mock('~/composables/useStats', () => ({
  default: () => ({
    getCategoryDistribution: vi.fn().mockResolvedValue({ categories: [], totalExpenses: '0.00' }),
    getTrendStats: vi.fn().mockResolvedValue({ monthlyTrends: [] }),
    getPrevisionalTransactions: getPrevisionalTransactionsMock,
    getDailyTrendStats: getDailyTrendStatsMock,
  }),
}))

const getSettingsMock = vi.fn()

vi.mock('~/composables/useUserSettings', () => ({
  default: () => ({ getSettings: getSettingsMock }),
}))

// The dashboard only sends a booklet id to the stats when it has the shape of a UUID.
const COURANT = { id: '11111111-1111-4111-8111-111111111111', label: 'Compte courant', amount: 100, transactions: [] }
const EPARGNE = { id: '22222222-2222-4222-8222-222222222222', label: 'Épargne', amount: 250, transactions: [] }

/** Backs useLocalStorage with a real store, so a choice can survive a second mount. */
const storage = new Map<string, ReturnType<typeof ref>>()

function flushPromises() {
  return new Promise<void>(resolve => queueMicrotask(resolve))
}

async function settle() {
  for (let i = 0; i < 12; i++) await flushPromises()
  await nextTick()
}

function mountDashboard() {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('navigateTo', vi.fn())
  vi.stubGlobal('useBooklet', () => ({ createBooklet: vi.fn(), fetch: vi.fn().mockResolvedValue([COURANT, EPARGNE]) }))
  vi.stubGlobal('useTransaction', () => ({ saveTransaction: vi.fn() }))
  vi.stubGlobal('useRegularTransaction', () => ({
    getRegularTransaction: vi.fn().mockResolvedValue([]),
    saveMonthlyTransaction: vi.fn(),
  }))
  vi.stubGlobal('useTag', () => ({ getAllTags: vi.fn().mockResolvedValue([]) }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: () => false,
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn(), errorAxios: vi.fn() }))
  vi.stubGlobal('rgbToHex', () => '#ffffff')
  vi.stubGlobal('useLocalStorage', (key: string, defaultValue: unknown) => {
    if (!storage.has(key)) storage.set(key, ref(defaultValue))
    return storage.get(key)!
  })

  return shallowMount(DashboardPage, {
    global: {
      mocks: { capitalizeFirst: (value: string) => value },
      stubs: {
        BookletBookingDialog: true,
        TransactionCreationDialog: true,
        RegularTransactionCreationDialog: true,
        CsvImportDialog: true,
      },
    },
  })
}

function selectAllAccounts(wrapper: ReturnType<typeof mountDashboard>) {
  return wrapper.find('[data-test="account-selector"]').setValue('all')
}

/** Booklet id of the last stats request, undefined when it covered every booklet. */
function lastStatsBookletId() {
  return getDailyTrendStatsMock.mock.calls.at(-1)?.[2]
}

function lastStatsRange() {
  const call = getDailyTrendStatsMock.mock.calls.at(-1)
  return { start: call?.[0], end: call?.[1] }
}

describe('pages/index all accounts mode', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    storage.clear()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 8, 18, 12, 0, 0))
    getSettingsMock.mockResolvedValue({
      projectionWindowDays: 15,
      bookletCycles: [
        { bookletId: COURANT.id, label: COURANT.label, monthlyPeriodStartDay: 25, monthlyPeriodEndDay: null },
      ],
    })
    getPrevisionalTransactionsMock.mockResolvedValue({
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
    })
    getDailyTrendStatsMock.mockResolvedValue({ dailyTrends: [] })
  })

  afterEach(() => vi.useRealTimers())

  it('offers "Tous les comptes" before the individual booklets', async () => {
    const wrapper = mountDashboard()
    await settle()

    const options = wrapper.findAll('[data-test="account-selector"] option')
    expect(options[0].text()).toBe('Tous les comptes')
    expect(options.map(option => option.text())).toContain('Compte courant')
  })

  it('queries the stats for every booklet once selected', async () => {
    const wrapper = mountDashboard()
    await settle()
    expect(lastStatsBookletId()).toBe(COURANT.id)

    await selectAllAccounts(wrapper)
    await settle()

    expect(lastStatsBookletId()).toBeUndefined()
  })

  // Cycles are per booklet, so an aggregated view has none of its own (decision of 19/09/2026).
  it('uses the calendar month when every booklet is aggregated', async () => {
    const wrapper = mountDashboard()
    await settle()

    await selectAllAccounts(wrapper)
    await settle()

    expect(lastStatsRange()).toEqual({ start: '2026-09-01', end: '2026-09-30' })
  })

  it('keeps the booklet own cycle once that booklet is selected', async () => {
    mountDashboard()
    await settle()

    // The default selection is the first booklet, whose cycle starts on the 25th.
    expect(lastStatsRange()).toEqual({ start: '2026-08-25', end: '2026-09-24' })
  })

  it('shows the total balance of every booklet', async () => {
    const wrapper = mountDashboard()
    await settle()

    await selectAllAccounts(wrapper)
    await settle()

    expect((wrapper.vm as any).selectedBookletBalance).toBe(350)
  })

  it('names the aggregated view in the header', async () => {
    const wrapper = mountDashboard()
    await settle()

    await selectAllAccounts(wrapper)
    await settle()

    expect(wrapper.find('[data-test="dashboard-scope"]').text()).toContain('Tous les comptes')
  })

  // The budget target is stored per booklet: "all accounts" has no target of its own.
  it('hides the per-booklet budget', async () => {
    const wrapper = mountDashboard()
    await settle()
    expect(wrapper.find('[data-test="account-budget"]').exists()).toBe(true)

    await selectAllAccounts(wrapper)
    await settle()

    expect(wrapper.find('[data-test="account-budget"]').exists()).toBe(false)
  })

  it('hides the quick actions that need a single booklet', async () => {
    const wrapper = mountDashboard()
    await settle()

    await selectAllAccounts(wrapper)
    await settle()

    expect(wrapper.find('[data-test="quick-add-transaction"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="quick-import-csv"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="quick-add-regular"]').exists()).toBe(true)
  })

  it('remembers the aggregated view on the next visit', async () => {
    const first = mountDashboard()
    await settle()
    await selectAllAccounts(first)
    await settle()
    first.unmount()

    const second = mountDashboard()
    await settle()

    expect((second.find('[data-test="account-selector"]').element as HTMLSelectElement).value).toBe('all')
    expect(lastStatsBookletId()).toBeUndefined()
  })
})
