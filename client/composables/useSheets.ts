import useQuery from './useQuery';
import { SheetDTO } from '../types/index';

interface UserSheetDTO {
  userId: number,
  month: string,
  year: number,
  accountLabel: string
}

export default function useSheet() {
  const {deleteQuery, post} = useQuery()
  const {user} = useAuth()

  async function findByDate(month: string, year: number, accountLabel: string) {
    const response = post('sheets/get', {
      userId: user.value?.id,
      month: month,
      year: year,
      accountLabel: accountLabel
    })
    return response
  }
  
  function saveSheet(accountLabel: string, sheetDTO: SheetDTO): Promise<SheetDTO> {
    return post('/sheet/save', {
      userId: user.value?.id,
      accountLabel: accountLabel,
      sheetDTO: sheetDTO
    })
  }

  function deleteSheet(accountId: number, ids: Array<number>) : Promise<any>{
    return deleteQuery('/sheet/delete', {
      accountId: accountId,
      sheetIds: ids
    })
  }

  return {findByDate, saveSheet, deleteSheet}
}