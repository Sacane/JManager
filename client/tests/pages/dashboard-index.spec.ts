import { shallowMount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import DashboardPage from '../../pages/dashboard/index.vue'

vi.mock('@vueuse/core', () => ({
  useIntersectionObserver: vi.fn(),
}))

vi.mock('vue-chartjs', () => ({
  Bar: { template: '<div class="bar-chart" />' },
  Doughnut: { template: '<div class="doughnut-chart" />' },
  Line: { template: '<div class="line-chart" />' },
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
      startDate: '2026-03-28',
      endDate: '2026-04-27',
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
      startDate: '2026-03-28',
      endDate: '2026-04-30',
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
