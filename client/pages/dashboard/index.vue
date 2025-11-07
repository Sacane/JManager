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
import { addMonths, endOfMonth, format, startOfMonth } from 'date-fns'
import { fr } from 'date-fns/locale'
import { Bar, Doughnut, Line } from 'vue-chartjs'
import useAuth from '@/composables/useAuth'
import BookletBookingDialog from '~/components/dialog/BookletBookingDialog.vue'
import useStats from '~/composables/useStats'
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
const toast = useJToast()

// Refs
const isAccountDialogOpen = ref(false)
const accounts = ref<BookletDTO[]>([])
const regularTransactions = ref<RegularTransactionDTO[]>([])
const tags = ref<TagDTO[]>([])
const categoryDistribution = ref<CategoryDistributionDTO | null>(null)
const trendStats = ref<TrendStatsDTO | null>(null)
const previsionalTransactions = ref<PrevisionalTransactionsDTO | null>(null)
const isLoading = ref(true)

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

const currentMonthTrend = computed(() => {
  if (!trendStats.value?.monthlyTrends.length) {
    return null
  }

  const currentMonth = new Date().getMonth() + 1
  const currentYear = new Date().getFullYear()

  return trendStats.value.monthlyTrends.find(
    trend => trend.month === currentMonth && trend.year === currentYear,
  )
})

const previousMonthTrend = computed(() => {
  if (!trendStats.value?.monthlyTrends.length) {
    return null
  }

  const lastMonth = new Date()
  lastMonth.setMonth(lastMonth.getMonth() - 1)
  const month = lastMonth.getMonth() + 1
  const year = lastMonth.getFullYear()

  return trendStats.value.monthlyTrends.find(
    trend => trend.month === month && trend.year === year,
  )
})

const monthlyExpenses = computed(() => {
  const expenses = currentMonthTrend.value?.expenses
  return expenses ? Number.parseFloat(expenses) : 0
})

const monthlyIncome = computed(() => {
  const income = currentMonthTrend.value?.income
  return income ? Number.parseFloat(income) : 0
})

const expensesGrowth = computed(() => {
  if (!currentMonthTrend.value || !previousMonthTrend.value) {
    return 0
  }

  const current = Number.parseFloat(currentMonthTrend.value.expenses)
  const previous = Number.parseFloat(previousMonthTrend.value.expenses)

  if (previous === 0) {
    return 0
  }

  return ((current - previous) / previous * 100)
})

const incomeGrowth = computed(() => {
  if (!currentMonthTrend.value || !previousMonthTrend.value) {
    return 0
  }

  const current = Number.parseFloat(currentMonthTrend.value.income)
  const previous = Number.parseFloat(previousMonthTrend.value.income)

  if (previous === 0) {
    return 0
  }

  return ((current - previous) / previous * 100)
})

const balanceGrowth = computed(() => {
  if (!currentMonthTrend.value || !previousMonthTrend.value) {
    return 0
  }

  const current = Number.parseFloat(currentMonthTrend.value.balance)
  const previous = Number.parseFloat(previousMonthTrend.value.balance)

  if (previous === 0) {
    return 0
  }

  return ((current - previous) / previous * 100)
})

const savingsRate = computed(() => {
  if (monthlyIncome.value === 0 || monthlyExpenses.value > monthlyIncome.value) {
    return 0
  }

  return ((monthlyIncome.value - monthlyExpenses.value) / monthlyIncome.value * 100)
})

const upcomingPayments = computed(() =>
  previsionalTransactions.value?.transactions.slice(0, 5) || [],
)

const totalPrevisionalTransactions = computed(() =>
  previsionalTransactions.value?.transactions.length || 0,
)

// Chart data
const expensesTrendData = computed(() => {
  if (!trendStats.value?.monthlyTrends.length) {
    return {
      labels: [],
      datasets: [],
    }
  }

  // Get last 12 months
  const sortedTrends = [...trendStats.value.monthlyTrends]
    .sort((a, b) => {
      if (a.year !== b.year) {
        return a.year - b.year
      }
      return a.month - b.month
    })
    .slice(-12)

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

  const sortedCategories = [...categoryDistribution.value.categories]
    .sort((a, b) => Number.parseFloat(b.totalAmount) - Number.parseFloat(a.totalAmount))
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

const monthlyComparisonData = computed(() => {
  if (!trendStats.value?.monthlyTrends.length) {
    return {
      labels: [],
      datasets: [],
    }
  }

  const now = new Date()
  const currentMonthData = trendStats.value.monthlyTrends.find(
    t => t.month === now.getMonth() + 1 && t.year === now.getFullYear(),
  )

  const prevMonth = new Date(now.getFullYear(), now.getMonth() - 1)
  const previousMonthData = trendStats.value.monthlyTrends.find(
    t => t.month === prevMonth.getMonth() + 1 && t.year === prevMonth.getFullYear(),
  )

  const currentExpenses = currentMonthData ? Number.parseFloat(currentMonthData.expenses) : 0
  const previousExpenses = previousMonthData ? Number.parseFloat(previousMonthData.expenses) : 0

  return {
    labels: ['Semaine 1', 'Semaine 2', 'Semaine 3', 'Semaine 4'],
    datasets: [
      {
        label: 'Ce mois',
        data: Array.from({ length: 4 }).fill(currentExpenses / 4) as number[],
        backgroundColor: '#822acc',
        borderRadius: 8,
      },
      {
        label: 'Mois dernier',
        data: Array.from({ length: 4 }).fill(previousExpenses / 4) as number[],
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
  isLoading.value = true
  try {
    // Load basic data
    const [accountsData, regularTransData, tagsData] = await Promise.all([
      fetchBooklets(),
      getRegularTransaction().catch(() => []),
      getAllTags().catch(() => []),
    ])

    accounts.value = accountsData
    regularTransactions.value = regularTransData
    tags.value = tagsData

    // Load stats data
    const now = new Date()
    const startDate = format(startOfMonth(now), 'yyyy-MM-dd')
    const endDate = format(endOfMonth(addMonths(now, 3)), 'yyyy-MM-dd')

    const [categoryData, trendsData, previsionalData] = await Promise.all([
      getCategoryDistribution().catch(() => null),
      getTrendStats().catch(() => null),
      getPrevisionalTransactions(startDate, endDate).catch(() => null),
    ])

    console.warn(trendsData)

    categoryDistribution.value = categoryData
    trendStats.value = trendsData
    previsionalTransactions.value = previsionalData
  } catch (error) {
    toast.error('Erreur lors du chargement des données')
    console.error(error)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  loadDashboardData()
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
            Voici un aperçu de vos finances au {{ new Date().toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }) }}
          </p>
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
              Solde total
            </h3>
            <p class="text-3xl font-extrabold mb-2" style="color: var(--text-primary);">
              {{ totalBalance.toFixed(2) }} €
            </p>
            <p class="text-xs" style="color: var(--text-tertiary);">
              {{ accounts.length }} livret{{ accounts.length > 1 ? 's' : '' }} actif{{ accounts.length > 1 ? 's' : '' }}
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
              Dépenses du mois
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
              Revenus du mois
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
      <section ref="chartsRef" class="grid grid-cols-[repeat(auto-fit,minmax(400px,1fr))] gap-6 mb-8 opacity-0 translate-y-5 transition-all duration-600 delay-200" :class="{ 'opacity-100 translate-y-0': isChartsVisible }">
        <div class="rounded-2xl p-6 shadow-lg col-span-full" style="background-color: var(--card-bg);">
          <div class="mb-5">
            <h2 class="text-xl font-bold mb-1.5 flex items-center gap-2.5" style="color: var(--text-primary);">
              <i class="pi pi-chart-line text-purple-600" />
              Évolution des finances
            </h2>
            <p class="text-sm" style="color: var(--text-secondary);">
              Comparaison revenus vs dépenses sur 12 mois
            </p>
          </div>
          <div class="h-75 relative">
            <Line :data="expensesTrendData" :options="chartOptions" />
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="mb-5">
            <h2 class="text-xl font-bold mb-1.5 flex items-center gap-2.5" style="color: var(--text-primary);">
              <i class="pi pi-chart-pie text-purple-600" />
              Dépenses par catégorie
            </h2>
            <p class="text-sm" style="color: var(--text-secondary);">
              Répartition totale: {{ categoryDistribution?.totalExpenses || '0.00' }} €
            </p>
          </div>
          <div class="h-70 relative">
            <Doughnut :data="categoryExpensesData" :options="doughnutOptions" />
          </div>
        </div>

        <div class="rounded-2xl p-6 shadow-lg" style="background-color: var(--card-bg);">
          <div class="mb-5">
            <h2 class="text-xl font-bold mb-1.5 flex items-center gap-2.5" style="color: var(--text-primary);">
              <i class="pi pi-chart-bar text-purple-600" />
              Comparaison hebdomadaire
            </h2>
            <p class="text-sm" style="color: var(--text-secondary);">
              Ce mois vs mois dernier
            </p>
          </div>
          <div class="h-75 relative">
            <Bar :data="monthlyComparisonData" :options="chartOptions" />
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
            <div v-if="upcomingPayments.length === 0" class="flex flex-col items-center justify-center py-10 px-5 text-center gap-4">
              <i class="pi pi-calendar-times text-5xl" style="color: var(--text-muted);" />
              <p class="m-0" style="color: var(--text-secondary);">
                Aucune transaction prévue
              </p>
              <button class="px-5 py-2.5 bg-gradient-to-br from-purple-600 to-purple-700 text-white border-none rounded-lg font-semibold cursor-pointer transition-all hover:-translate-y-0.5 hover:shadow-lg" @click="navigateTo('/regular-transaction')">
                Configurer une mensualité
              </button>
            </div>
            <div v-else class="flex flex-col gap-3">
              <div v-for="payment in upcomingPayments" :key="payment.id ?? `payment-${Math.random()}`" class="flex items-center gap-4 p-3 rounded-xl" style="background-color: var(--bg-tertiary);">
                <div class="w-10 h-10 rounded-lg flex items-center justify-center text-white text-lg flex-shrink-0" :class="!payment.isIncome ? 'bg-gradient-to-br from-red-500 to-red-600' : 'bg-gradient-to-br from-green-500 to-green-600'">
                  <i :class="!payment.isIncome ? 'pi pi-arrow-down' : 'pi pi-arrow-up'" />
                </div>
                <div class="flex-1">
                  <p class="font-semibold m-0 mb-1 text-sm" style="color: var(--text-primary);">
                    {{ payment.label }}
                  </p>
                  <p class="text-xs m-0" style="color: var(--text-secondary);">
                    {{ new Date(payment.date).toLocaleDateString('fr-FR') }}
                  </p>
                </div>
                <p class="font-bold text-base m-0" :class="!payment.isIncome ? 'text-red-500' : 'text-green-500'">
                  {{ !payment.isIncome ? '-' : '+' }}{{ Number.parseFloat(payment.amount).toFixed(2) }} €
                </p>
              </div>
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
</style>
