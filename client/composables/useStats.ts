import useQuery from './useQuery'

interface StatsFilters {
  bookletId?: string | number
  startDate?: string
  endDate?: string
}

export default function useStats() {
  const { get } = useQuery()

  async function getMonthlyAccountStats(bookletId: string, year: number): Promise<MonthlyAccountStatsDTO> {
    return get(`stats/monthly/${bookletId}/${year}`)
  }

  async function getCategoryDistribution(filters: StatsFilters = {}): Promise<CategoryDistributionDTO> {
    return get('stats/category-distribution', filters)
  }

  async function getTrendStats(filters: StatsFilters = {}): Promise<TrendStatsDTO> {
    return get('stats/trends', filters)
  }

  async function getPrevisionalTransactions(startDate: string, endDate: string, bookletId?: string): Promise<PrevisionalTransactionsDTO> {
    return get('stats/previsional', {
      startDate,
      endDate,
      bookletId,
    })
  }

  async function getDailyTrendStats(startDate: string, endDate: string, bookletId?: string): Promise<DailyTrendStatsDTO> {
    return get('stats/daily-trends', {
      startDate,
      endDate,
      bookletId,
    })
  }

  return {
    getMonthlyAccountStats,
    getCategoryDistribution,
    getTrendStats,
    getPrevisionalTransactions,
    getDailyTrendStats,
  }
}
