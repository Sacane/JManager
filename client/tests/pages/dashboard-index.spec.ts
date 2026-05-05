import { shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardPage from '../../pages/dashboard/index.vue'

vi.mock('@vueuse/core', () => ({
  useIntersectionObserver: vi.fn(),
}))

vi.mock('vue-chartjs', () => ({
  Bar: { name: 'Bar', props: ['data', 'options'], template: '<div class="bar-chart" />' },
  Doughnut: { name: 'Doughnut', props: ['data', 'options'], template: '<div class="doughnut-chart" />' },
  Line: { name: 'Line', props: ['data', 'options'], template: '<div class="line-chart" />' },
}))

vi.mock('@/composables/useAuth', () => ({
  default: () => ({ user: { username: 'johan' } }),
}))

const fetchBookletsMock = vi.fn().mockResolvedValue([
  {
    id: '11111111-1111-4111-8111-111111111111',
    amount: 1200,
    label: 'Compte principal',
    transactions: [],
  },
])

const categoryCurrent = {
  categories: [
    { tagLabel: 'Courses', tagId: 'tag-1', colorDTO: { red: 255, green: 0, blue: 0 }, totalAmount: '200.00', percentage: 50, transactionCount: 2 },
    { tagLabel: 'Transport', tagId: 'tag-2', colorDTO: { red: 0, green: 0, blue: 255 }, totalAmount: '100.00', percentage: 25, transactionCount: 1 },
  ],
  totalExpenses: '400.00',
}

function makeCategory(tagLabel: string, tagId: string, totalAmount: string, percentage: number) {
  return {
    tagLabel,
    tagId,
    colorDTO: { red: 100, green: 100, blue: 100 },
    totalAmount,
    percentage,
    transactionCount: 1,
  }
}

// 9 tags in non-sorted order; Tag1 is the highest (300) and Tag9 the lowest (10)
const nineTagDistribution = {
  categories: [
    makeCategory('Tag3', 'tag-3', '150.00', 15),
    makeCategory('Tag1', 'tag-1', '300.00', 30),
    makeCategory('Tag9', 'tag-9', '10.00', 1),
    makeCategory('Tag5', 'tag-5', '80.00', 8),
    makeCategory('Tag2', 'tag-2', '200.00', 20),
    makeCategory('Tag7', 'tag-7', '40.00', 4),
    makeCategory('Tag4', 'tag-4', '100.00', 10),
    makeCategory('Tag8', 'tag-8', '20.00', 2),
    makeCategory('Tag6', 'tag-6', '60.00', 6),
  ],
  totalExpenses: '960.00',
}

const getCategoryDistributionMock = vi.fn().mockResolvedValue(categoryCurrent)

const getTrendStatsMock = vi.fn().mockResolvedValue({
  monthlyTrends: [],
})

const getPrevisionalTransactionsMock = vi.fn().mockResolvedValue({
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

const getDailyTrendStatsMock = vi.fn().mockResolvedValue({
  dailyTrends: [],
})

const getUserSettingsMock = vi.fn().mockResolvedValue({
  projectionWindowDays: 15,
  bookletCycles: [
    {
      bookletId: '11111111-1111-4111-8111-111111111111',
      label: 'Compte principal',
      monthlyPeriodStartDay: 1,
      monthlyPeriodEndDay: null,
    },
  ],
})

vi.mock('~/composables/useStats', () => ({
  default: () => ({
    getCategoryDistribution: getCategoryDistributionMock,
    getTrendStats: getTrendStatsMock,
    getPrevisionalTransactions: getPrevisionalTransactionsMock,
    getDailyTrendStats: getDailyTrendStatsMock,
  }),
}))

vi.mock('~/composables/useUserSettings', () => ({
  default: () => ({
    getSettings: getUserSettingsMock,
  }),
}))

function flushPromises() {
  return new Promise<void>(resolve => queueMicrotask(resolve))
}

async function settleDashboard() {
  // loadDashboardData chains: withLoading → Promise.all([booklets, settings, ...])
  // → loadStatsData() → Promise.all([getCategoryDistribution x2, getTrendStats x3, ...])
  // Each nested await requires at least one microtask tick; drain with multiple flushes.
  for (let i = 0; i < 8; i++) {
    await flushPromises()
  }
}

function mountDashboardPage() {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('navigateTo', vi.fn())
  vi.stubGlobal('useBooklet', () => ({
    createBooklet: vi.fn(),
    fetch: fetchBookletsMock,
  }))
  vi.stubGlobal('useRegularTransaction', () => ({ getRegularTransaction: vi.fn().mockResolvedValue([]) }))
  vi.stubGlobal('useTag', () => ({ getAllTags: vi.fn().mockResolvedValue([]) }))
  vi.stubGlobal('useLoading', () => ({
    isScopeLoading: () => false,
    withLoading: async <T>(action: () => Promise<T>) => action(),
  }))
  vi.stubGlobal('useJToast', () => ({ success: vi.fn(), error: vi.fn(), errorAxios: vi.fn() }))
  vi.stubGlobal('capitalizeFirst', (value: string) => value)
  vi.stubGlobal('rgbToHex', () => '#ffffff')

  return shallowMount(DashboardPage, {
    global: {
      stubs: {
        BookletBookingDialog: true,
      },
    },
  })
}

describe('pages/dashboard/index tags insights', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useRealTimers()

    fetchBookletsMock.mockResolvedValue([
      {
        id: '11111111-1111-4111-8111-111111111111',
        amount: 1200,
        label: 'Compte principal',
        transactions: [],
      },
    ])

    getCategoryDistributionMock.mockResolvedValue(categoryCurrent)
    getTrendStatsMock.mockResolvedValue({ monthlyTrends: [] })
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
    getUserSettingsMock.mockResolvedValue({
      projectionWindowDays: 15,
      bookletCycles: [
        {
          bookletId: '11111111-1111-4111-8111-111111111111',
          label: 'Compte principal',
          monthlyPeriodStartDay: 1,
          monthlyPeriodEndDay: null,
        },
      ],
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('renders top tags section with variation label', async () => {
    const wrapper = mountDashboardPage()

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('Top tags de la période')
    expect(wrapper.text()).toContain('Variation vs période précédente')
    expect(wrapper.text()).toContain('Alertes de la période')
    expect(wrapper.text()).toContain('Actions rapides')
    expect(wrapper.text()).toContain('Aucun mouvement à venir')
    expect(wrapper.text()).toContain('Projection fin de période')
    expect(wrapper.text()).toContain('Budget du compte')
    expect(wrapper.text()).toContain('Définis une cible pour activer les alertes budget.')
  })

  it('uses account cycle and configurable projection window in stats calls', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-10T12:00:00.000Z'))

    getUserSettingsMock.mockResolvedValue({
      projectionWindowDays: 21,
      bookletCycles: [
        {
          bookletId: '11111111-1111-4111-8111-111111111111',
          label: 'Compte principal',
          monthlyPeriodStartDay: 28,
          monthlyPeriodEndDay: null,
        },
      ],
    })

    mountDashboardPage()
    await flushPromises()
    await flushPromises()

    expect(getCategoryDistributionMock).toHaveBeenCalledWith(expect.objectContaining({
      bookletId: '11111111-1111-4111-8111-111111111111',
      startDate: '2026-02-28',
      endDate: '2026-03-27',
    }))

    expect(getPrevisionalTransactionsMock).toHaveBeenCalledWith(
      '2026-03-10',
      '2026-03-31',
      '11111111-1111-4111-8111-111111111111',
    )
  })

  it('bounds monthly period end to next-month cycle day minus one', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))

    getUserSettingsMock.mockResolvedValue({
      projectionWindowDays: 21,
      bookletCycles: [
        {
          bookletId: '11111111-1111-4111-8111-111111111111',
          label: 'Compte principal',
          monthlyPeriodStartDay: 28,
          monthlyPeriodEndDay: null,
        },
      ],
    })

    mountDashboardPage()
    await flushPromises()
    await flushPromises()

    expect(getCategoryDistributionMock).toHaveBeenCalledWith(expect.objectContaining({
      bookletId: '11111111-1111-4111-8111-111111111111',
      startDate: '2026-02-28',
      endDate: '2026-03-27',
    }))
  })

  it('uses explicit configured end day for monthly period', async () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-03-29T12:00:00.000Z'))

    getUserSettingsMock.mockResolvedValue({
      projectionWindowDays: 21,
      bookletCycles: [
        {
          bookletId: '11111111-1111-4111-8111-111111111111',
          label: 'Compte principal',
          monthlyPeriodStartDay: 28,
          monthlyPeriodEndDay: 30,
        },
      ],
    })

    mountDashboardPage()
    await flushPromises()
    await flushPromises()

    expect(getCategoryDistributionMock).toHaveBeenCalledWith(expect.objectContaining({
      bookletId: '11111111-1111-4111-8111-111111111111',
      startDate: '2026-02-28',
      endDate: '2026-03-30',
    }))
  })
})

describe('pages/dashboard/index category distribution chart completeness', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useRealTimers()

    fetchBookletsMock.mockResolvedValue([
      {
        id: '11111111-1111-4111-8111-111111111111',
        amount: 1200,
        label: 'Compte principal',
        transactions: [],
      },
    ])

    getTrendStatsMock.mockResolvedValue({ monthlyTrends: [] })
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
    getUserSettingsMock.mockResolvedValue({
      projectionWindowDays: 15,
      bookletCycles: [
        {
          bookletId: '11111111-1111-4111-8111-111111111111',
          label: 'Compte principal',
          monthlyPeriodStartDay: 1,
          monthlyPeriodEndDay: null,
        },
      ],
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // Scenario 1 — 6 or fewer tags: no regression
  it('shows all tags in insights list when there are 4 categories', async () => {
    getCategoryDistributionMock.mockResolvedValue(categoryCurrent)
    const wrapper = mountDashboardPage()
    await settleDashboard()

    expect(wrapper.text()).toContain('Courses')
    expect(wrapper.text()).toContain('Transport')
  })

  // Scenario 2 — more than 6 tags: list shows all 9
  it('shows all 9 tags in insights list when there are 9 categories', async () => {
    getCategoryDistributionMock.mockResolvedValue(nineTagDistribution)
    const wrapper = mountDashboardPage()
    await settleDashboard()

    for (const cat of nineTagDistribution.categories) {
      expect(wrapper.text()).toContain(cat.tagLabel)
    }
  })

  // Scenario 3 — tags are ordered by descending amount in both chart and list
  it('orders tags by descending expense amount in insights list', async () => {
    getCategoryDistributionMock.mockResolvedValue(nineTagDistribution)
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const text = wrapper.text()
    // Tag1 (300) must appear before Tag2 (200), which must appear before Tag3 (150)
    const pos1 = text.indexOf('Tag1')
    const pos2 = text.indexOf('Tag2')
    const pos3 = text.indexOf('Tag3')
    expect(pos1).toBeGreaterThan(-1)
    expect(pos2).toBeGreaterThan(-1)
    expect(pos3).toBeGreaterThan(-1)
    expect(pos1).toBeLessThan(pos2)
    expect(pos2).toBeLessThan(pos3)
  })

  // Scenario 4 — empty state: no regression
  it('shows empty state message when there are no categories', async () => {
    getCategoryDistributionMock.mockResolvedValue({ categories: [], totalExpenses: '0.00' })
    const wrapper = mountDashboardPage()
    await settleDashboard()

    expect(wrapper.text()).toContain('Aucun tag de dépense sur cette période')
  })
})

describe('pages/dashboard/index doughnut slice click toggle', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useRealTimers()

    fetchBookletsMock.mockResolvedValue([
      {
        id: '11111111-1111-4111-8111-111111111111',
        amount: 1200,
        label: 'Compte principal',
        transactions: [],
      },
    ])

    getCategoryDistributionMock.mockResolvedValue(categoryCurrent)
    getTrendStatsMock.mockResolvedValue({ monthlyTrends: [] })
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
    getUserSettingsMock.mockResolvedValue({
      projectionWindowDays: 15,
      bookletCycles: [
        {
          bookletId: '11111111-1111-4111-8111-111111111111',
          label: 'Compte principal',
          monthlyPeriodStartDay: 1,
          monthlyPeriodEndDay: null,
        },
      ],
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // Scenario 5 — No center label when no slice has been clicked
  it('does not show center label initially', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    expect(wrapper.find('[data-test="doughnut-center-label"]').exists()).toBe(false)
  })

  // Scenario 1 — Clicking a slice for the first time shows its amount
  it('shows amount in center label when a slice is clicked', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    // Simulate Chart.js onClick: call the handler directly via the Doughnut options
    const doughnutStub = wrapper.findComponent({ name: 'Doughnut' })
    const options = doughnutStub.props('options') as any
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()

    const centerLabel = wrapper.find('[data-test="doughnut-center-label"]')
    expect(centerLabel.exists()).toBe(true)
    // Courses: 200.00 €
    expect(centerLabel.text()).toBe('200.00 €')
  })

  // Scenario 2 — Clicking the same slice a second time toggles to percentage
  it('toggles to percentage when same slice is clicked again', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const doughnutStub = wrapper.findComponent({ name: 'Doughnut' })
    const options = doughnutStub.props('options') as any

    // First click: amount
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()

    // Second click: percentage
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()

    const centerLabel = wrapper.find('[data-test="doughnut-center-label"]')
    expect(centerLabel.exists()).toBe(true)
    // Courses: 200 / 300 total = 66.7%
    expect(centerLabel.text()).toBe('66.7%')
  })

  // Scenario 3 — Clicking the same slice a third time toggles back to amount
  it('toggles back to amount on third click of same slice', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const doughnutStub = wrapper.findComponent({ name: 'Doughnut' })
    const options = doughnutStub.props('options') as any

    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()

    const centerLabel = wrapper.find('[data-test="doughnut-center-label"]')
    expect(centerLabel.exists()).toBe(true)
    expect(centerLabel.text()).toBe('200.00 €')
  })

  // Scenario 4 — Clicking a different slice resets to amount view
  it('resets to amount when a different slice is clicked', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const doughnutStub = wrapper.findComponent({ name: 'Doughnut' })
    const options = doughnutStub.props('options') as any

    // Click slice A, toggle to percentage
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-test="doughnut-center-label"]').text()).toBe('66.7%')

    // Click slice B → resets to amount for Transport
    options.onClick(null, [{ index: 1 }])
    await wrapper.vm.$nextTick()

    const centerLabel = wrapper.find('[data-test="doughnut-center-label"]')
    expect(centerLabel.text()).toBe('100.00 €')
  })

  // Scenario 6 — Tooltip continues to show amount with € and percentage (all slices visible)
  it('provides tooltip callback with amount € and percentage', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const doughnutStub = wrapper.findComponent({ name: 'Doughnut' })
    const options = doughnutStub.props('options') as any
    const tooltipCallback = options.plugins.tooltip.callbacks.label

    const result = tooltipCallback({
      label: 'Courses',
      parsed: 200,
      dataset: { data: [200, 100] },
      chart: { getDataVisibility: () => true },
    })

    expect(result).toBe('Courses: 200.00 \u20AC (66.7%)')
  })

  // Scenario: Tooltip percentage excludes hidden slices from total
  it('tooltip percentage uses visible-only total when a slice is hidden', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const doughnutStub = wrapper.findComponent({ name: 'Doughnut' })
    const options = doughnutStub.props('options') as any

    // Hide slice 1 (Transport 100) via legend → hiddenDoughnutIndices is populated
    const hiddenState = new Set<number>()
    const mockChart = {
      toggleDataVisibility: vi.fn().mockImplementation((i: number) => {
        if (hiddenState.has(i)) hiddenState.delete(i)
        else hiddenState.add(i)
      }),
      update: vi.fn(),
      getDataVisibility: vi.fn().mockImplementation((i: number) => !hiddenState.has(i)),
    }
    options.plugins.legend.onClick(null, { index: 1 }, { chart: mockChart })
    await wrapper.vm.$nextTick()

    // Now call the tooltip: hiddenDoughnutIndices has {1}, so total = 200 → 100%
    const tooltipCallback = options.plugins.tooltip.callbacks.label
    const result = tooltipCallback({
      label: 'Courses',
      parsed: 200,
      dataset: { data: [200, 100] },
    })

    expect(result).toBe('Courses: 200.00 € (100.0%)')
  })

  // Scenario: Center label percentage updates when a slice is hidden via legend
  it('center label percentage uses visible-only total after legend hide', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const doughnutStub = wrapper.findComponent({ name: 'Doughnut' })
    const options = doughnutStub.props('options') as any

    // Select slice 0 (Courses 200) → toggle to percentage (200/300 = 66.7%)
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-test="doughnut-center-label"]').text()).toBe('66.7%')

    // Hide slice 1 (Transport 100) via legend → visible total becomes 200
    const hiddenState = new Set<number>()
    const mockChart = {
      toggleDataVisibility: vi.fn().mockImplementation((i: number) => {
        if (hiddenState.has(i)) hiddenState.delete(i)
        else hiddenState.add(i)
      }),
      update: vi.fn(),
      getDataVisibility: vi.fn().mockImplementation((i: number) => !hiddenState.has(i)),
    }
    options.plugins.legend.onClick(null, { index: 1 }, { chart: mockChart })
    await wrapper.vm.$nextTick()

    // Courses: 200 / 200 visible = 100.0%
    expect(wrapper.find('[data-test="doughnut-center-label"]').text()).toBe('100.0%')
  })

  // Scenario: Center label clears when its own selected slice is hidden via legend
  it('clears center label when the selected slice is hidden via legend', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const doughnutStub = wrapper.findComponent({ name: 'Doughnut' })
    const options = doughnutStub.props('options') as any

    // Select slice 0 (Courses)
    options.onClick(null, [{ index: 0 }])
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[data-test="doughnut-center-label"]').exists()).toBe(true)

    // Hide slice 0 via legend → center label must disappear
    const hiddenState = new Set<number>()
    const mockChart = {
      toggleDataVisibility: vi.fn().mockImplementation((i: number) => {
        if (hiddenState.has(i)) hiddenState.delete(i)
        else hiddenState.add(i)
      }),
      update: vi.fn(),
      getDataVisibility: vi.fn().mockImplementation((i: number) => !hiddenState.has(i)),
    }
    options.plugins.legend.onClick(null, { index: 0 }, { chart: mockChart })
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-test="doughnut-center-label"]').exists()).toBe(false)
  })

  // Scenario 7 — No regression when there are no category data
  it('does not show center label when no categories exist', async () => {
    getCategoryDistributionMock.mockResolvedValue({ categories: [], totalExpenses: '0.00' })
    const wrapper = mountDashboardPage()
    await settleDashboard()

    expect(wrapper.find('[data-test="doughnut-center-label"]').exists()).toBe(false)
  })

  // Scenario 9 — Chart and tags side by side on desktop
  it('renders chart and tags in a horizontal layout on desktop', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const container = wrapper.find('[data-test="doughnut-container"]')
    expect(container.exists()).toBe(true)
    // On desktop (isSmallScreen = false), the container should have min-h-70 and flex-1
    expect(container.classes()).toContain('min-h-70')
    expect(container.classes()).toContain('flex-1')
    expect(container.classes()).not.toContain('h-72')
  })

  // Scenario — onClick with empty elements array does nothing
  it('does nothing when clicking outside slices', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const doughnutStub = wrapper.findComponent({ name: 'Doughnut' })
    const options = doughnutStub.props('options') as any

    // Click with no elements (miss)
    options.onClick(null, [])
    await wrapper.vm.$nextTick()

    expect(wrapper.find('[data-test="doughnut-center-label"]').exists()).toBe(false)
  })
})

describe('pages/dashboard/index chart wheel Y-axis zoom', () => {
  const trendData = {
    dailyTrends: [
      { date: '2026-05-01', expenses: '1000', income: '2000', cumulativeBalance: '1000' },
      { date: '2026-05-15', expenses: '500', income: '3000', cumulativeBalance: '2500' },
    ],
  }

  const trendStatsData = {
    monthlyTrends: [
      { month: 5, year: 2026, expenses: '1500', income: '2500' },
    ],
  }

  beforeEach(() => {
    vi.clearAllMocks()
    vi.useRealTimers()

    fetchBookletsMock.mockResolvedValue([
      {
        id: '11111111-1111-4111-8111-111111111111',
        amount: 1200,
        label: 'Compte principal',
        transactions: [],
      },
    ])

    getCategoryDistributionMock.mockResolvedValue(categoryCurrent)
    getTrendStatsMock.mockResolvedValue(trendStatsData)
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
    getDailyTrendStatsMock.mockResolvedValue(trendData)
    getUserSettingsMock.mockResolvedValue({
      projectionWindowDays: 15,
      bookletCycles: [
        {
          bookletId: '11111111-1111-4111-8111-111111111111',
          label: 'Compte principal',
          monthlyPeriodStartDay: 1,
          monthlyPeriodEndDay: null,
        },
      ],
    })
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  // Scenario 1 — forward scroll (deltaY < 0) zooms in: max decreases
  it('decreases Y-axis max on Line chart when scrolling forward', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const lineContainer = wrapper.find('[data-test="line-chart-container"]')
    expect(lineContainer.exists()).toBe(true)

    const lineStub = wrapper.findComponent({ name: 'Line' })
    const optsBefore = lineStub.props('options') as any
    expect(optsBefore.scales.y.max).toBeUndefined()

    await lineContainer.trigger('wheel', { deltaY: -100 })
    await wrapper.vm.$nextTick()

    const optsAfter = lineStub.props('options') as any
    expect(optsAfter.scales.y.max).toBeDefined()
    // Initial max is derived from data: max(1000,500,2000,3000,1000,2500) = 3000 + padding
    // After forward zoom the max should be less than the initial auto-computed max
    expect(optsAfter.scales.y.max).toBeLessThan(3000 + (3000 - 500) * 0.1)
  })

  // Scenario 2 — backward scroll (deltaY > 0) zooms out: max increases
  it('increases Y-axis max on Line chart when scrolling backward after a forward scroll', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const lineContainer = wrapper.find('[data-test="line-chart-container"]')

    // First, initialise scale with a forward scroll
    await lineContainer.trigger('wheel', { deltaY: -100 })
    await wrapper.vm.$nextTick()

    const lineStub = wrapper.findComponent({ name: 'Line' })
    const maxAfterZoomIn = (lineStub.props('options') as any).scales.y.max as number

    // Now scroll backward
    await lineContainer.trigger('wheel', { deltaY: 100 })
    await wrapper.vm.$nextTick()

    const maxAfterZoomOut = (lineStub.props('options') as any).scales.y.max as number
    expect(maxAfterZoomOut).toBeGreaterThan(maxAfterZoomIn)
  })

  // Scenario 6 — Line and Bar charts are independent
  it('does not affect Bar chart when scrolling over the Line chart', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const lineContainer = wrapper.find('[data-test="line-chart-container"]')
    await lineContainer.trigger('wheel', { deltaY: -100 })
    await wrapper.vm.$nextTick()

    const barStub = wrapper.findComponent({ name: 'Bar' })
    const barOpts = barStub.props('options') as any
    // Bar chart scales should remain auto (no custom min/max injected)
    expect(barOpts.scales.y.min).toBeUndefined()
    expect(barOpts.scales.y.max).toBeUndefined()
  })

  it('does not affect Line chart when scrolling over the Bar chart', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const barContainer = wrapper.find('[data-test="bar-chart-container"]')
    await barContainer.trigger('wheel', { deltaY: -100 })
    await wrapper.vm.$nextTick()

    const lineStub = wrapper.findComponent({ name: 'Line' })
    const lineOpts = lineStub.props('options') as any
    expect(lineOpts.scales.y.min).toBeUndefined()
    expect(lineOpts.scales.y.max).toBeUndefined()
  })

  // Scenario 4 — Scale resets when selected period changes
  it('resets Line chart Y-axis scale when the period changes', async () => {
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const lineContainer = wrapper.find('[data-test="line-chart-container"]')
    await lineContainer.trigger('wheel', { deltaY: -100 })
    await wrapper.vm.$nextTick()

    const lineStub = wrapper.findComponent({ name: 'Line' })
    expect((lineStub.props('options') as any).scales.y.max).toBeDefined()

    // Change the period
    const periodButtons = wrapper.findAll('button')
    const quarterBtn = periodButtons.find(b => b.text() === 'Trimestre')
    if (quarterBtn) await quarterBtn.trigger('click')
    await wrapper.vm.$nextTick()

    const optsAfterReset = lineStub.props('options') as any
    expect(optsAfterReset.scales.y.min).toBeUndefined()
    expect(optsAfterReset.scales.y.max).toBeUndefined()
  })

  // Scenario 7 — No crash when there is no chart data
  it('does not throw when scrolling over Line chart with no data', async () => {
    getDailyTrendStatsMock.mockResolvedValue({ dailyTrends: [] })
    const wrapper = mountDashboardPage()
    await settleDashboard()

    const lineContainer = wrapper.find('[data-test="line-chart-container"]')
    expect(async () => {
      await lineContainer.trigger('wheel', { deltaY: -100 })
      await wrapper.vm.$nextTick()
    }).not.toThrow()

    // Options should remain with no custom scale (auto)
    const lineStub = wrapper.findComponent({ name: 'Line' })
    const opts = lineStub.props('options') as any
    expect(opts.scales.y.min).toBeUndefined()
    expect(opts.scales.y.max).toBeUndefined()
  })
})
