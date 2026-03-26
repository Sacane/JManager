import useQuery from './useQuery'

interface StatsFilters {
  accountId?: string | number
  startDate?: string
  endDate?: string
}

export default function useStats() {
  const { get } = useQuery()

  async function getMonthlyAccountStats(accountId: string, year: number): Promise<MonthlyAccountStatsDTO> {
    return get(`stats/monthly/${accountId}/${year}`)
  }

  async function getCategoryDistribution(filters: StatsFilters = {}): Promise<CategoryDistributionDTO> {
    return get('stats/category-distribution', filters)
  }

  async function getTrendStats(filters: StatsFilters = {}): Promise<TrendStatsDTO> {
    return get('stats/trends', filters)
  }

  async function getPrevisionalTransactions(startDate: string, endDate: string, accountId?: string): Promise<PrevisionalTransactionsDTO> {
    return get('stats/previsional', {
      startDate,
      endDate,
      accountId,
    })
  }

  return {
    getMonthlyAccountStats,
    getCategoryDistribution,
    getTrendStats,
    getPrevisionalTransactions,
  }
}
