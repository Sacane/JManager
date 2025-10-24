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
import { rgbToHex } from '~/utils/util'

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
const selectedPeriod = ref<'month' | 'year'>('month')
const isLoading = ref(true)

// Animation refs
const overviewRef = ref(null)
const chartsRef = ref(null)
const isOverviewVisible = ref(false)
const isChartsVisible = ref(false)

// Setup intersection observers
useIntersectionObserver(overviewRef, ([{ isIntersecting }]) => {
  if (isIntersecting) {
    isOverviewVisible.value = true
  }
}, { threshold: 0.1 })

useIntersectionObserver(chartsRef, ([{ isIntersecting }]) => {
  if (isIntersecting) {
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
        data: Array.from({ length: 4 }).fill(currentExpenses / 4),
        backgroundColor: '#822acc',
        borderRadius: 8,
      },
      {
        label: 'Mois dernier',
        data: Array.from({ length: 4 }).fill(previousExpenses / 4),
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
  <div class="dashboard-container">
    <!-- Header Section -->
    <div class="dashboard-header">
      <div class="header-content">
        <div class="welcome-section">
          <h1 class="dashboard-title">
            Bonjour, {{ user?.username }} 👋
          </h1>
          <p class="dashboard-subtitle">
            Voici un aperçu de vos finances au {{ new Date().toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }) }}
          </p>
        </div>
        <!--
          <div class="header-actions">
            <button
              class="period-toggle"
              :class="{ active: selectedPeriod === 'month' }"
              @click="selectedPeriod = 'month'"
            >
              Mois
            </button>
            <button
              class="period-toggle"
              :class="{ active: selectedPeriod === 'year' }"
              @click="selectedPeriod = 'year'"
            >
              Année
            </button>
          </div>
        ! -->
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="isLoading" class="loading-container">
      <i class="pi pi-spin pi-spinner loading-icon" />
      <p>Chargement de vos données...</p>
    </div>

    <!-- Main Content -->
    <div v-else class="dashboard-content">
      <!-- KPI Cards -->
      <section ref="overviewRef" class="kpi-section" :class="{ visible: isOverviewVisible }">
        <div class="kpi-card balance-card">
          <div class="kpi-header">
            <div class="kpi-icon gradient-purple">
              <i class="pi pi-wallet" />
            </div>
            <span v-if="balanceGrowth !== 0" class="kpi-trend" :class="balanceGrowth > 0 ? 'positive' : 'negative'">
              <i :class="balanceGrowth > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
              {{ Math.abs(balanceGrowth).toFixed(1) }}%
            </span>
          </div>
          <div class="kpi-content">
            <h3 class="kpi-label">
              Solde total
            </h3>
            <p class="kpi-value">
              {{ totalBalance.toFixed(2) }} €
            </p>
            <p class="kpi-info">
              {{ accounts.length }} livret{{ accounts.length > 1 ? 's' : '' }} actif{{ accounts.length > 1 ? 's' : '' }}
            </p>
          </div>
        </div>

        <div class="kpi-card expenses-card">
          <div class="kpi-header">
            <div class="kpi-icon gradient-red">
              <i class="pi pi-arrow-down" />
            </div>
            <span v-if="expensesGrowth !== 0" class="kpi-trend" :class="expensesGrowth > 0 ? 'negative' : 'positive'">
              <i :class="expensesGrowth > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
              {{ Math.abs(expensesGrowth).toFixed(1) }}%
            </span>
          </div>
          <div>
            <h3 class="kpi-label">
              Dépenses du mois
            </h3>
            <p class="kpi-value">
              {{ monthlyExpenses.toFixed(2) }} €
            </p>
            <p class="kpi-info">
              Moy. journalière: {{ (monthlyExpenses / 30).toFixed(2) }} €
            </p>
          </div>
        </div>

        <div class="kpi-card income-card">
          <div class="kpi-header">
            <div class="kpi-icon gradient-green">
              <i class="pi pi-arrow-up" />
            </div>
            <span v-if="incomeGrowth !== 0" class="kpi-trend" :class="incomeGrowth > 0 ? 'positive' : 'negative'">
              <i :class="incomeGrowth > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
              {{ Math.abs(incomeGrowth).toFixed(1) }}%
            </span>
          </div>
          <div class="kpi-content">
            <h3 class="kpi-label">
              Revenus du mois
            </h3>
            <p class="kpi-value">
              {{ monthlyIncome.toFixed(2) }} €
            </p>
            <p class="kpi-info">
              Épargne: {{ (monthlyIncome - monthlyExpenses).toFixed(2) }} €
            </p>
          </div>
        </div>

        <div class="kpi-card savings-card">
          <div class="kpi-header">
            <div class="kpi-icon gradient-yellow">
              <i class="pi pi-chart-line" />
            </div>
            <span v-if="savingsRate !== 0" class="kpi-trend" :class="savingsRate > 0 ? 'positive' : 'negative'">
              <i :class="savingsRate > 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'" />
              {{ Math.abs(savingsRate).toFixed(1) }}%
            </span>
          </div>
          <div class="kpi-content">
            <h3 class="kpi-label">
              Taux d'épargne
            </h3>
            <p class="kpi-value">
              {{ savingsRate.toFixed(1) }}%
            </p>
            <p class="kpi-info">
              Objectif: 30%
            </p>
          </div>
        </div>
      </section>

      <!-- Charts Section -->
      <section ref="chartsRef" class="charts-section" :class="{ visible: isChartsVisible }">
        <div class="chart-card large-chart">
          <div class="chart-header">
            <h2 class="chart-title">
              <i class="pi pi-chart-line" />
              Évolution des finances
            </h2>
            <p class="chart-subtitle">
              Comparaison revenus vs dépenses sur 12 mois
            </p>
          </div>
          <div class="chart-body">
            <Line :data="expensesTrendData" :options="chartOptions" />
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <h2 class="chart-title">
              <i class="pi pi-chart-pie" />
              Dépenses par catégorie
            </h2>
            <p class="chart-subtitle">
              Répartition totale: {{ categoryDistribution?.totalExpenses || '0.00' }} €
            </p>
          </div>
          <div class="chart-body doughnut-container">
            <Doughnut :data="categoryExpensesData" :options="doughnutOptions" />
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-header">
            <h2 class="chart-title">
              <i class="pi pi-chart-bar" />
              Comparaison hebdomadaire
            </h2>
            <p class="chart-subtitle">
              Ce mois vs mois dernier
            </p>
          </div>
          <div class="chart-body">
            <Bar :data="monthlyComparisonData" :options="chartOptions" />
          </div>
        </div>
      </section>

      <!-- Quick Actions & Info Section -->
      <section class="info-section">
        <div class="info-card accounts-card">
          <div class="info-header">
            <h2 class="info-title">
              <i class="pi pi-book" />
              Mes livrets
            </h2>
            <button class="add-button" @click="isAccountDialogOpen = true">
              <i class="pi pi-plus" />
              Nouveau
            </button>
          </div>
          <div class="info-body">
            <div v-if="accounts.length === 0" class="empty-state">
              <i class="pi pi-inbox" />
              <p>Aucun livret créé</p>
              <button class="create-button" @click="isAccountDialogOpen = true">
                Créer mon premier livret
              </button>
            </div>
            <div v-else class="accounts-list">
              <div
                v-for="account in accounts.slice(0, 4)"
                :key="account.id"
                class="account-item"
                @click="navigateTo(`/account/${account.id}`)"
              >
                <div class="account-icon">
                  <i class="pi pi-wallet" />
                </div>
                <div class="account-details">
                  <p class="account-name">
                    {{ account.labelAccount }}
                  </p>
                  <p class="account-balance">
                    {{ Number.parseFloat(account.amount.toString()).toFixed(2) }} €
                  </p>
                </div>
                <i class="pi pi-chevron-right" />
              </div>
              <button v-if="accounts.length > 4" class="view-all" @click="navigateTo('/accounts')">
                Voir tous les livrets ({{ accounts.length }})
              </button>
            </div>
          </div>
        </div>

        <div class="info-card upcoming-card">
          <div class="info-header">
            <h2 class="info-title">
              <i class="pi pi-calendar" />
              Prochaines transactions
            </h2>
            <button class="add-button" @click="navigateTo('/regular-transaction')">
              <i class="pi pi-cog" />
              Gérer
            </button>
          </div>
          <div class="info-body">
            <div v-if="upcomingPayments.length === 0" class="empty-state">
              <i class="pi pi-calendar-times" />
              <p>Aucune transaction prévue</p>
              <button class="create-button" @click="navigateTo('/regular-transaction')">
                Configurer une mensualité
              </button>
            </div>
            <div v-else class="payments-list">
              <div v-for="payment in upcomingPayments" :key="payment.id" class="payment-item">
                <div class="payment-icon" :class="{ expense: !payment.isIncome, income: payment.isIncome }">
                  <i :class="!payment.isIncome ? 'pi pi-arrow-down' : 'pi pi-arrow-up'" />
                </div>
                <div class="payment-details">
                  <p class="payment-label">
                    {{ payment.label }}
                  </p>
                  <p class="payment-frequency">
                    {{ new Date(payment.date).toLocaleDateString('fr-FR') }}
                  </p>
                </div>
                <p class="payment-amount" :class="{ expense: !payment.isIncome, income: payment.isIncome }">
                  {{ !payment.isIncome ? '-' : '+' }}{{ Number.parseFloat(payment.amount).toFixed(2) }} €
                </p>
              </div>
            </div>
          </div>
        </div>

        <div class="info-card tags-card">
          <div class="info-header">
            <h2 class="info-title">
              <i class="pi pi-tags" />
              Tags populaires
            </h2>
            <button class="add-button" @click="navigateTo('/tag')">
              <i class="pi pi-plus" />
              Nouveau
            </button>
          </div>
          <div class="info-body">
            <div v-if="tags.length === 0" class="empty-state">
              <i class="pi pi-tag" />
              <p>Aucun tag créé</p>
              <button class="create-button" @click="navigateTo('/tag')">
                Créer un tag
              </button>
            </div>
            <div v-else class="tags-list">
              <div
                v-for="tag in tags.slice(0, 6)"
                :key="tag.tagId"
                class="tag-chip"
                :style="{
                  backgroundColor: `${rgbToHex(tag.colorDTO)}20`,
                  borderColor: rgbToHex(tag.colorDTO),
                  color: rgbToHex(tag.colorDTO),
                }"
              >
                <i class="pi pi-tag" />
                {{ tag.label }}
              </div>
              <button v-if="tags.length > 6" class="view-all-tags" @click="navigateTo('/tag')">
                +{{ tags.length - 6 }} autres
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Quick Stats Banner -->
      <section class="stats-banner">
        <div class="stat-item">
          <i class="pi pi-calendar-plus" />
          <div>
            <p class="stat-value">
              {{ regularTransactions.length }}
            </p>
            <p class="stat-label">
              Mensualités actives
            </p>
          </div>
        </div>
        <div class="stat-item">
          <i class="pi pi-tags" />
          <div>
            <p class="stat-value">
              {{ tags.length }}
            </p>
            <p class="stat-label">
              Tags créés
            </p>
          </div>
        </div>
        <div class="stat-item">
          <i class="pi pi-clock" />
          <div>
            <p class="stat-value">
              {{ totalPrevisionalTransactions }}
            </p>
            <p class="stat-label">
              Transactions prévisionnelles
            </p>
          </div>
        </div>
        <div class="stat-item">
          <i class="pi pi-chart-line" />
          <div>
            <p class="stat-value">
              {{ categoryDistribution?.categories.length || 0 }}
            </p>
            <p class="stat-label">
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
.dashboard-container {
  width: 100%;
  min-height: 100vh;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  padding: 20px;
  position: relative;
}

/* Ensure background extends to full height */
.dashboard-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #f9fafb 0%, #f3f4f6 100%);
  z-index: -1;
}

/* ===== HEADER ===== */
.dashboard-header {
  margin-bottom: 30px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
}

.dashboard-title {
  font-size: clamp(1.75rem, 3vw, 2.5rem);
  font-weight: 800;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.dashboard-subtitle {
  font-size: 1rem;
  color: #6b7280;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  background: white;
  padding: 4px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.period-toggle {
  padding: 10px 24px;
  border: none;
  background: transparent;
  border-radius: 8px;
  font-weight: 600;
  color: #6b7280;
  cursor: pointer;
  transition: all 0.3s ease;
}

.period-toggle.active {
  background: linear-gradient(135deg, #822acc, #651e9e);
  color: white;
}

/* ===== LOADING ===== */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
  min-height: 60vh;
}

.loading-icon {
  font-size: 48px;
  color: #822acc;
}

/* ===== KPI SECTION ===== */
.kpi-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s ease;
}

.kpi-section.visible {
  opacity: 1;
  transform: translateY(0);
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.kpi-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.gradient-purple {
  background: linear-gradient(135deg, #822acc, #651e9e);
}

.gradient-red {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.gradient-green {
  background: linear-gradient(135deg, #10b981, #059669);
}

.gradient-yellow {
  background: linear-gradient(135deg, #e0d824, #d4c91e);
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.kpi-trend.positive {
  background: #10b98120;
  color: #10b981;
}

.kpi-trend.negative {
  background: #ef444420;
  color: #ef4444;
}

.kpi-label {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.kpi-value {
  font-size: 2rem;
  font-weight: 800;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.kpi-info {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

/* ===== CHARTS SECTION ===== */
.charts-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s ease 0.2s;
}

.charts-section.visible {
  opacity: 1;
  transform: translateY(0);
}

.chart-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.chart-card.large-chart {
  grid-column: 1 / -1;
}

.chart-header {
  margin-bottom: 20px;
}

.chart-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 6px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.chart-title i {
  color: #822acc;
}

.chart-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.chart-body {
  height: 300px;
  position: relative;
}

.doughnut-container {
  height: 280px;
}

/* ===== DASHBOARD CONTENT ===== */
.dashboard-content {
  position: relative;
  z-index: 1;
  padding-bottom: 40px;
}

/* ===== KPI SECTION ===== */
.kpi-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s ease;
}

.kpi-section.visible {
  opacity: 1;
  transform: translateY(0);
}

.kpi-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.kpi-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.gradient-purple {
  background: linear-gradient(135deg, #822acc, #651e9e);
}

.gradient-red {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.gradient-green {
  background: linear-gradient(135deg, #10b981, #059669);
}

.gradient-yellow {
  background: linear-gradient(135deg, #e0d824, #d4c91e);
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.kpi-trend.positive {
  background: #10b98120;
  color: #10b981;
}

.kpi-trend.negative {
  background: #ef444420;
  color: #ef4444;
}

.kpi-label {
  font-size: 14px;
  color: #6b7280;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.kpi-value {
  font-size: 2rem;
  font-weight: 800;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.kpi-info {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
}

/* ===== CHARTS SECTION ===== */
.charts-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
  opacity: 0;
  transform: translateY(20px);
  transition: all 0.6s ease 0.2s;
}

.charts-section.visible {
  opacity: 1;
  transform: translateY(0);
}

.chart-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.chart-card.large-chart {
  grid-column: 1 / -1;
}

.chart-header {
  margin-bottom: 20px;
}

.chart-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 6px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.chart-title i {
  color: #822acc;
}

.chart-subtitle {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.chart-body {
  height: 300px;
  position: relative;
}

.doughnut-container {
  height: 280px;
}

/* ===== INFO SECTION ===== */
.info-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 24px;
  margin-bottom: 30px;
}

.info-card {
  background: white;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f3f4f6;
}

.info-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.info-title i {
  color: #822acc;
}

.add-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, #822acc, #651e9e);
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.add-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(130, 42, 204, 0.3);
}

.info-body {
  max-height: 350px;
  overflow-y: auto;
}

/* Empty State */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  text-align: center;
  gap: 16px;
}

.empty-state i {
  font-size: 48px;
  color: #d1d5db;
}

.empty-state p {
  color: #6b7280;
  margin: 0;
}

.create-button {
  padding: 10px 20px;
  background: linear-gradient(135deg, #822acc, #651e9e);
  color: white;
  border: none;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.create-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(130, 42, 204, 0.3);
}

/* Accounts List */
.accounts-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.account-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.account-item:hover {
  background: #822acc10;
  transform: translateX(5px);
}

.account-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, #822acc, #651e9e);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 20px;
  flex-shrink: 0;
}

.account-details {
  flex: 1;
}

.account-name {
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.account-balance {
  font-size: 14px;
  color: #6b7280;
  margin: 0;
}

.view-all {
  width: 100%;
  padding: 12px;
  background: transparent;
  border: 2px dashed #d1d5db;
  border-radius: 10px;
  color: #6b7280;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.view-all:hover {
  border-color: #822acc;
  color: #822acc;
  background: #822acc05;
}

/* Payments List */
.payments-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.payment-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 12px;
}

.payment-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  flex-shrink: 0;
}

.payment-icon.expense {
  background: linear-gradient(135deg, #ef4444, #dc2626);
}

.payment-icon.income {
  background: linear-gradient(135deg, #10b981, #059669);
}

.payment-details {
  flex: 1;
}

.payment-label {
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
  font-size: 14px;
}

.payment-frequency {
  font-size: 12px;
  color: #6b7280;
  margin: 0;
}

.payment-amount {
  font-weight: 700;
  font-size: 16px;
}

.payment-amount.expense {
  color: #ef4444;
}

.payment-amount.income {
  color: #10b981;
}

/* Tags List */
.tags-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 2px solid;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.tag-chip:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.view-all-tags {
  padding: 8px 16px;
  background: #e0d82420;
  border: 2px solid #e0d824;
  border-radius: 20px;
  color: #b8a920;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.view-all-tags:hover {
  background: #e0d82440;
  transform: translateY(-2px);
}

/* ===== STATS BANNER ===== */
.stats-banner {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  background: white;
  padding: 24px;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-item i {
  font-size: 32px;
  color: #822acc;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 800;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.stat-label {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

/* ===== RESPONSIVE ===== */
@media (max-width: 768px) {
  .dashboard-container {
    padding: 16px;
  }

  .header-content {
    flex-direction: column;
    align-items: flex-start;
  }

  .kpi-section,
  .charts-section,
  .info-section {
    grid-template-columns: 1fr;
  }

  .chart-card.large-chart {
    grid-column: 1;
  }

  .chart-body {
    height: 250px;
  }

  .doughnut-container {
    height: 230px;
  }

  .stats-banner {
    grid-template-columns: 1fr;
  }

  .period-toggle {
    padding: 8px 16px;
    font-size: 14px;
  }
}

/* Scrollbar Styling */
.info-body::-webkit-scrollbar {
  width: 6px;
}

.info-body::-webkit-scrollbar-track {
  background: #f3f4f6;
  border-radius: 10px;
}

.info-body::-webkit-scrollbar-thumb {
  background: #822acc;
  border-radius: 10px;
}

.info-body::-webkit-scrollbar-thumb:hover {
  background: #651e9e;
}
</style>
