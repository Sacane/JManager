import { readFileSync } from 'node:fs'
import { resolve as resolvePath } from 'node:path'
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

const DASHBOARD_SOURCE = readFileSync(resolvePath(process.cwd(), 'pages/index.vue'), 'utf8')

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

describe('pages/index design system', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.useFakeTimers({ toFake: ['Date'] })
    vi.setSystemTime(new Date(2026, 8, 18, 12, 0, 0))
  })

  afterEach(() => vi.useRealTimers())

  // Read from the source rather than the rendered markup: half of this page sits behind a v-if,
  // so a rendered-only check would let unmigrated blocks through. `:style` bindings are excluded
  // by requiring a whitespace before the attribute name.
  it('sets no design token through a static style attribute', () => {
    const staticStyles = [...DASHBOARD_SOURCE.matchAll(/\sstyle="([^"]*)"/g)].map(match => match[1])

    expect(staticStyles.filter(style => style.includes('var(--'))).toEqual([])
  })

  // The tag chip is coloured with the colour the user picked for that tag: it cannot become a class.
  it('keeps the tag colours as style bindings', () => {
    expect(DASHBOARD_SOURCE).toContain(':style="{ color: toReadableTagTextColor(tag.colorDTO) }"')
    expect(DASHBOARD_SOURCE).toMatch(/:style="\{ backgroundColor: `rgb\(/)
  })

  it('gives every heading one design system role', async () => {
    const ROLES = ['block-title', 'kpi-label', 'text-label-strong']
    const wrapper = mountDashboard()
    await settle()

    const headings = wrapper.findAll('h3')
    expect(headings.length).toBeGreaterThanOrEqual(8)

    const withoutOneRole = headings
      .map(heading => ({ text: heading.text(), classes: heading.classes() }))
      .filter(heading => heading.classes.filter(role => ROLES.includes(role)).length !== 1)

    expect(withoutOneRole).toEqual([])
  })

  it('names the repeated figure and panel patterns', async () => {
    const wrapper = mountDashboard()
    await settle()

    expect(wrapper.findAll('.kpi-value').length).toBeGreaterThanOrEqual(4)
    expect(wrapper.findAll('.kpi-hint').length).toBeGreaterThanOrEqual(4)
    expect(wrapper.findAll('.panel-sunken').length).toBeGreaterThanOrEqual(3)
    expect(wrapper.findAll('.text-note').length).toBeGreaterThanOrEqual(2)
  })
})
