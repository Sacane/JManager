import { shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick, ref } from 'vue'
import DashboardPage from '../../pages/index.vue'

vi.mock('@vueuse/core', () => ({ useIntersectionObserver: vi.fn() }))

vi.mock('vue-chartjs', () => ({
  Bar: { name: 'Bar', props: ['data', 'options'], template: '<div class="bar-chart" />' },
  Doughnut: { name: 'Doughnut', props: ['data', 'options'], template: '<div class="doughnut-chart" />' },
  Line: { name: 'Line', props: ['data', 'options'], template: '<div class="line-chart" />' },
}))

vi.mock('@/composables/useAuth', () => ({
  default: () => ({ user: { username: 'johan' } }),
}))

const fetchBookletsMock = vi.fn()
const createBookletMock = vi.fn()

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
  default: () => ({
    getSettings: vi.fn().mockResolvedValue({ projectionWindowDays: 15, bookletCycles: [] }),
  }),
}))

function flushPromises() {
  return new Promise<void>(resolve => queueMicrotask(resolve))
}

async function settle() {
  for (let i = 0; i < 8; i++) await flushPromises()
  await nextTick()
}

function mountDashboard(activeScopes: string[] = []) {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('navigateTo', vi.fn())
  vi.stubGlobal('useBooklet', () => ({ createBooklet: createBookletMock, fetch: fetchBookletsMock }))
  vi.stubGlobal('useRegularTransaction', () => ({ getRegularTransaction: vi.fn().mockResolvedValue([]) }))
  vi.stubGlobal('useTag', () => ({ getAllTags: vi.fn().mockResolvedValue([]) }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: (scope: string) => activeScopes.includes(scope),
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn(), errorAxios: vi.fn() }))
  vi.stubGlobal('capitalizeFirst', (value: string) => value)
  vi.stubGlobal('rgbToHex', () => '#ffffff')
  vi.stubGlobal('useLocalStorage', (_key: string, defaultValue: unknown) => ref(defaultValue))

  return shallowMount(DashboardPage, { global: { stubs: { BookletBookingDialog: true } } })
}

const onboarding = (wrapper: ReturnType<typeof mountDashboard>) => wrapper.find('[data-test="dashboard-onboarding"]')

describe('pages/index onboarding state', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchBookletsMock.mockResolvedValue([])
    createBookletMock.mockResolvedValue({ id: 'b-1', label: 'Livret A', amount: 0, transactions: [] })
  })

  it('invites the user to create a booklet when there is none', async () => {
    const wrapper = mountDashboard()
    await settle()

    expect(onboarding(wrapper).exists()).toBe(true)
    expect(onboarding(wrapper).text()).toMatch(/livret/i)
  })

  // Four KPI cards at 0.00 EUR and three empty charts say nothing to a new user.
  it('renders no indicator, chart or period control while empty', async () => {
    const wrapper = mountDashboard()
    await settle()

    expect(wrapper.find('[data-test="line-chart-container"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="bar-chart-container"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="daily-expense-average"]').exists()).toBe(false)
    expect(wrapper.find('.period-toggle').exists()).toBe(false)
    expect(wrapper.find('select').exists()).toBe(false)
  })

  it('opens the creation dialog from the onboarding action', async () => {
    const wrapper = mountDashboard()
    await settle()

    await wrapper.find('[data-test="onboarding-create-booklet"]').trigger('click')

    expect((wrapper.vm as any).isBookletDialogOpen).toBe(true)
  })

  it('shows the dashboard once a booklet exists', async () => {
    fetchBookletsMock.mockResolvedValue([
      { id: 'b-1', label: 'Compte principal', amount: 1200, transactions: [] },
    ])
    const wrapper = mountDashboard()
    await settle()

    expect(onboarding(wrapper).exists()).toBe(false)
    expect(wrapper.find('[data-test="daily-expense-average"]').exists()).toBe(true)
  })

  // Guarding on booklets.length alone would flash the onboarding screen on the first paint.
  it('shows the loading state rather than the onboarding one while loading', async () => {
    const wrapper = mountDashboard(['dashboard.initial'])
    await settle()

    expect(onboarding(wrapper).exists()).toBe(false)
    expect(wrapper.text()).toContain('Chargement')
  })

  it('does not treat the very first render as empty', async () => {
    const wrapper = mountDashboard()

    // No await: this is the paint before the fetch resolves.
    expect(onboarding(wrapper).exists()).toBe(false)

    await settle()

    expect(onboarding(wrapper).exists()).toBe(true)
  })
})
