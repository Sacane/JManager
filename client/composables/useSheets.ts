export default function useSheet() {
  const { deleteQuery, post, get, patch } = useQuery()
  const { user } = useAuth()
  const dateUse = useDate()

  async function findByDate(month: string | undefined, year: number, accountLabel: string) {
    return get('transaction', {
      userId: user.value?.id,
      month: month ?? dateUse.monthFromNumber(new Date().getMonth() + 1),
      year,
      accountLabel,
    })
  }
  async function findTransactionById(id: number) {
    return get(`transaction/${id}`, {
      userID: user.value?.id,
    })
  }

  function saveSheet(accountLabel: string, sheetDTO: SheetDTO): Promise<TransactionResultDTO> {
    return post('transaction', {
      userId: user.value?.id,
      accountLabel,
      transactionResult: sheetDTO,
    })
  }

  function deleteSheet(accountId: number, ids: Array<number>): Promise<any> {
    return deleteQuery(`transaction/${user.value?.id}`, {
      accountId,
      sheetIds: ids,
    })
  }

  function editSheet(sheet: SheetDTO, accountId: number): Promise<TransactionResultDTO> {
    return patch('transaction', {
      userId: user.value?.id,
      accountId,
      sheet,
    })
  }

  function confirmPreviewTransaction(accountId, transactionId): Promise<TransactionResultDTO> {
    return patch(`transaction/confirm`, {
      userID: user.value?.id,
      transactionID: transactionId,
      accountID: accountId,
    })
  }

  return { findByDate, saveSheet, deleteSheet, editSheet, findTransactionById, confirmPreviewTransaction }
}
