export default function useTransaction() {
  const { deleteQuery, post, get, patch } = useQuery()
  const dateUse = useDate()

  async function findByDate(month: string | undefined, year: number, accountLabel: string) {
    return get('transaction', {
      month: month ?? dateUse.monthFromNumber(new Date().getMonth() + 1),
      year,
      accountLabel,
    })
  }
  async function findTransactionById(id: number) {
    return get(`transaction/${id}`)
  }

  function saveTransaction(accountLabel: string, transactionCreationDTO: TransactionCreationDTO): Promise<TransactionResultDTO> {
    return post('transaction', {
      accountLabel,
      transactionResult: transactionCreationDTO,
    })
  }

  function deleteTransaction(accountId: number, ids: Array<number>): Promise<any> {
    return deleteQuery(`transaction`, {
      accountId,
      transactionIds: ids,
    })
  }

  function editTransaction(transactionCreationDTO: TransactionCreationDTO, accountId: number): Promise<TransactionResultDTO> {
    return patch('transaction', {
      accountId,
      transaction: transactionCreationDTO,
    })
  }

  function confirmPreviewTransaction(accountId: string, transactionId: string): Promise<TransactionResultDTO> {
    return patch(`transaction/confirm`, {
      transactionID: transactionId,
      accountID: accountId,
    })
  }

  return { findByDate, saveTransaction, deleteTransaction, editTransaction, findTransactionById, confirmPreviewTransaction }
}
