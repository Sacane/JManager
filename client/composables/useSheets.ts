export default function useSheet() {
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

  function saveSheet(accountLabel: string, sheetDTO: SheetDTO): Promise<TransactionResultDTO> {
    return post('transaction', {
      accountLabel,
      transactionResult: sheetDTO,
    })
  }

  function deleteSheet(accountId: number, ids: Array<number>): Promise<any> {
    return deleteQuery(`transaction`, {
      accountId,
      sheetIds: ids,
    })
  }

  function editSheet(sheet: SheetDTO, accountId: number): Promise<TransactionResultDTO> {
    return patch('transaction', {
      accountId,
      sheet,
    })
  }

  function confirmPreviewTransaction(accountId: string, transactionId: string): Promise<TransactionResultDTO> {
    return patch(`transaction/confirm`, {
      transactionID: transactionId,
      accountID: accountId,
    })
  }

  return { findByDate, saveSheet, deleteSheet, editSheet, findTransactionById, confirmPreviewTransaction }
}
