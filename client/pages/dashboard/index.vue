<script setup lang="ts">
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
import { addDays, addMonths, endOfMonth, endOfQuarter, endOfYear, format, isAfter, startOfMonth, startOfQuarter, startOfYear, subMonths } from 'date-fns'
import { fr } from 'date-fns/locale'
import { onBeforeUnmount } from 'vue'
import { Bar, Doughnut, Line } from 'vue-chartjs'
import useAuth from '@/composables/useAuth'
import BookletBookingDialog from '~/components/dialog/BookletBookingDialog.vue'
import useStats from '~/composables/useStats'
import { LOADING_SCOPES } from '~/constants/loadingScopes'
import { capitalizeFirst, rgbToHex } from '~/utils/util'

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
})

const { user } = useAuth()
const { createAccount, fetch: fetchBooklets } = useBooklet()
const { getRegularTransaction } = useRegularTransaction()
const { getAllTags } = useTag()
const { getCategoryDistribution, getTrendStats, getPrevisionalTransactions } = useStats()
const { isScopeLoading, withLoading } = useLoading()
const toast = useJToast()

// Refs
const isAccountDialogOpen = ref(false)
const accounts = ref<BookletDTO[]>([])
const regularTransactions = ref<RegularTransactionDTO[]>([])
const tags = ref<TagDTO[]>([])
const categoryDistribution = ref<CategoryDistributionDTO | null>(null)
const previousCategoryDistribution = ref<CategoryDistributionDTO | null>(null)
const trendStats = ref<TrendStatsDTO | null>(null)
const previousTrendStats = ref<TrendStatsDTO | null>(null)
const evolutionTrendStats = ref<TrendStatsDTO | null>(null)
const previsionalTransactions = ref<PrevisionalTransactionsDTO | null>(null)
const periodProjectionTransactions = ref<PrevisionalTransactionsDTO | null>(null)
const selectedAccountId = ref<string | number | null>(null)
const selectedPeriod = ref<'month' | 'quarter' | 'year'>('month')
const periodAnchorDate = ref(new Date())
const hasInitializedDashboard = ref(false)
const dashboardLoadingScope = LOADING_SCOPES.dashboard.initial
const isLoading = computed(() => isScopeLoading(dashboardLoadingScope))

// Animation refs
const overviewRef = ref(null)
const chartsRef = ref(null)
const isOverviewVisible = ref(false)
const isChartsVisible = ref(false)

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
  accounts.value.reduce((acc, curr) => acc + Number.parseFloat(curr.amount.toString()), 0.00),
)

const selectedAccount = computed(() =>
  accounts.value.find(account => account.id === selectedAccountId.value) ?? null,
)

const scopedAccountId = computed(() => {
  if (selectedAccountId.value === null) {
    return undefined
  }

  const raw = String(selectedAccountId.value)
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i
  return uuidRegex.test(raw) ? raw : undefined
})

const selectedAccountBalance = computed(() => {
  if (!selectedAccount.value) {
    return totalBalance.value
  }
  return Number.parseFloat(selectedAccount.value.amount.toString())
})

const selectedPeriodLabel = computed(() => {
  if (selectedPeriod.value === 'month') {
    return format(periodAnchorDate.value, 'MMMM yyyy', { locale: fr })
  }
  if (selectedPeriod.value === 'quarter') {
    const quarter = Math.floor(periodAnchorDate.value.getMonth() / 3) + 1
    return `T${quarter} ${periodAnchorDate.value.getFullYear()}`
  }
  return `${periodAnchorDate.value.getFullYear()}`
})

const periodMetricLabel = computed(() =>
  selectedPeriod.value === 'month' ? 'du mois' : 'de la période',
)

const currentDateRange = computed(() => {
  if (selectedPeriod.value === 'month') {
    return {
      start: startOfMonth(periodAnchorDate.value),
      end: endOfMonth(periodAnchorDate.value),
    }
  }

  if (selectedPeriod.value === 'quarter') {
    return {
      start: startOfQuarter(periodAnchorDate.value),
      end: endOfQuarter(periodAnchorDate.value),
    }
  }

  return {
    start: startOfYear(periodAnchorDate.value),
    end: endOfYear(periodAnchorDate.value),
  }
})

const previousDateRange = computed(() => {
  if (selectedPeriod.value === 'month') {
    const previousAnchor = subMonths(periodAnchorDate.value, 1)
    return {
      start: startOfMonth(previousAnchor),
      end: endOfMonth(previousAnchor),
    }
  }

  if (selectedPeriod.value === 'quarter') {
    const previousAnchor = subMonths(periodAnchorDate.value, 3)
    return {
      start: startOfQuarter(previousAnchor),
      end: endOfQuarter(previousAnchor),
    }
  }

  const previousAnchor = subMonths(periodAnchorDate.value, 12)
  return {
    start: startOfYear(previousAnchor),
    end: endOfYear(previousAnchor),
  }
})

const evolutionDateRange = computed(() => {
  if (selectedPeriod.value === 'month') {
    return {
      start: startOfMonth(subMonths(periodAnchorDate.value, 5)),
      end: endOfMonth(periodAnchorDate.value),
    }
  }

  return {
    start: currentDateRange.value.start,
    end: currentDateRange.value.end,
  }
})

const currentDateRangeLabel = computed(() =>
  `${format(currentDateRange.value.start, 'dd MMM', { locale: fr })} - ${format(currentDateRange.value.end, 'dd MMM yyyy', { locale: fr })}`,
)

const currentMonthTrend = computed(() => {
  if (!trendStats.value?.monthlyTrends.length) {
    return null
  }

  const currentMonth = periodAnchorDate.value.getMonth() + 1
  const currentYear = periodAnchorDate.value.getFullYear()

  return trendStats.value.monthlyTrends.find(
    trend => trend.month === currentMonth && trend.year === currentYear,
  )
})

const previousMonthTrend = computed(() => {
  if (!trendStats.value?.monthlyTrends.length) {
    return null
  }

  const lastMonth = subMonths(periodAnchorDate.value, 1)
  const month = lastMonth.getMonth() + 1
  const year = lastMonth.getFullYear()

  return trendStats.value.monthlyTrends.find(
    trend => trend.month === month && trend.year === year,
  )
})

const monthlyExpenses = computed(() => {
  if (!trendStats.value?.monthlyTrends.length) {
    return 0
  }
  return trendStats.value.monthlyTrends.reduce(
    (acc, trend) => acc + Number.parseFloat(trend.expenses),
    0,
  )
})

const monthlyIncome = computed(() => {
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

  return ((monthlyExpenses.value - previousPeriodExpenses.value) / previousPeriodExpenses.value * 100)
})

const incomeGrowth = computed(() => {
  if (previousPeriodIncome.value === 0) {
    return 0
  }

  return ((monthlyIncome.value - previousPeriodIncome.value) / previousPeriodIncome.value * 100)
})

const balanceGrowth = computed(() => {
  const currentBalance = monthlyIncome.value - monthlyExpenses.value
  const previousBalance = previousPeriodIncome.value - previousPeriodExpenses.value

  if (previousBalance === 0) {
    return 0
  }

  return ((currentBalance - previousBalance) / Math.abs(previousBalance) * 100)
})

const savingsRate = computed(() => {
  if (monthlyIncome.value === 0 || monthlyExpenses.value > monthlyIncome.value) {
    return 0
  }

  return ((monthlyIncome.value - monthlyExpenses.value) / monthlyIncome.value * 100)
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

const projectionPeriodEnded = computed(() => isAfter(new Date(), currentDateRange.value.end))

const periodProjectionNet = computed(() => {
  if (!periodProjectionTransactions.value) {
    return 0
  }

  return Number.parseFloat(periodProjectionTransactions.value.totalAmount || '0')
})

const projectedEndPeriodBalance = computed(() =>
  selectedAccountBalance.value + periodProjectionNet.value,
)

const dashboardAlerts = computed(() => {
  const alerts: Array<{ key: string, level: 'danger' | 'warning' | 'info', title: string, detail: string }> = []

  if (monthlyExpenses.value > monthlyIncome.value && monthlyIncome.value > 0) {
    alerts.push({
      key: 'overspending',
      level: 'danger',
      title: 'Dépenses supérieures aux revenus',
      detail: `Le déficit de la période est de ${(monthlyExpenses.value - monthlyIncome.value).toFixed(2)} €`,
    })
  }

  if (totalUpcomingNet.value < 0) {
    alerts.push({
      key: 'upcoming-negative',
      level: 'warning',
      title: 'Fenêtre 15 jours négative',
      detail: `Impact prévisionnel: ${totalUpcomingNet.value.toFixed(2)} €`,
    })
  }

  if (totalPrevisionalTransactions.value === 0) {
    alerts.push({
      key: 'no-upcoming',
      level: 'info',
      title: 'Aucun mouvement à venir',
      detail: 'Aucune transaction prévue dans les 15 prochains jours',
    })
  }

  return alerts.slice(0, 3)
})

// Chart data
const expensesTrendData = computed(() => {
  if (!evolutionTrendStats.value?.monthlyTrends.length) {
    return {
      labels: [],
      datasets: [],
    }
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
        borderColor: '#ef4444',
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        tension: 0.4,
        fill: true,
      },
      {
        label: 'Revenus',
        data: sortedTrends.map(trend => Number.parseFloat(trend.income)),
        borderColor: '#10b981',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        tension: 0.4,
        fill: true,
      },
    ],
  }
})

const categoryExpensesData = computed(() => {
  if (!categoryDistribution.value?.categories.length) {
    return {
      labels: [],
      datasets: [{ data: [], backgroundColor: [], borderWidth: 0 }],
    }
  }

  const sortedCategories = categoryDistribution.value.categories.toSorted((a, b) => Number.parseFloat(b.totalAmount) - Number.parseFloat(a.totalAmount))
    .slice(0, 6)

  return {
    labels: sortedCategories.map(cat => cat.tagLabel),
    datasets: [
      {
        data: sortedCategories.map(cat => Number.parseFloat(cat.totalAmount)),
        backgroundColor: [
          '#822acc',
          '#10b981',
          '#f59e0b',
          '#3b82f6',
          '#ef4444',
          '#8b5cf6',
        ],
        borderWidth: 0,
      },
    ],
  }
})

const topTagsInsights = computed(() => {
  const currentCategories = categoryDistribution.value?.categories ?? []
  if (currentCategories.length === 0) {
    return []
  }

  const previousMap = new Map(
    (previousCategoryDistribution.value?.categories ?? []).map(category => [
      category.tagId ?? category.tagLabel,
      Number.parseFloat(category.totalAmount),
    ]),
  )

  return currentCategories
    .toSorted((a, b) => Number.parseFloat(b.totalAmount) - Number.parseFloat(a.totalAmount))
    .slice(0, 5)
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
      }
    })
})

const monthlyComparisonData = computed(() => {
  const currentBalance = monthlyIncome.value - monthlyExpenses.value
  const previousBalance = previousPeriodIncome.value - previousPeriodExpenses.value

  return {
    labels: ['Revenus', 'Dépenses', 'Solde net'],
    datasets: [
      {
        label: 'Période active',
        data: [monthlyIncome.value, monthlyExpenses.value, currentBalance],
        backgroundColor: '#822acc',
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
          const total = context.dataset.data.reduce((a: number, b: number) => a + b, 0)
          const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : 0
          return `${label}: ${value.toFixed(2)}€ (${percentage}%)`
        },
      },
    },
  },
  cutout: '65%',
}

// Responsive behavior for legends and chart sizing
const isSmallScreen = ref(false)

function updateIsSmallScreen() {
  if (typeof window !== 'undefined') {
    isSmallScreen.value = window.innerWidth <= 640
  }
}

// computed options so we can change legend position for doughnut on small screens
const chartOptionsComputed = computed(() => {
  // shallow clone
  const opts = JSON.parse(JSON.stringify(chartOptions))
  // keep legend at bottom for small screens (line/bar already bottom)
  opts.plugins = opts.plugins || {}
  opts.plugins.legend = opts.plugins.legend || {}
  opts.plugins.legend.position = 'bottom'
  return opts
})

const doughnutOptionsComputed = computed(() => {
  const opts = JSON.parse(JSON.stringify(doughnutOptions))
  opts.plugins = opts.plugins || {}
  opts.plugins.legend = opts.plugins.legend || {}
  // put legend under chart on small screens to avoid horizontal overflow
  opts.plugins.legend.position = isSmallScreen.value ? 'bottom' : 'right'
  // reduce label size on small screens
  opts.plugins.legend.labels = opts.plugins.legend.labels || {}
  opts.plugins.legend.labels.font = opts.plugins.legend.labels.font || {}
  opts.plugins.legend.labels.font.size = isSmallScreen.value ? 11 : 12
  return opts
})

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
function handleAccountCreation(account: { label: string, digit: number }) {
  createAccount(account.label, account.digit, '€')
    .then((acc) => {
      if (accounts.value.length < 10) {
        accounts.value.push(acc)
      }
      toast.success('Le compte a bien été créé')
      navigateTo(`/account/${acc.id}`)
    })
    .catch(err => toast.errorAxios(err))
}

function cancel() {
  isAccountDialogOpen.value = false
}

async function loadDashboardData() {
  await withLoading(async () => {
    try {
      // Load basic data
      const [accountsData, regularTransData, tagsData] = await Promise.all([
        fetchBooklets().catch(() => []),
        getRegularTransaction().catch(() => []),
        getAllTags().catch(() => []),
      ])

      accounts.value = Array.isArray(accountsData) ? accountsData : []
      regularTransactions.value = Array.isArray(regularTransData) ? regularTransData : []
      tags.value = Array.isArray(tagsData) ? tagsData : []

      if (!selectedAccountId.value && accounts.value.length > 0) {
        const firstAccountId = accounts.value[0]?.id
        selectedAccountId.value = firstAccountId ?? null
      }

      await loadStatsData()
      hasInitializedDashboard.value = true
    } catch (error) {
      toast.error('Erreur lors du chargement des données')
      console.error(error)
    }
  }, dashboardLoadingScope)
}

async function loadStatsData() {
  const startDate = format(currentDateRange.value.start, 'yyyy-MM-dd')
  const endDate = format(currentDateRange.value.end, 'yyyy-MM-dd')
  const previousStartDate = format(previousDateRange.value.start, 'yyyy-MM-dd')
  const previousEndDate = format(previousDateRange.value.end, 'yyyy-MM-dd')
  const evolutionStartDate = format(evolutionDateRange.value.start, 'yyyy-MM-dd')
  const evolutionEndDate = format(evolutionDateRange.value.end, 'yyyy-MM-dd')

  const upcomingStart = new Date()
  const upcomingEnd = addDays(upcomingStart, 15)
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
      scopedAccountId.value,
    ).catch(() => null)
    : Promise.resolve(null)

  const [categoryData, previousCategoryData, trendsData, previousTrendsData, evolutionTrendsData, previsionalData, periodProjectionData] = await Promise.all([
    getCategoryDistribution({
      accountId: scopedAccountId.value,
      startDate,
      endDate,
    }).catch(() => null),
    getCategoryDistribution({
      accountId: scopedAccountId.value,
      startDate: previousStartDate,
      endDate: previousEndDate,
    }).catch(() => null),
    getTrendStats({
      accountId: scopedAccountId.value,
      startDate,
      endDate,
    }).catch(() => null),
    getTrendStats({
      accountId: scopedAccountId.value,
      startDate: previousStartDate,
      endDate: previousEndDate,
    }).catch(() => null),
    getTrendStats({
      accountId: scopedAccountId.value,
      startDate: evolutionStartDate,
      endDate: evolutionEndDate,
    }).catch(() => null),
    getPrevisionalTransactions(upcomingStartDate, upcomingEndDate, scopedAccountId.value).catch(() => null),
    periodProjectionPromise,
  ])

  categoryDistribution.value = categoryData
  previousCategoryDistribution.value = previousCategoryData
  trendStats.value = trendsData
  previousTrendStats.value = previousTrendsData
  evolutionTrendStats.value = evolutionTrendsData
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
  loadDashboardData()
})

watch([selectedAccountId, selectedPeriod, periodAnchorDate], () => {
  if (!hasInitializedDashboard.value || accounts.value.length === 0) {
    return
  }
  loadStatsData()
})
</script>

<template>
  <div class="w-full min-h-screen p-5 relative" style="background: linear-gradient(135deg, var(--bg-gradient-from) 0%, var(--bg-gradient-to) 100%);">
    <!-- Header Section -->
    <div class="mb-8">
      <div class="flex justify-between items-center flex-wrap gap-5">
        <div>
          <h1 class="text-4xl font-extrabold mb-2" style="color: var(--text-primary);">
            Bonjour, {{ capitalizeFirst(user?.username) }} 👋
          </h1>
          <p class="text-base" style="color: var(--text-secondary);">
            Vue {{ selectedPeriodLabel }} • {{ selectedAccount?.labelAccount || 'Tous les comptes' }}
          </p>
          <div class="flex items-center gap-2.5 mt-3 flex-wrap">
            <span class="px-3 py-1.5 rounded-full text-xs font-semibold" style="background-color: var(--card-bg); color: var(--text-secondary); border: 1px solid var(--border-color);">
              Période: {{ currentDateRangeLabel }}
            </span>
            <span class="px-3 py-1.5 rounded-full text-xs font-semibold" style="background-color: var(--card-bg); color: var(--text-secondary); border: 1px solid var(--border-color);">
              À venir 15 jours: {{ totalPrevisionalTransactions }} transaction(s)
            </span>
            <span class="px-3 py-1.5 rounded-full text-xs font-semibold" :class="totalUpcomingNet >= 0 ? 'text-green-500' : 'text-red-500'" style="background-color: var(--card-bg); border: 1px solid var(--border-color);">
              Solde prévisionnel court terme: {{ totalUpcomingNet.toFixed(2) }} €
            </span>
            <span class="px-3 py-1.5 rounded-full text-xs font-semibold" :class="projectedEndPeriodBalance >= selectedAccountBalance ? 'text-green-500' : 'text-red-500'" style="background-color: var(--card-bg); border: 1px solid var(--border-color);">
              Projection fin de période:
              {{ projectionPeriodEnded ? 'Période clôturée' : `${projectedEndPeriodBalance.toFixed(2)} €` }}
            </span>
          </div>
        </div>
        <div class="flex items-center gap-3 flex-wrap">
          <select v-model="selectedAccountId" class="px-3 py-2 rounded-lg border text-sm font-semibold" style="background-color: var(--card-bg); border-color: var(--border-color); color: var(--text-primary);">
            <option v-for="account in accounts" :key="account.id" :value="account.id">
              {{ account.labelAccount }}
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

    <!-- Loading State -->
    <div v-if="isLoading" class="flex flex-col items-center justify-center py-20 gap-4 min-h-60vh">
      <i class="pi pi-spin pi-spinner text-5xl text-purple-600" />
      <p style="color: var(--text-secondary);">
        Chargement de vos données...
      </p>
    </div>

    <!-- Main Content -->
    <div v-else class="relative z-1 pb-10">
      <!-- KPI Cards -->
      <section ref="overviewRef" class="grid grid-cols-[repeat(auto-fit,minmax(280px,1fr))] gap-6 mb-8 opacity-0 translate-y-5 transition-all duration-600" :class="{ 'opacity-100 translate-y-0': isOverviewVisible }">
        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="flex justify-between items-center mb-4">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl text-white bg-gradient-to-br from-purple-600 to-purple-700">
              <i class="pi pi-wallet" />
            </div>
            <span v-if="balanceGrowth !== 0" class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold" :class="balanceGrowth > 0 ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'">
              <i :class="balanceGrowth > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
              {{ Math.abs(balanceGrowth).toFixed(1) }}%
            </span>
          </div>
          <div>
            <h3 class="text-sm mb-2 font-medium" style="color: var(--text-secondary);">
              Solde du compte
            </h3>
            <p class="text-3xl font-extrabold mb-2" style="color: var(--text-primary);">
              {{ selectedAccountBalance.toFixed(2) }} €
            </p>
            <p class="text-xs" style="color: var(--text-tertiary);">
              {{ selectedAccount?.labelAccount || 'Compte sélectionné' }}
            </p>
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="flex justify-between items-center mb-4">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl text-white bg-gradient-to-br from-red-500 to-red-600">
              <i class="pi pi-arrow-down" />
            </div>
            <span v-if="expensesGrowth !== 0" class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold" :class="expensesGrowth > 0 ? 'bg-red-500/10 text-red-500' : 'bg-green-500/10 text-green-500'">
              <i :class="expensesGrowth > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
              {{ Math.abs(expensesGrowth).toFixed(1) }}%
            </span>
          </div>
          <div>
            <h3 class="text-sm mb-2 font-medium" style="color: var(--text-secondary);">
              Dépenses {{ periodMetricLabel }}
            </h3>
            <p class="text-3xl font-extrabold mb-2" style="color: var(--text-primary);">
              {{ monthlyExpenses.toFixed(2) }} €
            </p>
            <p class="text-xs" style="color: var(--text-tertiary);">
              Moy. journalière: {{ (monthlyExpenses / 30).toFixed(2) }} €
            </p>
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="flex justify-between items-center mb-4">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl text-white bg-gradient-to-br from-green-500 to-green-600">
              <i class="pi pi-arrow-up" />
            </div>
            <span v-if="incomeGrowth !== 0" class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold" :class="incomeGrowth > 0 ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'">
              <i :class="incomeGrowth > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
              {{ Math.abs(incomeGrowth).toFixed(1) }}%
            </span>
          </div>
          <div>
            <h3 class="text-sm mb-2 font-medium" style="color: var(--text-secondary);">
              Revenus {{ periodMetricLabel }}
            </h3>
            <p class="text-3xl font-extrabold mb-2" style="color: var(--text-primary);">
              {{ monthlyIncome.toFixed(2) }} €
            </p>
            <p class="text-xs" style="color: var(--text-tertiary);">
              Épargne: {{ (monthlyIncome - monthlyExpenses).toFixed(2) }} €
            </p>
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="flex justify-between items-center mb-4">
            <div class="w-14 h-14 rounded-2xl flex items-center justify-center text-2xl text-white bg-gradient-to-br from-yellow-400 to-yellow-500">
              <i class="pi pi-chart-line" />
            </div>
            <span v-if="savingsRate !== 0" class="flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold" :class="savingsRate > 0 ? 'bg-green-500/10 text-green-500' : 'bg-red-500/10 text-red-500'">
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
              Objectif: 30%
            </p>
          </div>
        </div>
      </section>

      <!-- Charts Section -->
      <section ref="chartsRef" class="grid grid-cols-[repeat(auto-fit,minmax(320px,1fr))] gap-6 mb-8 opacity-0 translate-y-5 transition-all duration-600 delay-200" :class="{ 'opacity-100 translate-y-0': isChartsVisible }">
        <div class="rounded-2xl p-6 shadow-lg col-span-full" style="background-color: var(--card-bg);">
          <div class="mb-5">
            <h2 class="text-xl font-bold mb-1.5 flex items-center gap-2.5" style="color: var(--text-primary);">
              <i class="pi pi-chart-line text-purple-600" />
              Évolution des finances
            </h2>
            <p class="text-sm" style="color: var(--text-secondary);">
              Comparaison revenus vs dépenses sur la période sélectionnée
            </p>
          </div>
          <div class="chart-container h-75 relative">
            <Line :data="expensesTrendData" :options="chartOptionsComputed" />
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="mb-5">
            <h2 class="text-xl font-bold mb-1.5 flex items-center gap-2.5" style="color: var(--text-primary);">
              <i class="pi pi-chart-pie text-purple-600" />
              Dépenses par catégorie
            </h2>
            <p class="text-sm" style="color: var(--text-secondary);">
              {{ selectedPeriodLabel }} • Total: {{ categoryDistribution?.totalExpenses || '0.00' }} €
            </p>
          </div>
          <div class="chart-container h-70 relative">
            <Doughnut :data="categoryExpensesData" :options="doughnutOptionsComputed" />
          </div>
          <div class="mt-5">
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
            <div v-else class="flex flex-col gap-2">
              <div v-for="tag in topTagsInsights" :key="tag.tagLabel" class="rounded-xl p-3 flex items-center justify-between" style="background-color: var(--bg-tertiary);">
                <div>
                  <p class="text-sm font-semibold m-0" style="color: var(--text-primary);">
                    {{ tag.tagLabel }}
                  </p>
                  <p class="text-xs m-0 mt-1" style="color: var(--text-secondary);">
                    {{ tag.currentAmount.toFixed(2) }} € • {{ Number(tag.percentage).toFixed(1) }}%
                  </p>
                </div>
                <span class="text-xs font-semibold px-2 py-1 rounded-full" :class="tag.variation === null ? 'bg-gray-500/10 text-gray-500' : (tag.variation > 0 ? 'bg-red-500/10 text-red-500' : 'bg-green-500/10 text-green-500')">
                  {{ tag.variation === null ? 'Nouveau' : `${tag.variation > 0 ? '+' : ''}${tag.variation.toFixed(1)}%` }}
                </span>
              </div>
            </div>
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="mb-5">
            <h2 class="text-xl font-bold mb-1.5 flex items-center gap-2.5" style="color: var(--text-primary);">
              <i class="pi pi-chart-bar text-purple-600" />
              Comparaison de période
            </h2>
            <p class="text-sm" style="color: var(--text-secondary);">
              Période active vs période précédente
            </p>
          </div>
          <div class="chart-container h-75 relative">
            <Bar :data="monthlyComparisonData" :options="chartOptionsComputed" />
          </div>
        </div>
      </section>

      <!-- Quick Actions & Info Section -->
      <section class="grid grid-cols-[repeat(auto-fit,minmax(350px,1fr))] gap-6 mb-8">
        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="flex justify-between items-center mb-5 pb-4" style="border-bottom: 2px solid var(--border-color);">
            <h2 class="text-lg font-bold flex items-center gap-2.5 m-0" style="color: var(--text-primary);">
              <i class="pi pi-book text-purple-600" />
              Mes livrets
            </h2>
            <button class="flex items-center gap-1.5 px-4 py-2 bg-gradient-to-br from-purple-600 to-purple-700 text-white border-none rounded-lg text-sm font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-lg" @click="isAccountDialogOpen = true">
              <i class="pi pi-plus" />
              Nouveau
            </button>
          </div>
          <div class="max-h-87.5 overflow-y-auto">
            <div v-if="accounts.length === 0" class="flex flex-col items-center justify-center py-10 px-5 text-center gap-4">
              <i class="pi pi-inbox text-5xl" style="color: var(--text-muted);" />
              <p class="m-0" style="color: var(--text-secondary);">
                Aucun livret créé
              </p>
              <button class="px-5 py-2.5 bg-gradient-to-br from-purple-600 to-purple-700 text-white border-none rounded-lg font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-lg" @click="isAccountDialogOpen = true">
                Créer mon premier livret
              </button>
            </div>
            <div v-else class="flex flex-col gap-3">
              <div
                v-for="account in accounts.slice(0, 4)"
                :key="account.id"
                class="flex items-center gap-4 p-4 rounded-xl cursor-pointer transition-all hover:translate-x-1.5"
                style="background-color: var(--bg-tertiary);"
                @click="navigateTo(`/account/${account.id}`)"
              >
                <div class="w-12 h-12 bg-gradient-to-br from-purple-600 to-purple-700 rounded-xl flex items-center justify-center text-white text-xl flex-shrink-0">
                  <i class="pi pi-wallet" />
                </div>
                <div class="flex-1">
                  <p class="font-semibold m-0 mb-1" style="color: var(--text-primary);">
                    {{ account.labelAccount }}
                  </p>
                  <p class="text-sm m-0" style="color: var(--text-secondary);">
                    {{ Number.parseFloat(account.amount.toString()).toFixed(2) }} €
                  </p>
                </div>
                <i class="pi pi-chevron-right" style="color: var(--text-tertiary);" />
              </div>
              <button v-if="accounts.length > 4" class="w-full py-3 bg-transparent border-2 border-dashed rounded-lg font-semibold cursor-pointer transition-all hover:border-purple-600 hover:text-purple-600" style="border-color: var(--border-color); color: var(--text-secondary);" @click="navigateTo('/accounts')">
                Voir tous les livrets ({{ accounts.length }})
              </button>
            </div>
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="flex justify-between items-center mb-5 pb-4" style="border-bottom: 2px solid var(--border-color);">
            <h2 class="text-lg font-bold flex items-center gap-2.5 m-0" style="color: var(--text-primary);">
              <i class="pi pi-calendar text-purple-600" />
              Prochaines transactions
            </h2>
            <button class="flex items-center gap-1.5 px-4 py-2 bg-gradient-to-br from-purple-600 to-purple-700 text-white border-none rounded-lg text-sm font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-lg" @click="navigateTo('/regular-transaction')">
              <i class="pi pi-cog" />
              Gérer
            </button>
          </div>
          <div class="max-h-87.5 overflow-y-auto">
            <div v-if="upcomingRegularPayments.length === 0 && upcomingNonRegularPayments.length === 0" class="flex flex-col items-center justify-center py-10 px-5 text-center gap-4">
              <i class="pi pi-calendar-times text-5xl" style="color: var(--text-muted);" />
              <p class="m-0" style="color: var(--text-secondary);">
                Aucune transaction prévue
              </p>
              <button class="px-5 py-2.5 bg-gradient-to-br from-purple-600 to-purple-700 text-white border-none rounded-lg font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-lg" @click="navigateTo('/regular-transaction')">
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
                    <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white text-lg flex-shrink-0" :class="!payment.isIncome ? 'bg-gradient-to-br from-red-500 to-red-600' : 'bg-gradient-to-br from-green-500 to-green-600'">
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
                    <p class="font-bold text-base m-0" :class="!payment.isIncome ? 'text-red-500' : 'text-green-500'">
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
                    <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white text-lg flex-shrink-0" :class="!payment.isIncome ? 'bg-gradient-to-br from-red-500 to-red-600' : 'bg-gradient-to-br from-green-500 to-green-600'">
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
                    <p class="font-bold text-base m-0" :class="!payment.isIncome ? 'text-red-500' : 'text-green-500'">
                      {{ !payment.isIncome ? '-' : '+' }}{{ Number.parseFloat(payment.amount).toFixed(2) }} €
                    </p>
                  </div>
                </div>
              </div>

              <p class="text-xs m-0" style="color: var(--text-tertiary);">
                {{ totalPrevisionalTransactions }} transaction(s) sur la fenêtre de 15 jours
              </p>
            </div>
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="flex justify-between items-center mb-5 pb-4" style="border-bottom: 2px solid var(--border-color);">
            <h2 class="text-lg font-bold flex items-center gap-2.5 m-0" style="color: var(--text-primary);">
              <i class="pi pi-tags text-purple-600" />
              Tags populaires
            </h2>
            <button class="flex items-center gap-1.5 px-4 py-2 bg-gradient-to-br from-purple-600 to-purple-700 text-white border-none rounded-lg text-sm font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-lg" @click="navigateTo('/tag')">
              <i class="pi pi-plus" />
              Nouveau
            </button>
          </div>
          <div class="max-h-87.5 overflow-y-auto">
            <div v-if="tags.length === 0" class="flex flex-col items-center justify-center py-10 px-5 text-center gap-4">
              <i class="pi pi-tag text-5xl" style="color: var(--text-muted);" />
              <p class="m-0" style="color: var(--text-secondary);">
                Aucun tag créé
              </p>
              <button class="px-5 py-2.5 bg-gradient-to-br from-purple-600 to-purple-700 text-white border-none rounded-lg font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-lg" @click="navigateTo('/tag')">
                Créer un tag
              </button>
            </div>
            <div v-else class="flex flex-wrap gap-2.5">
              <div
                v-for="tag in tags.slice(0, 6)"
                :key="tag.tagId"
                class="inline-flex items-center gap-1.5 px-4 py-2 border-2 rounded-full text-xs font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-md"
                :style="{
                  backgroundColor: `${rgbToHex(tag.colorDTO)}20`,
                  borderColor: rgbToHex(tag.colorDTO),
                  color: rgbToHex(tag.colorDTO),
                }"
              >
                <i class="pi pi-tag" />
                {{ tag.label }}
              </div>
              <button v-if="tags.length > 6" class="px-4 py-2 bg-yellow-400/10 border-2 border-yellow-400 rounded-full text-yellow-600 text-xs font-semibold cursor-pointer transition-all hover:bg-yellow-400/20 hover:-translate-y-0.5" @click="navigateTo('/tag')">
                +{{ tags.length - 6 }} autres
              </button>
            </div>
          </div>
        </div>
      </section>

      <section class="grid grid-cols-[repeat(auto-fit,minmax(280px,1fr))] gap-6 mb-8">
        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-lg font-bold m-0 flex items-center gap-2" style="color: var(--text-primary);">
              <i class="pi pi-bell text-orange-500" />
              Alertes de la période
            </h2>
            <span class="text-xs font-semibold px-2 py-1 rounded-full" style="background-color: var(--bg-tertiary); color: var(--text-secondary);">
              {{ dashboardAlerts.length }} active(s)
            </span>
          </div>

          <div v-if="dashboardAlerts.length === 0" class="text-sm" style="color: var(--text-secondary);">
            Aucun signal particulier sur cette période
          </div>
          <div v-else class="flex flex-col gap-3">
            <div v-for="alert in dashboardAlerts" :key="alert.key" class="rounded-xl p-3 border" :class="alert.level === 'danger' ? 'bg-red-500/8 border-red-500/25' : (alert.level === 'warning' ? 'bg-yellow-500/10 border-yellow-500/25' : 'bg-blue-500/8 border-blue-500/25')">
              <p class="text-sm font-semibold m-0" style="color: var(--text-primary);">
                {{ alert.title }}
              </p>
              <p class="text-xs m-0 mt-1" style="color: var(--text-secondary);">
                {{ alert.detail }}
              </p>
            </div>
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <h2 class="text-lg font-bold m-0 mb-4 flex items-center gap-2" style="color: var(--text-primary);">
            <i class="pi pi-bolt text-purple-600" />
            Actions rapides
          </h2>
          <div class="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <button class="quick-action-btn" @click="navigateTo('/account')">
              <i class="pi pi-wallet" />
              Voir mes comptes
            </button>
            <button class="quick-action-btn" @click="navigateTo('/regular-transaction')">
              <i class="pi pi-calendar" />
              Ajuster les régulières
            </button>
            <button class="quick-action-btn" @click="navigateTo('/tag')">
              <i class="pi pi-tags" />
              Revoir mes tags
            </button>
          </div>
        </div>
      </section>

      <!-- Quick Stats Banner -->
      <section class="grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] gap-5 p-6 rounded-2xl shadow-lg mb-5" style="background-color: var(--card-bg);">
        <div class="flex items-center gap-4">
          <i class="pi pi-calendar-plus text-4xl text-purple-600" />
          <div>
            <p class="text-2xl font-extrabold m-0 mb-1" style="color: var(--text-primary);">
              {{ regularTransactions.length }}
            </p>
            <p class="text-xs m-0" style="color: var(--text-secondary);">
              Mensualités actives
            </p>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <i class="pi pi-tags text-4xl text-purple-600" />
          <div>
            <p class="text-2xl font-extrabold m-0 mb-1" style="color: var(--text-primary);">
              {{ tags.length }}
            </p>
            <p class="text-xs m-0" style="color: var(--text-secondary);">
              Tags créés
            </p>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <i class="pi pi-clock text-4xl text-purple-600" />
          <div>
            <p class="text-2xl font-extrabold m-0 mb-1" style="color: var(--text-primary);">
              {{ totalPrevisionalTransactions }}
            </p>
            <p class="text-xs m-0" style="color: var(--text-secondary);">
              Transactions prévisionnelles
            </p>
          </div>
        </div>
        <div class="flex items-center gap-4">
          <i class="pi pi-chart-line text-4xl text-purple-600" />
          <div>
            <p class="text-2xl font-extrabold m-0 mb-1" style="color: var(--text-primary);">
              {{ categoryDistribution?.categories.length || 0 }}
            </p>
            <p class="text-xs m-0" style="color: var(--text-secondary);">
              Catégories actives
            </p>
          </div>
        </div>
      </section>
    </div>
  </div>

  <BookletBookingDialog
    :digit="0.00"
    :visible="isAccountDialogOpen"
    @create-account="handleAccountCreation"
    @cancel="cancel"
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
  background: #822acc;
  border-radius: 10px;
}

*::-webkit-scrollbar-thumb:hover {
  background: #651e9e;
}

/* Chart container responsive heights */
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
  background-color: #822acc;
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
  transition: background-color 0.2s ease, border-color 0.2s ease;
}

.quick-action-btn:hover {
  background-color: var(--card-bg);
  border-color: #822acc;
}

@media (min-width: 640px) {
  .chart-container.h-70 {
    height: 18rem;
  }

  .chart-container.h-75 {
    height: 20rem;
  }
}

@media (max-width: 639px) {
  .chart-container.h-70 {
    height: 15rem;
  }

  .chart-container.h-75 {
    height: 18rem;
  }
}
</style>
