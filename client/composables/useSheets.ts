export default function useSheet() {
  const { deleteQuery, post, get } = useQuery()
  const { user } = useAuth()
  const dateUse = useDate()

  async function findByDate(month: string | undefined, year: number, accountLabel: string) {
    return get('sheet', {
      userId: user.value?.id,
      month: month ?? dateUse.monthFromNumber(new Date().getMonth() + 1),
      year,
      accountLabel,
    })
  }
  async function findTransactionById(id: number) {
    return get(`sheet/transaction/${id}`, {
      userID: user.value?.id,
    })
  }

  function saveSheet(accountLabel: string, sheetDTO: SheetDTO): Promise<SheetDTO> {
    return post('sheet', {
      userId: user.value?.id,
      accountLabel,
      sheetDTO,
    })
  }

  function deleteSheet(accountId: number, ids: Array<number>): Promise<any> {
    return deleteQuery(`sheet/delete/${user.value?.id}`, {
      accountId,
      sheetIds: ids,
    })
  }

  function editSheet(sheet: SheetDTO, accountId: number): Promise<SheetDTO> {
    return post('sheet/edit', {
      userId: user.value?.id,
      accountId,
      sheet,
    })
  }

  return { findByDate, saveSheet, deleteSheet, editSheet, findTransactionById }
}
