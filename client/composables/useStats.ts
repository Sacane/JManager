import useQuery from './useQuery'

export default function useStats() {
  const { get } = useQuery()

  async function getMonthlyAccountStats(accountId: string, year: number): Promise<MonthlyAccountStatsDTO> {
    return get(`stats/monthly/${accountId}/${year}`)
  }

  async function getCategoryDistribution(): Promise<CategoryDistributionDTO> {
    return get('stats/category-distribution')
  }

  async function getTrendStats(): Promise<TrendStatsDTO> {
    return get('stats/trends')
  }

  async function getPrevisionalTransactions(startDate: string, endDate: string): Promise<PrevisionalTransactionsDTO> {
    return get('stats/previsional', {
      startDate,
      endDate,
    })
  }

  return {
    getMonthlyAccountStats,
    getCategoryDistribution,
    getTrendStats,
    getPrevisionalTransactions,
  }
}
