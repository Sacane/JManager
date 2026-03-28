import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
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
    id: 1,
    amount: 1200,
    labelAccount: 'Compte principal',
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

const categoryPrevious = {
  categories: [
    { tagLabel: 'Courses', tagId: 'tag-1', totalAmount: '100.00', percentage: 40, transactionCount: 1 },
    { tagLabel: 'Transport', tagId: 'tag-2', totalAmount: '120.00', percentage: 48, transactionCount: 1 },
  ],
  totalExpenses: '300.00',
}

const getCategoryDistributionMock = vi
  .fn()
  .mockResolvedValueOnce(categoryCurrent)
  .mockResolvedValueOnce(categoryPrevious)

const getTrendStatsMock = vi.fn().mockResolvedValue({
  monthlyTrends: [],
})

const getPrevisionalTransactionsMock = vi.fn().mockResolvedValue({
  transactions: [],
  groupedByAccount: {},
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
  accountCycles: [
    {
      accountId: '1',
      label: 'Compte principal',
      monthlyPeriodStartDay: 1,
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
  return new Promise(resolve => setTimeout(resolve, 0))
}

function mountDashboardPage() {
  vi.stubGlobal('definePageMeta', vi.fn())
  vi.stubGlobal('navigateTo', vi.fn())
  vi.stubGlobal('useBooklet', () => ({
    createAccount: vi.fn(),
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
  it('renders top tags section with variation label', async () => {
    const wrapper = mountDashboardPage()

    await flushPromises()
    await flushPromises()

    expect(wrapper.text()).toContain('Top tags de la période')
    expect(wrapper.text()).toContain('Variation vs période précédente')
    expect(wrapper.text()).toContain('Courses')
    expect(wrapper.text()).toContain('Alertes de la période')
    expect(wrapper.text()).toContain('Actions rapides')
    expect(wrapper.text()).toContain('Aucun mouvement à venir')
    expect(wrapper.text()).toContain('Projection fin de période')
    expect(wrapper.text()).toContain('Budget du compte')
    expect(wrapper.text()).toContain('Définis une cible pour activer les alertes budget.')
  })
})
