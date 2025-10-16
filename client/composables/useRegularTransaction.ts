export type Frequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export default function useRegularTransaction() {
  const { post, get } = useQuery()

  async function saveMonthlyTransaction(monthlyTransaction: MonthlyTransactionCreationRequest): Promise<RegularTransactionDTO> {
    return post('transaction/monthly', monthlyTransaction)
  }

  async function getRegularTransaction(): Promise<RegularTransactionDTO[]> {
    return get('transaction/regular')
  }
  async function getRegularTransactionById(id: string): Promise<RegularTransactionDTO> {
    return get(`transaction/regular/${id}`)
  }

  return { getRegularTransaction, saveMonthlyTransaction, getRegularTransactionById }
}
