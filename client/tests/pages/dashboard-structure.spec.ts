import { shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import DashboardPage from '../../pages/index.vue'

vi.mock('@vueuse/core', () => ({ useIntersectionObserver: vi.fn() }))

vi.mock('vue-chartjs', () => ({
  Bar: { name: 'Bar', props: ['data', 'options'], template: '<div class="bar-chart" />' },
  Doughnut: { name: 'Doughnut', props: ['data', 'options'], template: '<div class="doughnut-chart" />' },
  Line: { name: 'Line', props: ['data', 'options'], template: '<div class="line-chart" />' },
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

const BOOKLET = { id: '11111111-1111-4111-8111-111111111111', label: 'Livret A', amount: 1200, transactions: [] }

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
  vi.stubGlobal('useBooklet', () => ({ createBooklet: vi.fn(), fetch: vi.fn().mockResolvedValue([BOOKLET]) }))
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
  vi.stubGlobal('useLocalStorage', (_key: string, defaultValue: unknown) => ref(defaultValue))

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

function zone(wrapper: ReturnType<typeof mountDashboard>, name: 'situation' | 'upcoming' | 'breakdown') {
  return wrapper.find(`[data-test="zone-${name}"]`)
}

describe('pages/index structure', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 8, 18, 12, 0, 0))
  })

  afterEach(() => vi.useRealTimers())

  // About fifteen blocks of equal weight made the page ask to be read in full.
  it('groups the overview into three named zones', async () => {
    const wrapper = mountDashboard()
    await settle()

    expect(zone(wrapper, 'situation').find('h2').text()).toMatch(/où j'en suis/i)
    expect(zone(wrapper, 'upcoming').find('h2').text()).toMatch(/ce qui arrive/i)
    expect(zone(wrapper, 'breakdown').find('h2').text()).toMatch(/où part l'argent/i)
  })

  it('places the situation figures in the first zone', async () => {
    const wrapper = mountDashboard()
    await settle()

    const situation = zone(wrapper, 'situation')
    expect(situation.find('[data-test="kpi-balance"]').exists()).toBe(true)
    expect(situation.find('[data-test="kpi-projection"]').exists()).toBe(true)
    expect(situation.find('[data-test="account-budget"]').exists()).toBe(true)
    expect(situation.find('[data-test="line-chart-container"]').exists()).toBe(true)
  })

  it('places what is coming in the second zone', async () => {
    const wrapper = mountDashboard()
    await settle()

    const upcoming = zone(wrapper, 'upcoming')
    expect(upcoming.find('[data-test="upcoming-list"]').exists()).toBe(true)
    expect(upcoming.find('[data-test="period-alerts"]').exists()).toBe(true)
  })

  it('places where the money goes in the third zone', async () => {
    const wrapper = mountDashboard()
    await settle()

    const breakdown = zone(wrapper, 'breakdown')
    expect(breakdown.find('[data-test="category-breakdown"]').exists()).toBe(true)
    expect(breakdown.text()).toContain('Top tags de la période')
  })

  // "Tags populaires" listed the first six tags in API order: its title promised a ranking that
  // did not exist. The real ranking is "Top tags de la période".
  it('drops the blocks that repeated or misrepresented other figures', async () => {
    const wrapper = mountDashboard()
    await settle()

    const text = wrapper.text()
    expect(text).not.toContain('Tags populaires')
    expect(text).not.toContain('Mes livrets')
    expect(text).not.toContain('Tags créés')
    expect(text).not.toContain('Objectif : 30')
  })

  // The four header pills repeated figures shown further down.
  it('shows each upcoming figure in one place only', async () => {
    const wrapper = mountDashboard()
    await settle()

    const text = wrapper.text()
    expect(text).not.toContain('Solde prévisionnel court terme')
    expect(text.match(/Projection fin de période/g)).toHaveLength(1)
    expect(wrapper.findAll('[data-test="upcoming-summary"]')).toHaveLength(1)
  })

  // The exact range matters once cycles are per booklet: "septembre" alone does not say 25/08.
  it('states the exact date range of the period in the header', async () => {
    const wrapper = mountDashboard()
    await settle()

    expect(wrapper.find('[data-test="dashboard-scope"]').text()).toContain((wrapper.vm as any).currentDateRangeLabel)
  })

  it('keeps the quick actions on the overview', async () => {
    const wrapper = mountDashboard()
    await settle()

    expect(wrapper.find('[data-test="quick-add-regular"]').exists()).toBe(true)
  })

  it('opens on the overview, without the period comparison', async () => {
    const wrapper = mountDashboard()
    await settle()

    expect(wrapper.find('[data-test="tab-overview"]').attributes('aria-selected')).toBe('true')
    expect(wrapper.find('[data-test="bar-chart-container"]').exists()).toBe(false)
  })

  it('moves the period comparison to the analysis tab', async () => {
    const wrapper = mountDashboard()
    await settle()

    await wrapper.find('[data-test="tab-analysis"]').trigger('click')

    expect(wrapper.find('[data-test="tab-analysis"]').attributes('aria-selected')).toBe('true')
    expect(wrapper.find('[data-test="bar-chart-container"]').exists()).toBe(true)
    expect(zone(wrapper, 'situation').exists()).toBe(false)
  })
})
