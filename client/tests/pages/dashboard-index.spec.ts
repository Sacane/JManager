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
    { tagLabel: 'Courses', tagId: 'tag-1', totalAmount: '200.00', percentage: 50, transactionCount: 2 },
    { tagLabel: 'Transport', tagId: 'tag-2', totalAmount: '100.00', percentage: 25, transactionCount: 1 },
  ],
  totalExpenses: '400.00',
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
