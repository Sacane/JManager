export type Frequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export default function useRegularTransaction() {
  const { post, get, patch, deleteQuery } = useQuery()

  async function saveMonthlyTransaction(monthlyTransaction: MonthlyTransactionCreationRequest): Promise<RegularTransactionDTO> {
    return post('transaction/monthly', monthlyTransaction)
  }

  async function getRegularTransaction(page: number = 0, size: number = 10): Promise<PageDTO<RegularTransactionDTO>> {
    return get('transaction/regular', { page, size })
  }

  async function getRegularTransactionById(id: string): Promise<RegularTransactionDTO> {
    return get(`transaction/regular/${id}`)
  }

  async function updateRegularTransaction(transaction: UpdateRegularTransactionRequest): Promise<RegularTransactionDTO> {
    return patch('transaction/regular', transaction)
  }

  async function deleteRegularTransaction(id: string): Promise<void> {
    return deleteQuery(`transaction/regular/${id}`, {})
  }

  async function deleteRegularTransactions(request: RegularTransactionsDeletionRequest): Promise<void> {
    return deleteQuery('transaction/regular', request)
  }

  async function linkRegularTransactionToBooklet(transactionId: string, bookletId: string): Promise<RegularTransactionDTO> {
    return post(`transaction/regular/${transactionId}/link/${bookletId}`, {})
  }

  async function unlinkRegularTransactionFromBooklet(transactionId: string, bookletId: string): Promise<RegularTransactionDTO> {
    return deleteQuery(`transaction/regular/${transactionId}/link/${bookletId}`, {})
  }

  return {
    getRegularTransaction,
    saveMonthlyTransaction,
    getRegularTransactionById,
    updateRegularTransaction,
    deleteRegularTransaction,
    deleteRegularTransactions,
    linkRegularTransactionToBooklet,
    unlinkRegularTransactionFromBooklet,
  }
}
