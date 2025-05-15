export type Frequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY'

export default function useRegularTransaction() {
  const { post, get } = useQuery()

  async function saveRegularTransaction(regularTransaction: RegularTransactionCreationRequest): Promise<RegularTransactionDTO> {
    return post('transaction/regular', regularTransaction)
  }

  async function getRegularTransaction(): Promise<RegularTransactionDTO[]> {
    return get('transaction/regular')
  }

  return { saveRegularTransaction, getRegularTransaction }
}
