import useQuery from './useQuery';
import { SheetDTO } from '../types/index';

interface UserSheetDTO {
  userId: number,
  month: string,
  year: number,
  accountLabel: string
}

export default function useSheet() {
  const {deleteQuery, post, get} = useQuery()
  const {user} = useAuth()
  const dateUse = useDate()

  async function findByDate(month: string | undefined, year: number, accountLabel: string) {
    const response = get('sheet', {
      userId: user.value?.id,
      month: month ?? dateUse.monthFromNumber(new Date().getMonth() + 1) ?? "NOVEMBER",
      year: year,
      accountLabel: accountLabel
    })
    return response
  }
  
  function saveSheet(accountLabel: string, sheetDTO: SheetDTO): Promise<SheetDTO> {
    return post('sheet/save', {
      userId: user.value?.id,
      accountLabel: accountLabel,
      sheetDTO: sheetDTO
    })
  }

  function deleteSheet(accountId: number, ids: Array<number>) : Promise<any>{
    return deleteQuery('sheet/delete/' + user.value?.id, {
      accountId: accountId,
      sheetIds: ids
    })
  }

  function editSheet(sheet: SheetDTO, accountId: number): Promise<SheetDTO> {
    return post('sheet/edit', {
      userId: user.value?.id,
      accountId: accountId,
      sheet: sheet
    })
  }

  return {findByDate, saveSheet, deleteSheet, editSheet}
}