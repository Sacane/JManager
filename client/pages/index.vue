<script setup lang="ts">
import type { AxiosError } from 'axios'
import type { Ref } from 'vue'
import { useIntersectionObserver } from '@vueuse/core'
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Filler,
  Legend,
  LinearScale,
  LineElement,
  PointElement,
  Title,
  Tooltip,
} from 'chart.js'
import { addDays, addMonths, endOfMonth, format, isAfter, startOfMonth, subMonths } from 'date-fns'
import { fr } from 'date-fns/locale'
import { nextTick, onBeforeUnmount } from 'vue'
import { Bar, Doughnut, Line } from 'vue-chartjs'
import useAuth from '@/composables/useAuth'
import BookletBookingDialog from '~/components/dialog/BookletBookingDialog.vue'
import useStats from '~/composables/useStats'
import useUserSettings from '~/composables/useUserSettings'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import authMiddleware from '~/middleware/auth'
import { countDaysInRange, resolveMonthlyCycleRangeForTargetMonth, resolveMonthlyCycleRangeFromAnchor } from '~/utils/monthlyCycleRange'
import { capitalizeFirst, toReadableTagTextColor } from '~/utils/util'

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  ArcElement,
  Title,
  Tooltip,
  Legend,
  Filler,
)

definePageMeta({
  layout: 'sidebar-layout',
  middleware: [authMiddleware],
})

const { user } = useAuth()
const { createBooklet, fetch: fetchBooklets } = useBooklet()
const { getRegularTransaction, saveMonthlyTransaction } = useRegularTransaction()
const { saveTransaction } = useTransaction()
const { getAllTags } = useTag()
const { getCategoryDistribution, getTrendStats, getPrevisionalTransactions, getDailyTrendStats } = useStats()
const { getSettings: getUserSettings } = useUserSettings()
const { isScopeLoading, withLoading } = useLoading()
const toast = useJToast()
const BUDGET_STORAGE_KEY = 'dashboard.budgetTargetsByBooklet.v1'
// Persists the selected booklet across navigation (e.g. leaving the dashboard for another
// page/tab and coming back) so the user doesn't have to reselect it every time.
const SELECTED_BOOKLET_STORAGE_KEY = 'dashboard.selectedBookletId.v1'

// Refs
const isBookletDialogOpen = ref(false)
const booklets = ref<BookletDTO[]>([])

// Only the order is used here: the drag and drop lived in the "Mes livrets" block, removed by UX-18
// since the booklets page already offers it.
const { orderedItems: orderedBooklets } = useBookletOrder(booklets)
const regularTransactions = ref<RegularTransactionDTO[]>([])
const tags = ref<TagDTO[]>([])
const categoryDistribution = ref<CategoryDistributionDTO | null>(null)
const previousCategoryDistribution = ref<CategoryDistributionDTO | null>(null)
const trendStats = ref<TrendStatsDTO | null>(null)
const previousTrendStats = ref<TrendStatsDTO | null>(null)
const evolutionTrendStats = ref<TrendStatsDTO | null>(null)
const dailyTrendStats = ref<DailyTrendStatsDTO | null>(null)
const previsionalTransactions = ref<PrevisionalTransactionsDTO | null>(null)
const periodProjectionTransactions = ref<PrevisionalTransactionsDTO | null>(null)
const selectedBookletId = useLocalStorage<string | number | null>(SELECTED_BOOKLET_STORAGE_KEY, null)

/**
 * Sentinel stored in place of a booklet id when every booklet is aggregated (UX-44).
 *
 * The stats endpoints aggregate every booklet when no id is sent. Cycles are per booklet, so this
 * mode has no cycle of its own and uses the calendar month — a decision taken on 19/09/2026.
 * The per-booklet budget and the quick actions needing one target booklet are hidden in it.
 */
const ALL_BOOKLETS = 'all'
const isAllBooklets = computed(() => selectedBookletId.value === ALL_BOOKLETS)
// With a single booklet, "every booklet" is that booklet under a different period: nothing to offer.
const hasSeveralBooklets = computed(() => booklets.value.length > 1)
const selectedPeriod = ref<'month' | 'quarter' | 'year'>('month')
const periodAnchorDate = ref(new Date())
const hasInitializedDashboard = ref(false)
const budgetTargetsByBooklet = ref<Record<string, number>>({})
const budgetTargetInput = ref<number | undefined>(undefined)
const projectionWindowDays = ref(15)
const bookletMonthlyCycleById = ref<Record<string, { startDay: number, endDay: number | null }>>({})
const dashboardLoadingScope = LOADING_SCOPES.dashboard.initial
const isLoading = computed(() => isScopeLoading(dashboardLoadingScope))

// Guarded on the loaded state, not on the list alone: keying off booklets.length would flash the
// onboarding screen on the first paint, before the fetch resolves (UX-43).
const hasNoBooklet = computed(() => hasInitializedDashboard.value && booklets.value.length === 0)

// Animation refs
const overviewRef = ref(null)
const chartsRef = ref(null)

/** Overview in three zones, or the secondary analysis (UX-18). */
type DashboardTab = 'overview' | 'analysis'
const DASHBOARD_TABS: DashboardTab[] = ['overview', 'analysis']
const activeDashboardTab = ref<DashboardTab>('overview')
const dashboardTabRefs = ref<Record<DashboardTab, HTMLButtonElement | null>>({ overview: null, analysis: null })

/**
 * Arrow keys move between tabs, as the tab role promises a screen reader user: only the active tab
 * sits in the Tab order, and focus follows the selection.
 */
function onDashboardTabKeydown(event: KeyboardEvent) {
  if (event.key !== 'ArrowRight' && event.key !== 'ArrowLeft') return
  event.preventDefault()
  const step = event.key === 'ArrowRight' ? 1 : -1
  const index = DASHBOARD_TABS.indexOf(activeDashboardTab.value)
  const next = DASHBOARD_TABS[(index + step + DASHBOARD_TABS.length) % DASHBOARD_TABS.length]
  activeDashboardTab.value = next
  nextTick(() => dashboardTabRefs.value[next]?.focus())
}
const isOverviewVisible = ref(false)
const isChartsVisible = ref(false)

// Doughnut slice toggle state
const selectedSliceIndex = ref<number | null>(null)
const sliceDisplayMode = ref<'amount' | 'percentage'>('amount')
const selectedParentCategoryIndex = ref<number | null>(null)
const hiddenDoughnutIndices = ref(new Set<number>())

// Y-axis scale overrides (null = auto-scale, non-null = custom bounds)
const lineChartYMin = ref<number | null>(null)
const lineChartYMax = ref<number | null>(null)
const barChartYMin = ref<number | null>(null)
const barChartYMax = ref<number | null>(null)

// Setup intersection observers
useIntersectionObserver(overviewRef, ([entry]) => {
  if (entry?.isIntersecting) {
    isOverviewVisible.value = true
  }
}, { threshold: 0.1 })

useIntersectionObserver(chartsRef, ([entry]) => {
  if (entry?.isIntersecting) {
    isChartsVisible.value = true
  }
}, { threshold: 0.1 })

// Computed values
const totalBalance = computed(() =>
  booklets.value.reduce((acc, curr) => acc + Number.parseFloat(curr.amount.toString()), 0.00),
)

const selectedBooklet = computed(() =>
  booklets.value.find(booklet => booklet.id === selectedBookletId.value) ?? null,
)

const scopedBookletId = computed(() => {
  // No id is what makes the stats endpoints aggregate every booklet. Stated rather than left to the
  // UUID check below, which would reject the sentinel only by accident.
  if (selectedBookletId.value === null || isAllBooklets.value) {
    return undefined
  }

  const raw = String(selectedBookletId.value)
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
  return uuidRegex.test(raw) ? raw : undefined
})

const selectedBookletBalance = computed(() => {
  if (!selectedBooklet.value) {
    return totalBalance.value
  }
  return Number.parseFloat(selectedBooklet.value.amount.toString())
})

// A start on the 1st with no custom end is the calendar month, which the aggregated mode uses on
// purpose: it has no cycle of its own. Stated rather than obtained by the cycle lookup missing.
const selectedMonthlyPeriodStartDay = computed(() => {
  if (!selectedBookletId.value || isAllBooklets.value) {
    return 1
  }

  const configured = bookletMonthlyCycleById.value[String(selectedBookletId.value)]?.startDay
  if (!configured) {
    return 1
  }

  return Math.min(31, Math.max(1, Math.trunc(configured)))
})

const selectedMonthlyPeriodEndDay = computed(() => {
  if (!selectedBookletId.value || isAllBooklets.value) {
    return null
  }

  const configured = bookletMonthlyCycleById.value[String(selectedBookletId.value)]?.endDay
  if (configured === null || configured === undefined) {
    return null
  }

  return Math.min(31, Math.max(1, Math.trunc(configured)))
})

const projectionWindowLabel = computed(() => `${projectionWindowDays.value} jours`)

function normalizeMonthlyPeriodEndDay(value: number | null | undefined): number | null {
  if (value === null || value === undefined || Number.isNaN(value)) {
    return null
  }

  return Math.min(31, Math.max(1, Math.trunc(value)))
}

function resolveCustomMonthlyRange(anchorDate: Date, cycleStartDay: number, cycleEndDay: number | null) {
  return resolveMonthlyCycleRangeFromAnchor(anchorDate, cycleStartDay, cycleEndDay)
}

function resolvePreviousCustomMonthlyRange(startDate: Date, cycleStartDay: number, cycleEndDay: number | null) {
  const dayBeforeCurrentRange = addDays(startDate, -1)
  return resolveCustomMonthlyRange(dayBeforeCurrentRange, cycleStartDay, cycleEndDay)
}

function normalizeProjectionWindowDays(value: number | undefined): number {
  if (value === undefined || Number.isNaN(value)) {
    return 15
  }

  return Math.min(60, Math.max(7, Math.trunc(value)))
}

const selectedPeriodLabel = computed(() => {
  if (selectedPeriod.value === 'month') {
    return format(periodAnchorDate.value, 'MMMM yyyy', { locale: fr })
  }
  if (selectedPeriod.value === 'quarter') {
    const start = subMonths(periodAnchorDate.value, 2)
    return `${format(start, 'MMM', { locale: fr })} - ${format(periodAnchorDate.value, 'MMM yyyy', { locale: fr })}`
  }
  const yearStart = subMonths(periodAnchorDate.value, 11)
  return `${format(yearStart, 'MMM yyyy', { locale: fr })} - ${format(periodAnchorDate.value, 'MMM yyyy', { locale: fr })}`
})

const periodMetricLabel = computed(() =>
  selectedPeriod.value === 'month' ? 'du mois' : 'de la période',
)

const currentDateRange = computed(() => {
  if (selectedPeriod.value === 'month') {
    return resolveMonthlyCycleRangeForTargetMonth(
      periodAnchorDate.value.getFullYear(),
      periodAnchorDate.value.getMonth() + 1,
      selectedMonthlyPeriodStartDay.value,
      selectedMonthlyPeriodEndDay.value,
    )
  }

  if (selectedPeriod.value === 'quarter') {
    return {
      start: startOfMonth(subMonths(periodAnchorDate.value, 2)),
      end: endOfMonth(periodAnchorDate.value),
    }
  }

  return {
    start: startOfMonth(subMonths(periodAnchorDate.value, 11)),
    end: endOfMonth(periodAnchorDate.value),
  }
})

const previousDateRange = computed(() => {
  if (selectedPeriod.value === 'month') {
    return resolvePreviousCustomMonthlyRange(
      currentDateRange.value.start,
      selectedMonthlyPeriodStartDay.value,
      selectedMonthlyPeriodEndDay.value,
    )
  }

  if (selectedPeriod.value === 'quarter') {
    return {
      start: startOfMonth(subMonths(periodAnchorDate.value, 5)),
      end: endOfMonth(subMonths(periodAnchorDate.value, 3)),
    }
  }

  return {
    start: startOfMonth(subMonths(periodAnchorDate.value, 23)),
    end: endOfMonth(subMonths(periodAnchorDate.value, 12)),
  }
})

const evolutionDateRange = computed(() => {
  if (selectedPeriod.value === 'month') {
    // For month, daily trends are used instead of multi-month evolution
    return currentDateRange.value
  }

  return {
    start: currentDateRange.value.start,
    end: currentDateRange.value.end,
  }
})

const currentDateRangeLabel = computed(() =>
  `${format(currentDateRange.value.start, 'dd MMM', { locale: fr })} - ${format(currentDateRange.value.end, 'dd MMM yyyy', { locale: fr })}`,
)

const periodExpenses = computed(() => {
  if (!trendStats.value?.monthlyTrends.length) {
    return 0
  }
  return trendStats.value.monthlyTrends.reduce(
    (acc, trend) => acc + Number.parseFloat(trend.expenses),
    0,
  )
})

// Chart.js needs resolved colour values, so the money tokens cannot be handed to it as
// `var(--income)`. Reading them from the document keeps the datasets on the same colour code
// as the rest of the UI, and depending on the active scheme re-resolves them when the theme
// changes — until now the chart colours were hardcoded and identical in both themes.
const { value: activeColorScheme } = useDark()

function cssColor(name: string, fallback: string): string {
  if (typeof window === 'undefined') return fallback
  const resolved = getComputedStyle(document.documentElement).getPropertyValue(name).trim()
  return resolved || fallback
}

const chartPalette = computed(() => {
  void activeColorScheme.value
  return {
    income: cssColor('--income', '#047857'),
    incomeSoft: cssColor('--income-soft', 'rgba(4, 120, 87, 0.12)'),
    expense: cssColor('--expense', '#BF0638'),
    expenseSoft: cssColor('--expense-soft', 'rgba(191, 6, 56, 0.12)'),
  }
})

const currentPeriodDayCount = computed(() => countDaysInRange(currentDateRange.value))

// The average must follow the selected period: a quarter and a year do not span 30 days.
const dailyExpenseAverage = computed(() =>
  currentPeriodDayCount.value > 0 ? periodExpenses.value / currentPeriodDayCount.value : 0,
)

const periodIncome = computed(() => {
  if (!trendStats.value?.monthlyTrends.length) {
    return 0
  }
  return trendStats.value.monthlyTrends.reduce(
    (acc, trend) => acc + Number.parseFloat(trend.income),
    0,
  )
})

const previousPeriodExpenses = computed(() => {
  if (!previousTrendStats.value?.monthlyTrends.length) {
    return 0
  }
  return previousTrendStats.value.monthlyTrends.reduce(
    (acc, trend) => acc + Number.parseFloat(trend.expenses),
    0,
  )
})

const previousPeriodIncome = computed(() => {
  if (!previousTrendStats.value?.monthlyTrends.length) {
    return 0
  }
  return previousTrendStats.value.monthlyTrends.reduce(
    (acc, trend) => acc + Number.parseFloat(trend.income),
    0,
  )
})

const expensesGrowth = computed(() => {
  if (previousPeriodExpenses.value === 0) {
    return 0
  }

  return ((periodExpenses.value - previousPeriodExpenses.value) / previousPeriodExpenses.value * 100)
})

const incomeGrowth = computed(() => {
  if (previousPeriodIncome.value === 0) {
    return 0
  }

  return ((periodIncome.value - previousPeriodIncome.value) / previousPeriodIncome.value * 100)
})

const balanceGrowth = computed(() => {
  const currentBalance = periodIncome.value - periodExpenses.value
  const previousBalance = previousPeriodIncome.value - previousPeriodExpenses.value

  if (previousBalance === 0) {
    return 0
  }

  return ((currentBalance - previousBalance) / Math.abs(previousBalance) * 100)
})

const savingsRate = computed(() => {
  if (periodIncome.value === 0 || periodExpenses.value > periodIncome.value) {
    return 0
  }

  return ((periodIncome.value - periodExpenses.value) / periodIncome.value * 100)
})

const upcomingRegularPayments = computed(() =>
  previsionalTransactions.value?.regularTransactions.slice(0, 5) || [],
)

const upcomingNonRegularPayments = computed(() =>
  previsionalTransactions.value?.nonRegularTransactions.slice(0, 5) || [],
)

const totalPrevisionalTransactions = computed(() =>
  previsionalTransactions.value?.transactions.length || 0,
)

const totalRegularUpcoming = computed(() =>
  Number.parseFloat(previsionalTransactions.value?.totalRegularAmount || '0'),
)

const totalNonRegularUpcoming = computed(() =>
  Number.parseFloat(previsionalTransactions.value?.totalNonRegularAmount || '0'),
)

const totalUpcomingNet = computed(() => totalRegularUpcoming.value + totalNonRegularUpcoming.value)

const selectedBookletBudgetKey = computed(() => {
  if (selectedBookletId.value === null || selectedBookletId.value === undefined) {
    return null
  }

  return String(selectedBookletId.value)
})

const selectedBudgetTarget = computed(() => {
  if (!selectedBookletBudgetKey.value) {
    return 0
  }

  return budgetTargetsByBooklet.value[selectedBookletBudgetKey.value] ?? 0
})

const isBudgetConfigured = computed(() => selectedBudgetTarget.value > 0)

const projectedRemainingExpenses = computed(() =>
  Number.parseFloat(periodProjectionTransactions.value?.totalExpenses || '0'),
)

const projectedPeriodExpenses = computed(() =>
  periodExpenses.value + projectedRemainingExpenses.value,
)

const budgetDelta = computed(() => selectedBudgetTarget.value - periodExpenses.value)

const projectedBudgetDelta = computed(() => selectedBudgetTarget.value - projectedPeriodExpenses.value)

const budgetConsumptionRate = computed(() => {
  if (!isBudgetConfigured.value) {
    return 0
  }

  return (periodExpenses.value / selectedBudgetTarget.value) * 100
})

const projectionPeriodEnded = computed(() => isAfter(new Date(), currentDateRange.value.end))

const periodProjectionNet = computed(() => {
  if (!periodProjectionTransactions.value) {
    return 0
  }

  return Number.parseFloat(periodProjectionTransactions.value.totalAmount || '0')
})

const projectedEndPeriodBalance = computed(() =>
  selectedBookletBalance.value + periodProjectionNet.value,
)

const dashboardAlerts = computed(() => {
  const alerts: Array<{ key: string, level: 'danger' | 'warning' | 'info', title: string, detail: string }> = []

  if (periodExpenses.value > periodIncome.value && periodIncome.value > 0) {
    alerts.push({
      key: 'overspending',
      level: 'danger',
      title: 'Dépenses supérieures aux revenus',
      detail: `Le déficit de la période est de ${(periodExpenses.value - periodIncome.value).toFixed(2)} €`,
    })
  }

  if (totalUpcomingNet.value < 0) {
    alerts.push({
      key: 'upcoming-negative',
      level: 'warning',
      title: `Fenêtre ${projectionWindowDays.value} jours négative`,
      detail: `Impact prévisionnel: ${totalUpcomingNet.value.toFixed(2)} €`,
    })
  }

  if (totalPrevisionalTransactions.value === 0) {
    alerts.push({
      key: 'no-upcoming',
      level: 'info',
      title: 'Aucun mouvement à venir',
      detail: `Aucune transaction prévue dans les ${projectionWindowDays.value} prochains jours`,
    })
  }

  if (isBudgetConfigured.value && projectedBudgetDelta.value < 0) {
    alerts.push({
      key: 'budget-overrun',
      level: 'warning',
      title: 'Budget projeté dépassé',
      detail: `Dépassement estimé: ${Math.abs(projectedBudgetDelta.value).toFixed(2)} €`,
    })
  }

  return alerts.slice(0, 3)
})

// Chart data
const expensesTrendData = computed(() => {
  if (selectedPeriod.value === 'month') {
    // Daily granularity for month view
    if (!dailyTrendStats.value?.dailyTrends.length) {
      return { labels: [], datasets: [] }
    }

    const trends = dailyTrendStats.value.dailyTrends
    const labels = trends.map((trend) => {
      const [year, month, day] = trend.date.split('-').map(Number)
      const d = new Date(year, month - 1, day)
      return format(d, 'dd MMM', { locale: fr })
    })

    return {
      labels,
      datasets: [
        {
          label: 'Dépenses',
          data: trends.map(t => Number.parseFloat(t.expenses)),
          borderColor: chartPalette.value.expense,
          backgroundColor: chartPalette.value.expenseSoft,
          tension: 0.4,
          fill: true,
        },
        {
          label: 'Revenus',
          data: trends.map(t => Number.parseFloat(t.income)),
          borderColor: chartPalette.value.income,
          backgroundColor: chartPalette.value.incomeSoft,
          tension: 0.4,
          fill: true,
        },
        {
          label: 'Solde cumulé',
          data: trends.map(t => Number.parseFloat(t.cumulativeBalance)),
          borderColor: '#8b5cf6',
          backgroundColor: 'rgba(139, 92, 246, 0.05)',
          tension: 0.4,
          fill: false,
          borderDash: [5, 5],
        },
      ],
    }
  }

  // Monthly granularity for quarter/year
  if (!evolutionTrendStats.value?.monthlyTrends.length) {
    return { labels: [], datasets: [] }
  }

  const sortedTrends = evolutionTrendStats.value.monthlyTrends.toSorted((a, b) => {
    if (a.year !== b.year) {
      return a.year - b.year
    }
    return a.month - b.month
  })

  const labels = sortedTrends.map((trend) => {
    const date = new Date(trend.year, trend.month - 1)
    return format(date, 'MMM', { locale: fr })
  })

  return {
    labels,
    datasets: [
      {
        label: 'Dépenses',
        data: sortedTrends.map(trend => Number.parseFloat(trend.expenses)),
        borderColor: chartPalette.value.expense,
        backgroundColor: chartPalette.value.expenseSoft,
        tension: 0.4,
        fill: true,
      },
      {
        label: 'Revenus',
        data: sortedTrends.map(trend => Number.parseFloat(trend.income)),
        borderColor: chartPalette.value.income,
        backgroundColor: chartPalette.value.incomeSoft,
        tension: 0.4,
        fill: true,
      },
    ],
  }
})

// Normalise raw categories from the API so that promoted sub-tags (i.e. sub-tags whose
// parent has no direct transactions, which the backend returns as top-level entries) are
// rolled back up under their parent tag. The result always contains only root-level tags,
// each one carrying its sub-tags in `subCategories`.
function normalizeCategories(raw: CategoryDataDTO[]): CategoryDataDTO[] {
  if (!raw.length) return []

  const tagById = new Map(tags.value.map(t => [t.tagId, t]))

  const rootMap = new Map<string, CategoryDataDTO>()
  const promotedSubs: CategoryDataDTO[] = []

  for (const cat of raw) {
    const tagInfo = cat.tagId ? tagById.get(cat.tagId) : undefined
    if (tagInfo?.parentId) {
      // The backend promoted this sub-tag to the top level — defer it for roll-up
      promotedSubs.push(cat)
    } else {
      rootMap.set(cat.tagId ?? cat.tagLabel, cat)
    }
  }

  for (const sub of promotedSubs) {
    const tagInfo = tagById.get(sub.tagId!)
    if (!tagInfo?.parentId) continue
    const parentTag = tagById.get(tagInfo.parentId)
    if (!parentTag) continue

    const parentKey = tagInfo.parentId
    const existing = rootMap.get(parentKey)
    if (existing) {
      // Parent already in root map (shouldn't happen since backend groups by parent, but handle gracefully)
      const newTotal = (Number.parseFloat(existing.totalAmount) + Number.parseFloat(sub.totalAmount)).toFixed(2)
      rootMap.set(parentKey, {
        ...existing,
        totalAmount: newTotal,
        percentage: existing.percentage + sub.percentage,
        transactionCount: existing.transactionCount + sub.transactionCount,
        subCategories: [...(existing.subCategories ?? []), sub],
      })
    } else {
      rootMap.set(parentKey, {
        tagLabel: parentTag.label ?? '',
        tagId: tagInfo.parentId,
        colorDTO: parentTag.colorDTO,
        totalAmount: sub.totalAmount,
        percentage: sub.percentage,
        transactionCount: sub.transactionCount,
        subCategories: [sub],
      })
    }
  }

  return [...rootMap.values()].toSorted(
    (a, b) => Number.parseFloat(b.totalAmount) - Number.parseFloat(a.totalAmount),
  )
}

const normalizedCategories = computed(() =>
  normalizeCategories(categoryDistribution.value?.categories ?? []),
)

const normalizedPreviousCategories = computed(() =>
  normalizeCategories(previousCategoryDistribution.value?.categories ?? []),
)

const categoryExpensesData = computed(() => {
  const categories = normalizedCategories.value
  if (!categories.length) {
    return {
      labels: [],
      datasets: [{ data: [], backgroundColor: [], borderWidth: 0 }],
    }
  }

  return {
    labels: categories.map(cat => cat.tagLabel),
    datasets: [
      {
        data: categories.map(cat => Number.parseFloat(cat.totalAmount)),
        backgroundColor: categories.map(cat =>
          `rgb(${cat.colorDTO.red}, ${cat.colorDTO.green}, ${cat.colorDTO.blue})`,
        ),
        borderWidth: 0,
      },
    ],
  }
})

const doughnutCenterLabel = computed(() => {
  if (selectedSliceIndex.value === null) {
    return null
  }

  const data = categoryExpensesData.value.datasets[0]?.data ?? []
  const value = data[selectedSliceIndex.value]
  if (value === undefined) {
    return null
  }

  if (sliceDisplayMode.value === 'percentage') {
    const total = data.reduce((a: number, b: number, i: number) =>
      hiddenDoughnutIndices.value.has(i) ? a : a + b, 0)
    const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0.0'
    return `${percentage}%`
  }

  return `${value.toFixed(2)} €`
})

function onDoughnutClick(_event: any, elements: any[]) {
  if (!elements.length) {
    return
  }

  const clickedIndex = elements[0].index
  const clickedCategory = normalizedCategories.value[clickedIndex]

  // Handle secondary chart toggle for parent categories with sub-tags
  if (clickedCategory?.subCategories?.length) {
    if (selectedParentCategoryIndex.value === clickedIndex) {
      selectedParentCategoryIndex.value = null
    } else {
      selectedParentCategoryIndex.value = clickedIndex
    }
  } else {
    selectedParentCategoryIndex.value = null
  }

  // Handle center label toggle (existing behavior)
  if (selectedSliceIndex.value === clickedIndex) {
    sliceDisplayMode.value = sliceDisplayMode.value === 'amount' ? 'percentage' : 'amount'
  } else {
    selectedSliceIndex.value = clickedIndex
    sliceDisplayMode.value = 'amount'
  }
}

const selectedParentCategory = computed(() => {
  if (selectedParentCategoryIndex.value === null) return null
  return normalizedCategories.value[selectedParentCategoryIndex.value] ?? null
})

const secondaryChartData = computed(() => {
  const parent = selectedParentCategory.value
  if (!parent?.subCategories?.length) {
    return null
  }

  const subs = parent.subCategories
  return {
    labels: subs.map(s => s.tagLabel),
    datasets: [
      {
        data: subs.map(s => Number.parseFloat(s.totalAmount)),
        backgroundColor: subs.map(s =>
          `rgb(${s.colorDTO.red}, ${s.colorDTO.green}, ${s.colorDTO.blue})`,
        ),
        borderWidth: 0,
      },
    ],
  }
})

// Responsive behavior for legends and chart sizing
const isSmallScreen = ref(false)

const secondaryDoughnutOptions = computed(() => ({
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'bottom' as const,
      labels: {
        padding: 10,
        usePointStyle: true,
        font: {
          size: isSmallScreen.value ? 10 : 11,
        },
      },
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      padding: 10,
      borderRadius: 8,
      callbacks: {
        label: (context: any) => {
          const label = context.label || ''
          const value = context.parsed || 0
          const total = context.dataset.data.reduce((a: number, b: number, i: number) =>
            context.chart?.getDataVisibility(i) === false ? a : a + b, 0)
          const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0
          return `${label}: ${value.toFixed(2)} € (${percentage}%)`
        },
      },
    },
  },
  cutout: '60%',
}))

const topTagsInsights = computed(() => {
  const currentCategories = normalizedCategories.value
  if (currentCategories.length === 0) {
    return []
  }

  const previousMap = new Map(
    normalizedPreviousCategories.value.map(category => [
      category.tagId ?? category.tagLabel,
      Number.parseFloat(category.totalAmount),
    ]),
  )

  return currentCategories
    .map((category) => {
      const currentAmount = Number.parseFloat(category.totalAmount)
      const previousAmount = previousMap.get(category.tagId ?? category.tagLabel) ?? 0
      const variation = previousAmount === 0
        ? null
        : ((currentAmount - previousAmount) / previousAmount) * 100

      return {
        tagLabel: category.tagLabel,
        currentAmount,
        percentage: category.percentage,
        variation,
        colorDTO: category.colorDTO,
      }
    })
})

const monthlyComparisonData = computed(() => {
  const currentBalance = periodIncome.value - periodExpenses.value
  const previousBalance = previousPeriodIncome.value - previousPeriodExpenses.value

  return {
    labels: ['Revenus', 'Dépenses', 'Solde net'],
    datasets: [
      {
        label: 'Période active',
        data: [periodIncome.value, periodExpenses.value, currentBalance],
        backgroundColor: '#6508CC',
        borderRadius: 8,
      },
      {
        label: 'Période précédente',
        data: [previousPeriodIncome.value, previousPeriodExpenses.value, previousBalance],
        backgroundColor: '#b1aeae',
        borderRadius: 8,
      },
    ],
  }
})

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'bottom' as const,
      labels: {
        padding: 15,
        usePointStyle: true,
        font: {
          size: 12,
        },
      },
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      padding: 12,
      borderRadius: 8,
      titleFont: {
        size: 14,
      },
      bodyFont: {
        size: 13,
      },
    },
  },
  scales: {
    y: {
      beginAtZero: true,
      grid: {
        color: 'rgba(0, 0, 0, 0.05)',
      },
    },
    x: {
      grid: {
        display: false,
      },
    },
  },
}

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'right' as const,
      labels: {
        padding: 15,
        usePointStyle: true,
        font: {
          size: 12,
        },
      },
    },
    tooltip: {
      backgroundColor: 'rgba(0, 0, 0, 0.8)',
      padding: 12,
      borderRadius: 8,
      callbacks: {
        label: (context: any) => {
          const label = context.label || ''
          const value = context.parsed || 0
          const total = context.dataset.data.reduce((a: number, b: number, i: number) =>
            hiddenDoughnutIndices.value.has(i) ? a : a + b, 0)
          const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0
          return `${label}: ${value.toFixed(2)} € (${percentage}%)`
        },
      },
    },
  },
  cutout: '65%',
}

function updateIsSmallScreen() {
  if (typeof window !== 'undefined') {
    isSmallScreen.value = window.innerWidth <= 640
  }
}

// computed options factory — injects custom Y-axis bounds when set
function makeChartOptions(yMin: Ref<number | null>, yMax: Ref<number | null>) {
  return computed(() => {
    const opts = JSON.parse(JSON.stringify(chartOptions))
    opts.plugins = opts.plugins || {}
    opts.plugins.legend = opts.plugins.legend || {}
    opts.plugins.legend.position = 'bottom'
    if (yMin.value !== null && yMax.value !== null) {
      opts.scales.y.min = yMin.value
      opts.scales.y.max = yMax.value
      opts.scales.y.beginAtZero = false
    }
    return opts
  })
}

const lineChartOptionsComputed = makeChartOptions(lineChartYMin, lineChartYMax)
const barChartOptionsComputed = makeChartOptions(barChartYMin, barChartYMax)

const doughnutOptionsComputed = computed(() => {
  return {
    ...doughnutOptions,
    plugins: {
      ...doughnutOptions.plugins,
      legend: {
        ...doughnutOptions.plugins.legend,
        position: 'bottom' as const,
        labels: {
          ...doughnutOptions.plugins.legend.labels,
          font: {
            ...doughnutOptions.plugins.legend.labels.font,
            size: isSmallScreen.value ? 11 : 12,
          },
        },
        onClick: (e: any, legendItem: any, legend: any) => {
          const index = legendItem.index as number
          const chart = legend.chart
          chart.toggleDataVisibility(index)
          chart.update()
          const newSet = new Set(hiddenDoughnutIndices.value)
          if (!chart.getDataVisibility(index)) {
            newSet.add(index)
          } else {
            newSet.delete(index)
          }
          hiddenDoughnutIndices.value = newSet
          if (selectedSliceIndex.value !== null && newSet.has(selectedSliceIndex.value)) {
            selectedSliceIndex.value = null
          }
        },
      },
    },
    onClick: onDoughnutClick,
  }
})

// --- Y-axis wheel zoom ---
// Multiplicative factor per wheel step: zoom-in shrinks range, zoom-out expands it.
// Using a factor means one zoom-in followed by one zoom-out returns to the exact same range.
const CHART_ZOOM_FACTOR = 0.8

function computeDataRange(chartData: { datasets: { data: number[] }[] }): { min: number, max: number } | null {
  const allValues = chartData.datasets.flatMap(ds => ds.data).filter(v => Number.isFinite(v))
  if (allValues.length === 0) return null
  const dataMin = Math.min(...allValues)
  const dataMax = Math.max(...allValues)
  if (dataMax <= dataMin) return null
  const padding = (dataMax - dataMin) * 0.1
  return { min: dataMin - padding, max: dataMax + padding }
}

function applyWheelToScale(
  yMin: Ref<number | null>,
  yMax: Ref<number | null>,
  chartData: { datasets: { data: number[] }[] },
  deltaY: number,
): void {
  const dataRange = computeDataRange(chartData)
  if (!dataRange) return

  if (yMin.value === null || yMax.value === null) {
    yMin.value = dataRange.min
    yMax.value = dataRange.max
  }

  const center = (yMin.value + yMax.value) / 2
  const halfRange = (yMax.value - yMin.value) / 2
  if (halfRange <= 0) return

  const zoomIn = deltaY < 0
  const factor = zoomIn ? CHART_ZOOM_FACTOR : (1 / CHART_ZOOM_FACTOR)
  const newHalfRange = halfRange * factor

  // Bounds: do not zoom in beyond 5% of the data span, nor zoom out beyond 4× the data span
  const dataSpan = dataRange.max - dataRange.min
  const minHalfRange = dataSpan * 0.025
  const maxHalfRange = dataSpan * 2

  const clampedHalfRange = Math.min(maxHalfRange, Math.max(minHalfRange, newHalfRange))

  yMin.value = center - clampedHalfRange
  yMax.value = center + clampedHalfRange
}

// The dashboard is several screens tall and the charts span its full width. Capturing a plain
// wheel would trap the page scroll, so zooming requires the same modifier browsers use for
// zoom, and the default scroll is only prevented when we actually handle the event (UX-08).
function isZoomGesture(event: WheelEvent): boolean {
  return event.ctrlKey || event.metaKey
}

function onLineChartWheel(event: WheelEvent): void {
  if (!isZoomGesture(event)) return
  event.preventDefault()
  applyWheelToScale(lineChartYMin, lineChartYMax, expensesTrendData.value, event.deltaY)
}

function onBarChartWheel(event: WheelEvent): void {
  if (!isZoomGesture(event)) return
  event.preventDefault()
  applyWheelToScale(barChartYMin, barChartYMax, monthlyComparisonData.value, event.deltaY)
}

const isLineChartScaled = computed(() => lineChartYMin.value !== null || lineChartYMax.value !== null)
const isBarChartScaled = computed(() => barChartYMin.value !== null || barChartYMax.value !== null)

function resetLineChartScale(): void {
  lineChartYMin.value = null
  lineChartYMax.value = null
}

function resetBarChartScale(): void {
  barChartYMin.value = null
  barChartYMax.value = null
}

if (typeof window !== 'undefined') {
  updateIsSmallScreen()
  window.addEventListener('resize', updateIsSmallScreen)
}

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', updateIsSmallScreen)
  }
})

// Functions
function handleBookletCreation(booklet: { label: string, digit: number }) {
  createBooklet(booklet.label, booklet.digit, '€')
    .then((acc) => {
      if (booklets.value.length < 10) {
        booklets.value.push(acc)
      }
      toast.success('Le compte a bien été créé')
      navigateTo(`/booklet/${acc.id}`)
    })
    .catch(err => toast.errorAxios(err))
}

function cancel() {
  isBookletDialogOpen.value = false
}

function loadBudgetTargets() {
  if (typeof window === 'undefined') {
    return
  }

  try {
    const rawValue = window.localStorage.getItem(BUDGET_STORAGE_KEY)
    if (!rawValue) {
      budgetTargetsByBooklet.value = {}
      return
    }

    const parsed = JSON.parse(rawValue)
    if (parsed && typeof parsed === 'object') {
      budgetTargetsByBooklet.value = parsed as Record<string, number>
    }
  } catch (error) {
    console.error('Unable to load budget targets', error)
    budgetTargetsByBooklet.value = {}
  }
}

function persistBudgetTargets() {
  if (typeof window === 'undefined') {
    return
  }

  window.localStorage.setItem(BUDGET_STORAGE_KEY, JSON.stringify(budgetTargetsByBooklet.value))
}

function syncBudgetInputFromSelection() {
  if (!selectedBookletBudgetKey.value) {
    budgetTargetInput.value = undefined
    return
  }

  const configuredBudget = budgetTargetsByBooklet.value[selectedBookletBudgetKey.value]
  budgetTargetInput.value = configuredBudget && configuredBudget > 0 ? configuredBudget : undefined
}

function saveBudgetTarget() {
  if (!selectedBookletBudgetKey.value) {
    return
  }

  const nextValue = Number(budgetTargetInput.value ?? 0)
  if (Number.isNaN(nextValue) || nextValue <= 0) {
    const { [selectedBookletBudgetKey.value]: _, ...remainingBudgets } = budgetTargetsByBooklet.value
    budgetTargetsByBooklet.value = remainingBudgets
    budgetTargetInput.value = undefined
    persistBudgetTargets()
    return
  }

  budgetTargetsByBooklet.value = {
    ...budgetTargetsByBooklet.value,
    [selectedBookletBudgetKey.value]: Number(nextValue.toFixed(2)),
  }
  budgetTargetInput.value = Number(nextValue.toFixed(2))
  persistBudgetTargets()
}

async function loadDashboardData() {
  await withLoading(async () => {
    try {
      // Load basic data
      const [bookletsData, regularTransData, tagsData, settingsData] = await Promise.all([
        fetchBooklets().catch(() => []),
        getRegularTransaction().catch(() => []),
        getAllTags().catch(() => []),
        getUserSettings().catch(() => null),
      ])

      booklets.value = Array.isArray(bookletsData) ? bookletsData : []
      regularTransactions.value = Array.isArray(regularTransData) ? regularTransData : []
      tags.value = Array.isArray(tagsData) ? tagsData : []

      if (settingsData) {
        projectionWindowDays.value = normalizeProjectionWindowDays(settingsData.projectionWindowDays)
        bookletMonthlyCycleById.value = Object.fromEntries(
          settingsData.bookletCycles.map((cycle: BookletMonthlyCycleDTO) => [
            cycle.bookletId,
            {
              startDay: Math.min(31, Math.max(1, Math.trunc(cycle.monthlyPeriodStartDay))),
              endDay: normalizeMonthlyPeriodEndDay(cycle.monthlyPeriodEndDay),
            },
          ]),
        )
      }

      // Keep the persisted selection only if it still points to an existing booklet
      // (e.g. it wasn't deleted since the last visit); otherwise fall back to the first one.
      // The aggregated mode stays valid while there is more than one booklet to aggregate.
      const persistedSelectionIsValid = selectedBookletId.value != null && (
        (isAllBooklets.value && hasSeveralBooklets.value)
        || orderedBooklets.value.some(booklet => booklet.id === selectedBookletId.value)
      )
      if (!persistedSelectionIsValid) {
        selectedBookletId.value = orderedBooklets.value[0]?.id ?? null
      }

      await loadStatsData()
      hasInitializedDashboard.value = true
    } catch (error) {
      toast.error('Erreur lors du chargement des données')
      console.error(error)
    }
  }, dashboardLoadingScope)
}

// ── Quick actions (UX-50) ─────────────────────────────────────────────────────
// The block used to hold three links to pages the sidebar already offers. It now opens the dialogs
// for what the dashboard lacked, on the booklet it shows, and reloads the figures after a success
// — a recorded expense that left every indicator unchanged would be the page lying.

const isQuickTransactionVisible = ref(false)
const isQuickTransactionSaving = ref(false)
const isQuickRegularVisible = ref(false)
const isQuickRegularSaving = ref(false)
const csvImportDialogRef = ref<{ openDialog: () => void } | null>(null)

function emptyQuickTransaction(): TransactionCreationDTO {
  return {
    id: null,
    label: '',
    value: null,
    isIncome: false,
    date: new Date(),
    tagDTO: { tagId: undefined, label: '', colorDTO: { red: 0, green: 0, blue: 0 }, isDefault: false },
    isPreview: false,
  }
}

const quickTransactionDraft = ref<TransactionCreationDTO>(emptyQuickTransaction())

function openQuickTransaction() {
  quickTransactionDraft.value = emptyQuickTransaction()
  isQuickTransactionVisible.value = true
}

async function recordQuickTransaction(transaction: TransactionCreationDTO) {
  const booklet = selectedBooklet.value
  if (!booklet) return

  isQuickTransactionSaving.value = true
  try {
    await saveTransaction(booklet.label, transaction)
    isQuickTransactionVisible.value = false
    toast.success(`Transaction enregistrée sur ${booklet.label}`)
    await loadDashboardData()
  } catch (error) {
    // The dialog stays open: closing it would throw away what the user typed.
    toast.errorAxios(error as AxiosError)
  } finally {
    isQuickTransactionSaving.value = false
  }
}

function openQuickImport() {
  csvImportDialogRef.value?.openDialog()
}

async function onQuickImportSuccess() {
  await loadDashboardData()
}

function openQuickRegular() {
  isQuickRegularVisible.value = true
}

async function recordQuickRegular(entry: MonthlyTransactionCreationRequest) {
  isQuickRegularSaving.value = true
  try {
    await saveMonthlyTransaction(entry)
    isQuickRegularVisible.value = false
    toast.success('Transaction régulière créée')
    await loadDashboardData()
  } catch (error) {
    toast.errorAxios(error as AxiosError)
  } finally {
    isQuickRegularSaving.value = false
  }
}

async function loadStatsData() {
  const startDate = format(currentDateRange.value.start, 'yyyy-MM-dd')
  const endDate = format(currentDateRange.value.end, 'yyyy-MM-dd')
  const previousStartDate = format(previousDateRange.value.start, 'yyyy-MM-dd')
  const previousEndDate = format(previousDateRange.value.end, 'yyyy-MM-dd')
  const evolutionStartDate = format(evolutionDateRange.value.start, 'yyyy-MM-dd')
  const evolutionEndDate = format(evolutionDateRange.value.end, 'yyyy-MM-dd')

  const upcomingStart = new Date()
  const upcomingEnd = addDays(upcomingStart, projectionWindowDays.value)
  const upcomingStartDate = format(upcomingStart, 'yyyy-MM-dd')
  const upcomingEndDate = format(upcomingEnd, 'yyyy-MM-dd')

  const now = new Date()
  const projectionStart = isAfter(currentDateRange.value.start, now) ? currentDateRange.value.start : now
  const projectionEnd = currentDateRange.value.end
  const shouldLoadPeriodProjection = !isAfter(projectionStart, projectionEnd)
  const periodProjectionPromise = shouldLoadPeriodProjection
    ? getPrevisionalTransactions(
        format(projectionStart, 'yyyy-MM-dd'),
        format(projectionEnd, 'yyyy-MM-dd'),
        scopedBookletId.value,
      ).catch(() => null)
    : Promise.resolve(null)

  const [categoryData, previousCategoryData, trendsData, previousTrendsData, evolutionTrendsData, previsionalData, periodProjectionData, dailyTrendsData] = await Promise.all([
    getCategoryDistribution({
      bookletId: scopedBookletId.value,
      startDate,
      endDate,
    }).catch(() => null),
    getCategoryDistribution({
      bookletId: scopedBookletId.value,
      startDate: previousStartDate,
      endDate: previousEndDate,
    }).catch(() => null),
    getTrendStats({
      bookletId: scopedBookletId.value,
      startDate,
      endDate,
    }).catch(() => null),
    getTrendStats({
      bookletId: scopedBookletId.value,
      startDate: previousStartDate,
      endDate: previousEndDate,
    }).catch(() => null),
    selectedPeriod.value !== 'month'
      ? getTrendStats({
          bookletId: scopedBookletId.value,
          startDate: evolutionStartDate,
          endDate: evolutionEndDate,
        }).catch(() => null)
      : Promise.resolve(null),
    getPrevisionalTransactions(upcomingStartDate, upcomingEndDate, scopedBookletId.value).catch(() => null),
    periodProjectionPromise,
    selectedPeriod.value === 'month'
      ? getDailyTrendStats(startDate, endDate, scopedBookletId.value).catch(() => null)
      : Promise.resolve(null),
  ])

  categoryDistribution.value = categoryData
  previousCategoryDistribution.value = previousCategoryData
  trendStats.value = trendsData
  previousTrendStats.value = previousTrendsData
  evolutionTrendStats.value = evolutionTrendsData
  dailyTrendStats.value = dailyTrendsData
  previsionalTransactions.value = previsionalData
  periodProjectionTransactions.value = periodProjectionData
}

function shiftPeriod(direction: -1 | 1) {
  if (selectedPeriod.value === 'month') {
    periodAnchorDate.value = addMonths(periodAnchorDate.value, direction)
    return
  }

  if (selectedPeriod.value === 'quarter') {
    periodAnchorDate.value = addMonths(periodAnchorDate.value, direction * 3)
    return
  }

  periodAnchorDate.value = new Date(
    periodAnchorDate.value.getFullYear() + direction,
    periodAnchorDate.value.getMonth(),
    1,
  )
}

onMounted(() => {
  loadBudgetTargets()
  loadDashboardData()
})

watch([selectedBookletId, selectedPeriod, periodAnchorDate], () => {
  lineChartYMin.value = null
  lineChartYMax.value = null
  barChartYMin.value = null
  barChartYMax.value = null
  if (!hasInitializedDashboard.value || booklets.value.length === 0) {
    return
  }
  selectedSliceIndex.value = null
  selectedParentCategoryIndex.value = null
  sliceDisplayMode.value = 'amount'
  loadStatsData()
})

watch(selectedBookletId, () => {
  syncBudgetInputFromSelection()
})
</script>

<template>
  <div class="page-shell w-full relative">
    <!-- Header Section -->
    <div>
      <div class="flex justify-between items-center flex-wrap gap-5">
        <div>
          <h1 class="page-heading mb-2">
            Bonjour, {{ capitalizeFirst(user?.username) }} 👋
          </h1>
          <p v-if="!hasNoBooklet" class="page-subheading" data-test="dashboard-scope">
            Vue {{ selectedPeriodLabel }} ({{ currentDateRangeLabel }}) • {{ isAllBooklets ? 'Tous les comptes' : selectedBooklet?.label }}
          </p>
          <p v-else class="page-subheading">
            Bienvenue — il ne manque plus qu'un livret pour commencer.
          </p>
        </div>
        <div v-if="!hasNoBooklet" class="flex items-center gap-3 flex-wrap">
          <select v-model="selectedBookletId" data-test="account-selector" aria-label="Compte affiché" class="px-3 py-2 rounded-lg border text-sm font-semibold" style="background-color: var(--card-bg); border-color: var(--border-color); color: var(--text-primary);">
            <option v-if="hasSeveralBooklets" :value="ALL_BOOKLETS">
              Tous les comptes
            </option>
            <option v-for="booklet in orderedBooklets" :key="booklet.id" :value="booklet.id">
              {{ booklet.label }}
            </option>
          </select>
          <div class="period-toggle flex items-center rounded-lg p-1">
            <button class="period-toggle-btn px-3 py-1.5 text-sm rounded-md" :class="selectedPeriod === 'month' ? 'is-active' : ''" @click="selectedPeriod = 'month'">
              Mois
            </button>
            <button class="period-toggle-btn px-3 py-1.5 text-sm rounded-md" :class="selectedPeriod === 'quarter' ? 'is-active' : ''" @click="selectedPeriod = 'quarter'">
              Trimestre
            </button>
            <button class="period-toggle-btn px-3 py-1.5 text-sm rounded-md" :class="selectedPeriod === 'year' ? 'is-active' : ''" @click="selectedPeriod = 'year'">
              Année
            </button>
          </div>
          <div class="flex items-center gap-2">
            <button class="period-nav-btn w-9 h-9 rounded-lg border" @click="shiftPeriod(-1)">
              <i class="pi pi-chevron-left" />
            </button>
            <button class="period-nav-btn w-9 h-9 rounded-lg border" @click="shiftPeriod(1)">
              <i class="pi pi-chevron-right" />
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Loading State: the shape of the dashboard, so the content settles in place rather than
         appearing as fifteen blocks at once behind a spinner (UX-26). -->
    <!-- First load only. A later reload (after a quick action) runs under the same scope and must
         keep the dashboard on screen rather than wipe it for placeholders. -->
    <PageSkeleton v-if="isLoading && !hasInitializedDashboard" variant="dashboard" label="Chargement de vos données…" />

    <!-- Onboarding: nothing to show yet, so the page asks for the one thing missing rather than
         rendering four indicators at 0.00 EUR and three empty charts. -->
    <div
      v-else-if="hasNoBooklet"
      class="stat-card flex flex-col items-center justify-center text-center gap-5 py-16 px-6 max-w-2xl mx-auto mt-8"
      data-test="dashboard-onboarding"
    >
      <div class="w-20 h-20 rounded-full grid place-items-center bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white text-3xl">
        <i class="pi pi-wallet" aria-hidden="true" />
      </div>
      <h2 class="heading-2 m-0">
        Créez votre premier livret
      </h2>
      <p class="body-lg max-w-md m-0">
        Un livret regroupe vos transactions et vos soldes. Dès que vous en aurez un,
        ce tableau de bord affichera vos dépenses, vos revenus et vos prévisions.
      </p>
      <button
        class="btn-primary"
        type="button"
        data-test="onboarding-create-booklet"
        @click="isBookletDialogOpen = true"
      >
        <i class="pi pi-plus mr-2" aria-hidden="true" />
        Créer mon premier livret
      </button>
    </div>

    <!-- Main Content -->
    <div v-else class="relative z-1 pb-10">
      <!-- Available from both views: these act, they do not describe (UX-50). -->
      <div class="stat-card">
        <h3 class="text-lg font-bold m-0 mb-4 flex items-center gap-2" style="color: var(--text-primary);">
          <i class="pi pi-bolt text-purple-600" />
          Actions rapides
        </h3>
        <!-- Actions, not links: the sidebar already reaches every page. The two acting on a single
             booklet name it, so the user knows where the entry lands (UX-50). -->
        <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <!-- These two need one target booklet: hidden when every booklet is aggregated (UX-44). -->
          <button v-if="!isAllBooklets" type="button" class="quick-action-btn" data-test="quick-add-transaction" @click="openQuickTransaction">
            <i class="pi pi-plus-circle" aria-hidden="true" />
            <span>
              Ajouter une transaction
              <span class="quick-action-target">sur {{ selectedBooklet?.label }}</span>
            </span>
          </button>
          <button v-if="!isAllBooklets" type="button" class="quick-action-btn" data-test="quick-import-csv" @click="openQuickImport">
            <i class="pi pi-upload" aria-hidden="true" />
            <span>
              Importer un relevé CSV
              <span class="quick-action-target">sur {{ selectedBooklet?.label }}</span>
            </span>
          </button>
          <button type="button" class="quick-action-btn" data-test="quick-add-regular" @click="openQuickRegular">
            <i class="pi pi-sync" aria-hidden="true" />
            <span>Créer une transaction régulière</span>
          </button>
        </div>
      </div>

      <!-- Overview and analysis (UX-18): the overview answers "where do I stand" in three zones;
           secondary analysis lives one click away instead of lengthening the page. -->
      <div role="tablist" aria-label="Vues du tableau de bord" class="dashboard-tabs" data-test="dashboard-tabs">
        <button
          id="dashboard-tab-overview"
          :ref="(element) => { dashboardTabRefs.overview = element as HTMLButtonElement | null }"
          type="button"
          role="tab"
          class="dashboard-tab"
          data-test="tab-overview"
          aria-controls="dashboard-panel-overview"
          :aria-selected="activeDashboardTab === 'overview'"
          :tabindex="activeDashboardTab === 'overview' ? 0 : -1"
          :class="{ 'is-active': activeDashboardTab === 'overview' }"
          @click="activeDashboardTab = 'overview'"
          @keydown="onDashboardTabKeydown"
        >
          Vue d'ensemble
        </button>
        <button
          id="dashboard-tab-analysis"
          :ref="(element) => { dashboardTabRefs.analysis = element as HTMLButtonElement | null }"
          type="button"
          role="tab"
          class="dashboard-tab"
          data-test="tab-analysis"
          aria-controls="dashboard-panel-analysis"
          :aria-selected="activeDashboardTab === 'analysis'"
          :tabindex="activeDashboardTab === 'analysis' ? 0 : -1"
          :class="{ 'is-active': activeDashboardTab === 'analysis' }"
          @click="activeDashboardTab = 'analysis'"
          @keydown="onDashboardTabKeydown"
        >
          Analyse
        </button>
      </div>

      <div
        v-if="activeDashboardTab === 'overview'"
        id="dashboard-panel-overview"
        role="tabpanel"
        aria-labelledby="dashboard-tab-overview"
      >
        <section ref="overviewRef" data-test="zone-situation" class="dashboard-zone opacity-0 translate-y-5 transition-all duration-600" :class="{ 'opacity-100 translate-y-0': isOverviewVisible }">
          <h2 class="zone-title">
            Où j'en suis
          </h2>
          <div class="grid grid-cols-[repeat(auto-fit,minmax(220px,1fr))] gap-6 mb-6">
            <div class="stat-card" data-test="kpi-balance">
              <div class="flex justify-between items-center mb-4">
                <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl text-white bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)]">
                  <i class="pi pi-wallet" />
                </div>
                <span v-if="balanceGrowth !== 0" class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold" :class="balanceGrowth > 0 ? 'bg-[var(--success-soft)] text-[var(--success)]' : 'bg-[var(--danger-soft)] text-[var(--danger)]'">
                  <i :class="balanceGrowth > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
                  {{ Math.abs(balanceGrowth).toFixed(1) }}%
                </span>
              </div>
              <div>
                <h3 class="text-sm mb-2 font-medium" style="color: var(--text-secondary);">
                  Solde du compte
                </h3>
                <p class="text-3xl font-extrabold mb-2" style="color: var(--text-primary);">
                  {{ selectedBookletBalance.toFixed(2) }} €
                </p>
                <p class="text-xs" style="color: var(--text-tertiary);">
                  {{ isAllBooklets ? 'Tous les comptes' : (selectedBooklet?.label || 'Compte sélectionné') }}
                </p>
              </div>
            </div>
            <div class="stat-card">
              <div class="flex justify-between items-center mb-4">
                <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl text-white bg-[var(--expense)]">
                  <i class="pi pi-arrow-down" />
                </div>
                <span v-if="expensesGrowth !== 0" class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold" :class="expensesGrowth > 0 ? 'bg-[var(--danger-soft)] text-[var(--danger)]' : 'bg-[var(--success-soft)] text-[var(--success)]'">
                  <i :class="expensesGrowth > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
                  {{ Math.abs(expensesGrowth).toFixed(1) }}%
                </span>
              </div>
              <div>
                <h3 class="text-sm mb-2 font-medium" style="color: var(--text-secondary);">
                  Dépenses {{ periodMetricLabel }}
                </h3>
                <p class="text-3xl font-extrabold mb-2" style="color: var(--text-primary);">
                  {{ periodExpenses.toFixed(2) }} €
                </p>
                <p class="text-xs" style="color: var(--text-tertiary);" data-test="daily-expense-average">
                  Moy. journalière: {{ dailyExpenseAverage.toFixed(2) }} €
                </p>
              </div>
            </div>
            <div class="stat-card">
              <div class="flex justify-between items-center mb-4">
                <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl text-white bg-[var(--income)]">
                  <i class="pi pi-arrow-up" />
                </div>
                <span v-if="incomeGrowth !== 0" class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold" :class="incomeGrowth > 0 ? 'bg-[var(--success-soft)] text-[var(--success)]' : 'bg-[var(--danger-soft)] text-[var(--danger)]'">
                  <i :class="incomeGrowth > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
                  {{ Math.abs(incomeGrowth).toFixed(1) }}%
                </span>
              </div>
              <div>
                <h3 class="text-sm mb-2 font-medium" style="color: var(--text-secondary);">
                  Revenus {{ periodMetricLabel }}
                </h3>
                <p class="text-3xl font-extrabold mb-2" style="color: var(--text-primary);">
                  {{ periodIncome.toFixed(2) }} €
                </p>
                <p class="text-xs" style="color: var(--text-tertiary);">
                  Épargne: {{ (periodIncome - periodExpenses).toFixed(2) }} €
                </p>
              </div>
            </div>
            <div class="stat-card">
              <div class="flex justify-between items-center mb-4">
                <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl text-white bg-[var(--warning)]">
                  <i class="pi pi-chart-line" />
                </div>
                <span v-if="savingsRate !== 0" class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold" :class="savingsRate > 0 ? 'bg-[var(--success-soft)] text-[var(--success)]' : 'bg-[var(--danger-soft)] text-[var(--danger)]'">
                  <i :class="savingsRate > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
                  {{ Math.abs(savingsRate).toFixed(1) }}%
                </span>
              </div>
              <div>
                <h3 class="text-sm mb-2 font-medium" style="color: var(--text-secondary);">
                  Taux d'épargne
                </h3>
                <p class="text-3xl font-extrabold mb-2" style="color: var(--text-primary);">
                  {{ savingsRate.toFixed(1) }}%
                </p>
                <p class="text-xs" style="color: var(--text-tertiary);">
                  de vos revenus {{ periodMetricLabel }}
                </p>
              </div>
            </div>
            <div class="stat-card" data-test="kpi-projection">
              <div class="flex justify-between items-center mb-4">
                <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl text-white bg-[var(--info)]">
                  <i class="pi pi-flag" />
                </div>
              </div>
              <div>
                <h3 class="text-sm mb-2 font-medium" style="color: var(--text-secondary);">
                  Projection fin de période
                </h3>
                <p class="text-3xl font-extrabold mb-2" :class="projectionPeriodEnded ? 'text-[var(--text-primary)]' : (projectedEndPeriodBalance >= selectedBookletBalance ? 'text-[var(--success)]' : 'text-[var(--danger)]')">
                  {{ projectionPeriodEnded ? 'Période clôturée' : `${projectedEndPeriodBalance.toFixed(2)} €` }}
                </p>
                <p class="text-xs" style="color: var(--text-tertiary);">
                  Solde attendu en fin de période, échéances comprises
                </p>
              </div>
            </div>
          </div>
          <div class="grid grid-cols-1 xl:grid-cols-3 gap-6">
            <div class="stat-card xl:col-span-2">
              <div class="mb-5 flex items-start justify-between gap-3 flex-wrap">
                <div>
                  <h3 class="text-xl font-bold mb-1.5 flex items-center gap-2.5" style="color: var(--text-primary);">
                    <i class="pi pi-chart-line text-purple-600" />
                    Évolution des finances
                  </h3>
                  <p class="text-sm" style="color: var(--text-secondary);">
                    Comparaison revenus vs dépenses sur la période sélectionnée
                  </p>
                </div>
                <button
                  v-if="isLineChartScaled"
                  class="chart-reset-btn"
                  data-test="reset-line-chart-scale"
                  @click="resetLineChartScale"
                >
                  <i class="pi pi-refresh" />
                  Réinitialiser l'échelle
                </button>
              </div>
              <div class="chart-container h-75 relative" data-test="line-chart-container" @wheel="onLineChartWheel">
                <Line :data="expensesTrendData" :options="lineChartOptionsComputed" />
              </div>
            </div>
            <!-- Stored per booklet: "all accounts" has no budget of its own (UX-44). -->
            <div v-if="!isAllBooklets" class="stat-card" data-test="account-budget">
              <div class="flex items-center justify-between mb-4 gap-3">
                <h3 class="text-lg font-bold m-0 flex items-center gap-2" style="color: var(--text-primary);">
                  <i class="pi pi-euro text-[var(--success)]" />
                  Budget du compte
                </h3>
                <span class="text-xs font-semibold px-2 py-1 rounded-full" :class="!isBudgetConfigured ? 'bg-gray-500/10 text-gray-500' : (projectedBudgetDelta >= 0 ? 'bg-[var(--success-soft)] text-[var(--success)]' : 'bg-[var(--danger-soft)] text-[var(--danger)]')">
                  {{ !isBudgetConfigured ? 'Non configuré' : (projectedBudgetDelta >= 0 ? 'Dans le budget' : 'Dépassement') }}
                </span>
              </div>

              <div class="flex items-end gap-2 mb-4">
                <div class="flex-1">
                  <label for="budget-target" class="text-xs font-semibold block mb-1" style="color: var(--text-secondary);">
                    Cible {{ selectedPeriod === 'month' ? 'mensuelle' : 'périodique' }} (€)
                  </label>
                  <input
                    id="budget-target"
                    v-model.number="budgetTargetInput"
                    data-test="budget-target-input"
                    type="number"
                    min="0"
                    step="0.01"
                    class="budget-input"
                    placeholder="Ex: 1200"
                    @blur="saveBudgetTarget"
                  >
                </div>
                <button class="budget-save-btn" data-test="budget-save-btn" @click="saveBudgetTarget">
                  Enregistrer
                </button>
              </div>

              <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div class="rounded-xl p-3" style="background-color: var(--bg-tertiary);">
                  <p class="text-xs m-0" style="color: var(--text-secondary);">
                    Dépenses consommées
                  </p>
                  <p class="text-lg font-bold m-0 mt-1" style="color: var(--text-primary);">
                    {{ periodExpenses.toFixed(2) }} €
                  </p>
                </div>
                <div class="rounded-xl p-3" style="background-color: var(--bg-tertiary);">
                  <p class="text-xs m-0" style="color: var(--text-secondary);">
                    Reste budget
                  </p>
                  <p class="text-lg font-bold m-0 mt-1" :class="budgetDelta >= 0 ? 'text-[var(--success)]' : 'text-[var(--danger)]'">
                    {{ isBudgetConfigured ? `${budgetDelta.toFixed(2)} €` : 'N/A' }}
                  </p>
                </div>
                <div class="rounded-xl p-3" style="background-color: var(--bg-tertiary);">
                  <p class="text-xs m-0" style="color: var(--text-secondary);">
                    Projection budget
                  </p>
                  <p class="text-lg font-bold m-0 mt-1" :class="projectedBudgetDelta >= 0 ? 'text-[var(--success)]' : 'text-[var(--danger)]'">
                    {{ isBudgetConfigured ? `${projectedBudgetDelta.toFixed(2)} €` : 'N/A' }}
                  </p>
                </div>
              </div>

              <p class="text-xs m-0 mt-3" style="color: var(--text-secondary);">
                {{ isBudgetConfigured ? `Consommation: ${budgetConsumptionRate.toFixed(1)}% du budget` : 'Définis une cible pour activer les alertes budget.' }}
              </p>
            </div>
          </div>
        </section>

        <section data-test="zone-upcoming" class="dashboard-zone">
          <h2 class="zone-title">
            Ce qui arrive
          </h2>
          <div class="grid grid-cols-[repeat(auto-fit,minmax(350px,1fr))] gap-6">
            <div class="stat-card" data-test="upcoming-list">
              <div class="flex justify-between items-center mb-5 pb-4" style="border-bottom: 2px solid var(--border-color);">
                <h3 class="text-lg font-bold flex items-center gap-2.5 m-0" style="color: var(--text-primary);">
                  <i class="pi pi-calendar text-purple-600" />
                  Prochaines transactions
                </h3>
                <button class="flex items-center gap-1.5 px-4 py-2 bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white border-none rounded-lg text-sm font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-lg" @click="navigateTo('/regular-transaction')">
                  <i class="pi pi-cog" />
                  Gérer
                </button>
              </div>
              <p class="upcoming-summary" data-test="upcoming-summary">
                {{ totalPrevisionalTransactions }} transaction(s) sur les {{ projectionWindowLabel }} à venir · net
                <strong :class="totalUpcomingNet >= 0 ? 'text-[var(--success)]' : 'text-[var(--danger)]'">{{ totalUpcomingNet.toFixed(2) }} €</strong>
              </p>
              <div class="max-h-87.5 overflow-y-auto">
                <div v-if="upcomingRegularPayments.length === 0 && upcomingNonRegularPayments.length === 0" class="flex flex-col items-center justify-center py-10 px-5 text-center gap-4">
                  <i class="pi pi-calendar-times text-5xl" style="color: var(--text-muted);" />
                  <p class="m-0" style="color: var(--text-secondary);">
                    Aucune transaction prévue
                  </p>
                  <button class="px-5 py-2.5 bg-gradient-to-br from-[var(--primary)] to-[var(--primary-2)] text-white border-none rounded-lg font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-lg" @click="navigateTo('/regular-transaction')">
                    Configurer une mensualité
                  </button>
                </div>
                <div v-else class="flex flex-col gap-4">
                  <div class="rounded-xl p-3" style="background-color: var(--bg-tertiary);">
                    <div class="flex justify-between items-center mb-2">
                      <p class="text-sm font-semibold m-0" style="color: var(--text-primary);">
                        Régulières
                      </p>
                      <p class="text-xs font-semibold m-0" style="color: var(--text-secondary);">
                        Total: {{ totalRegularUpcoming.toFixed(2) }} €
                      </p>
                    </div>
                    <div v-if="upcomingRegularPayments.length === 0" class="text-xs" style="color: var(--text-secondary);">
                      Aucune régulière à venir
                    </div>
                    <div v-else class="flex flex-col gap-2">
                      <div v-for="payment in upcomingRegularPayments" :key="payment.id ?? `${payment.label}-${payment.date}`" class="flex items-center gap-4 p-3 rounded-xl" style="background-color: var(--card-bg);">
                        <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white text-lg flex-shrink-0" :class="!payment.isIncome ? 'bg-[var(--expense)]' : 'bg-[var(--income)]'">
                          <i :class="!payment.isIncome ? 'pi pi-arrow-down' : 'pi pi-arrow-up'" />
                        </div>
                        <div class="flex-1">
                          <p class="font-semibold m-0 mb-1 text-sm" style="color: var(--text-primary);">
                            {{ payment.label }}
                          </p>
                          <p class="text-xs m-0" style="color: var(--text-secondary);">
                            {{ new Date(payment.date).toLocaleDateString('fr-FR') }} • <span class="font-semibold">Régulière</span>
                          </p>
                        </div>
                        <p class="font-bold text-base m-0" :class="!payment.isIncome ? 'text-[var(--expense)]' : 'text-[var(--income)]'">
                          {{ !payment.isIncome ? '-' : '+' }}{{ Number.parseFloat(payment.amount).toFixed(2) }} €
                        </p>
                      </div>
                    </div>
                  </div>

                  <div class="rounded-xl p-3" style="background-color: var(--bg-tertiary);">
                    <div class="flex justify-between items-center mb-2">
                      <p class="text-sm font-semibold m-0" style="color: var(--text-primary);">
                        Non régulières
                      </p>
                      <p class="text-xs font-semibold m-0" style="color: var(--text-secondary);">
                        Total: {{ totalNonRegularUpcoming.toFixed(2) }} €
                      </p>
                    </div>
                    <div v-if="upcomingNonRegularPayments.length === 0" class="text-xs" style="color: var(--text-secondary);">
                      Aucune non régulière à venir
                    </div>
                    <div v-else class="flex flex-col gap-2">
                      <div v-for="payment in upcomingNonRegularPayments" :key="payment.id ?? `${payment.label}-${payment.date}`" class="flex items-center gap-4 p-3 rounded-xl" style="background-color: var(--card-bg);">
                        <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white text-lg flex-shrink-0" :class="!payment.isIncome ? 'bg-[var(--expense)]' : 'bg-[var(--income)]'">
                          <i :class="!payment.isIncome ? 'pi pi-arrow-down' : 'pi pi-arrow-up'" />
                        </div>
                        <div class="flex-1">
                          <p class="font-semibold m-0 mb-1 text-sm" style="color: var(--text-primary);">
                            {{ payment.label }}
                          </p>
                          <p class="text-xs m-0" style="color: var(--text-secondary);">
                            {{ new Date(payment.date).toLocaleDateString('fr-FR') }} • <span class="font-semibold">Non régulière</span>
                          </p>
                        </div>
                        <p class="font-bold text-base m-0" :class="!payment.isIncome ? 'text-[var(--expense)]' : 'text-[var(--income)]'">
                          {{ !payment.isIncome ? '-' : '+' }}{{ Number.parseFloat(payment.amount).toFixed(2) }} €
                        </p>
                      </div>
                    </div>
                  </div>

                  <p class="text-xs m-0" style="color: var(--text-tertiary);">
                    {{ totalPrevisionalTransactions }} transaction(s) sur la fenêtre de {{ projectionWindowLabel }}
                  </p>
                </div>
              </div>
            </div>
            <div class="stat-card" data-test="period-alerts">
              <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-bold m-0 flex items-center gap-2" style="color: var(--text-primary);">
                  <i class="pi pi-bell text-orange-500" />
                  Alertes de la période
                </h3>
                <span class="text-xs font-semibold px-2 py-1 rounded-full" style="background-color: var(--bg-tertiary); color: var(--text-secondary);">
                  {{ dashboardAlerts.length }} active(s)
                </span>
              </div>

              <div v-if="dashboardAlerts.length === 0" class="text-sm" style="color: var(--text-secondary);">
                Aucun signal particulier sur cette période
              </div>
              <div v-else class="flex flex-col gap-3">
                <div v-for="alert in dashboardAlerts" :key="alert.key" class="rounded-xl p-3 border" :class="alert.level === 'danger' ? 'bg-[var(--danger-soft)] border-[var(--danger)]/30' : (alert.level === 'warning' ? 'bg-[var(--warning-soft)] border-[var(--warning)]/30' : 'bg-[var(--info-soft)] border-[var(--info)]/30')">
                  <p class="text-sm font-semibold m-0" style="color: var(--text-primary);">
                    {{ alert.title }}
                  </p>
                  <p class="text-xs m-0 mt-1" style="color: var(--text-secondary);">
                    {{ alert.detail }}
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section ref="chartsRef" data-test="zone-breakdown" class="dashboard-zone opacity-0 translate-y-5 transition-all duration-600 delay-200" :class="{ 'opacity-100 translate-y-0': isChartsVisible }">
          <h2 class="zone-title">
            Où part l'argent
          </h2>
          <div class="stat-card col-span-full" data-test="category-breakdown">
            <div class="flex flex-col gap-6">
              <div class="flex flex-col sm:flex-row gap-6">
                <div class="flex-1 flex flex-col" :class="secondaryChartData ? 'sm:w-1/3' : 'sm:w-1/2'">
                  <div class="mb-5">
                    <h3 class="text-xl font-bold mb-1.5 flex items-center gap-2.5" style="color: var(--text-primary);">
                      <i class="pi pi-chart-pie text-purple-600" />
                      Dépenses par catégorie
                    </h3>
                    <p class="text-sm" style="color: var(--text-secondary);">
                      {{ selectedPeriodLabel }} • Total: {{ categoryDistribution?.totalExpenses || '0.00' }} €
                    </p>
                  </div>
                  <div class="doughnut-chart-container relative flex-1" :class="isSmallScreen ? 'h-72' : 'min-h-70'" data-test="doughnut-container">
                    <Doughnut :data="categoryExpensesData" :options="doughnutOptionsComputed" />
                    <div
                      v-if="doughnutCenterLabel"
                      class="absolute inset-0 flex items-center justify-center pointer-events-none"
                      data-test="doughnut-center-label"
                    >
                      <span
                        class="font-bold"
                        :class="isSmallScreen ? 'text-sm' : 'text-base'"
                        style="color: var(--text-primary);"
                      >
                        {{ doughnutCenterLabel }}
                      </span>
                    </div>
                  </div>
                </div>

                <!-- Secondary doughnut chart for sub-tags breakdown -->
                <Transition name="fade">
                  <div v-if="secondaryChartData" class="flex-1 flex flex-col sm:w-1/3" data-test="secondary-doughnut">
                    <div class="mb-5">
                      <h3 class="text-lg font-semibold mb-1 flex items-center gap-2" style="color: var(--text-primary);">
                        <i class="pi pi-sitemap text-purple-500" />
                        {{ selectedParentCategory?.tagLabel }}
                      </h3>
                      <p class="text-xs" style="color: var(--text-secondary);">
                        Détail des sous-tags • {{ Number.parseFloat(selectedParentCategory?.totalAmount ?? '0').toFixed(2) }} €
                      </p>
                    </div>
                    <div class="doughnut-chart-container relative flex-1" :class="isSmallScreen ? 'h-60' : 'min-h-55'">
                      <Doughnut :data="secondaryChartData" :options="secondaryDoughnutOptions" />
                    </div>
                  </div>
                </Transition>

                <div class="flex-1 flex flex-col" :class="[secondaryChartData ? 'sm:w-1/3' : 'sm:w-1/2', isSmallScreen ? 'mt-2' : '']">
                  <div class="flex justify-between items-center mb-3">
                    <h3 class="text-sm font-semibold m-0" style="color: var(--text-primary);">
                      Top tags de la période
                    </h3>
                    <span class="text-xs" style="color: var(--text-secondary);">
                      Variation vs période précédente
                    </span>
                  </div>
                  <div v-if="topTagsInsights.length === 0" class="text-sm" style="color: var(--text-secondary);">
                    Aucun tag de dépense sur cette période
                  </div>
                  <div v-else class="flex flex-col gap-2 overflow-y-auto flex-1">
                    <div v-for="tag in topTagsInsights" :key="tag.tagLabel" class="rounded-xl p-3 flex items-center justify-between" style="background-color: var(--bg-tertiary);">
                      <div class="flex items-center gap-2.5 min-w-0">
                        <span
                          class="w-3 h-3 rounded-full flex-shrink-0"
                          :style="{ backgroundColor: `rgb(${tag.colorDTO.red}, ${tag.colorDTO.green}, ${tag.colorDTO.blue})` }"
                        />
                        <div class="min-w-0">
                          <p
                            class="text-sm font-semibold m-0 truncate"
                            :style="{ color: toReadableTagTextColor(tag.colorDTO) }"
                          >
                            {{ tag.tagLabel }}
                          </p>
                          <p class="text-xs m-0 mt-1" style="color: var(--text-secondary);">
                            {{ tag.currentAmount.toFixed(2) }} € • {{ Number(tag.percentage).toFixed(1) }}%
                          </p>
                        </div>
                      </div>
                      <span class="text-xs font-semibold px-2 py-1 rounded-full" :class="tag.variation === null ? 'bg-gray-500/10 text-gray-500' : (tag.variation > 0 ? 'bg-[var(--danger-soft)] text-[var(--danger)]' : 'bg-[var(--success-soft)] text-[var(--success)]')">
                        {{ tag.variation === null ? 'Nouveau' : `${tag.variation > 0 ? '+' : ''}${tag.variation.toFixed(1)}%` }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>

      <section
        v-else
        id="dashboard-panel-analysis"
        role="tabpanel"
        aria-labelledby="dashboard-tab-analysis"
        data-test="zone-analysis"
        class="dashboard-zone"
      >
        <h2 class="zone-title">
          Analyse
        </h2>
        <div class="stat-card">
          <div class="mb-5 flex items-start justify-between gap-3 flex-wrap">
            <div>
              <h3 class="text-xl font-bold mb-1.5 flex items-center gap-2.5" style="color: var(--text-primary);">
                <i class="pi pi-chart-bar text-purple-600" />
                Comparaison de période
              </h3>
              <p class="text-sm" style="color: var(--text-secondary);">
                Période active vs période précédente
              </p>
            </div>
            <button
              v-if="isBarChartScaled"
              class="chart-reset-btn"
              data-test="reset-bar-chart-scale"
              @click="resetBarChartScale"
            >
              <i class="pi pi-refresh" />
              Réinitialiser l'échelle
            </button>
          </div>
          <div class="chart-container h-75 relative" data-test="bar-chart-container" @wheel="onBarChartWheel">
            <Bar :data="monthlyComparisonData" :options="barChartOptionsComputed" />
          </div>
        </div>
      </section>
    </div>
  </div>

  <BookletBookingDialog
    :digit="0.00"
    :visible="isBookletDialogOpen"
    @create-booklet="handleBookletCreation"
    @cancel="cancel"
  />

  <!-- Quick action dialogs (UX-50): the same components the booklet and regular transaction pages
       use, so an entry made here behaves exactly as one made there. -->
  <TransactionCreationDialog
    :visible="isQuickTransactionVisible"
    :loading="isQuickTransactionSaving"
    :digit-placeholder="null"
    :transaction-placeholder="quickTransactionDraft"
    :title="isQuickTransactionVisible ? `Nouvelle transaction — ${selectedBooklet?.label ?? ''}` : ''"
    @cancel-creation="isQuickTransactionVisible = false"
    @create-transaction="recordQuickTransaction"
  />

  <CsvImportDialog
    v-if="selectedBooklet"
    ref="csvImportDialogRef"
    :booklet-id="String(selectedBooklet.id)"
    @import-success="onQuickImportSuccess"
  />

  <RegularTransactionCreationDialog
    :visible="isQuickRegularVisible"
    :booklets="booklets"
    :loading="isQuickRegularSaving"
    @cancel-creation="isQuickRegularVisible = false"
    @create-transaction="recordQuickRegular"
  />
</template>

<style scoped>
/* Custom scrollbar styling */
*::-webkit-scrollbar {
  width: 6px;
}

*::-webkit-scrollbar-track {
  background: var(--bg-tertiary);
  border-radius: 10px;
}

*::-webkit-scrollbar-thumb {
  background: var(--primary);
  border-radius: 10px;
}

*::-webkit-scrollbar-thumb:hover {
  background: var(--primary-2);
}

/* Chart container responsive heights */
.chart-reset-btn {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  padding: 0.35rem 0.7rem;
  border-radius: 0.5rem;
  border: 1px solid var(--card-border);
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.chart-reset-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
}

.chart-container {
  position: relative;
  width: 100%;
}

.period-toggle {
  background-color: var(--card-bg);
  border: 1px solid var(--border-color);
}

.period-toggle-btn {
  color: var(--text-secondary);
  background-color: transparent;
  border: none;
  font-weight: 600;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.period-toggle-btn:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.period-toggle-btn.is-active {
  background-color: var(--primary);
  color: #fff;
}

.period-nav-btn {
  border-color: var(--border-color);
  color: var(--text-secondary);
  background-color: var(--card-bg);
  transition: background-color 0.2s ease, color 0.2s ease, border-color 0.2s ease;
}

.period-nav-btn:hover {
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.quick-action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border: 1px solid var(--border-color);
  border-radius: 0.75rem;
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  padding: 0.75rem 1rem;
  font-size: 0.875rem;
  font-weight: 600;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

/* Overview / analysis switch (UX-18). */
.dashboard-tabs {
  display: inline-flex;
  gap: 0.25rem;
  margin: 0 0 1.5rem;
  padding: 0.25rem;
  border: 1px solid var(--border-color);
  border-radius: 0.75rem;
  background-color: var(--bg-tertiary);
}

.dashboard-tab {
  padding: 0.45rem 1rem;
  border: none;
  border-radius: 0.5rem;
  background: transparent;
  color: var(--text-secondary);
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.dashboard-tab.is-active {
  background-color: var(--card-bg);
  color: var(--primary);
  box-shadow: 0 1px 3px var(--shadow-color, rgba(0, 0, 0, 0.08));
}

/* A zone groups blocks answering one question; its title carries the reading order the fifteen
   blocks of equal weight lacked. */
.dashboard-zone {
  margin-bottom: 2.5rem;
}

.zone-title {
  margin: 0 0 1rem;
  font-size: 0.8rem;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.upcoming-summary {
  margin: 0 0 1rem;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

/* The booklet an action lands on, under its label: the account must never be implicit (UX-50). */
.quick-action-target {
  display: block;
  font-size: 0.75rem;
  font-weight: 500;
  color: var(--text-secondary);
  overflow-wrap: anywhere;
}

.quick-action-btn:hover {
  background-color: var(--card-bg);
  border-color: var(--primary);
}

.budget-input {
  width: 100%;
  border: 1px solid var(--border-color);
  border-radius: 0.75rem;
  padding: 0.6rem 0.75rem;
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
}

.budget-save-btn {
  border: 1px solid var(--border-color);
  border-radius: 0.75rem;
  padding: 0.6rem 0.9rem;
  background-color: var(--bg-tertiary);
  color: var(--text-primary);
  font-size: 0.8rem;
  font-weight: 700;
}

.budget-save-btn:hover {
  border-color: var(--primary);
  background-color: var(--card-bg);
}

.doughnut-chart-container {
  position: relative;
  width: 100%;
}

@media (min-width: 640px) {
  .chart-container.h-75 {
    height: 20rem;
  }
}

@media (max-width: 639px) {
  .chart-container.h-75 {
    height: 18rem;
  }
}
</style>
