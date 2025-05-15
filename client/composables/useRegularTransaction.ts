export default function useRegularTransaction() {
  const { post, get } = useQuery()

  async function saveRegularTransaction(regularTransaction: RegularTransactionCreationRequest): Promise<RegularTransactionDTO> {
    return post('transaction/regularTransaction', regularTransaction)
  }

  async function getRegularTransaction(): Promise<RegularTransactionDTO[]> {
    return get('transaction/regularTransaction')
  }

  return { saveRegularTransaction, getRegularTransaction }
}
