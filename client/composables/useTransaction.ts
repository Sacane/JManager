import { format } from 'date-fns'

export interface TransactionDeletionDTO {
  deletedIds: string[]
  amount: string
}

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
  async function findTransactionById(id: string): Promise<TransactionResultDTO> {
    return get(`transaction/${id}`)
  }

  function saveTransaction(accountLabel: string, transactionCreationDTO: TransactionCreationDTO): Promise<TransactionResultDTO> {
    return post('transaction', {
      accountLabel,
      transactionResult: {
        ...transactionCreationDTO,
        date: format(transactionCreationDTO.date, 'yyyy-MM-dd'),
      },
    })
  }

  function deleteTransaction(accountId: string, ids: Array<string>): Promise<TransactionDeletionDTO> {
    return deleteQuery(`transaction`, {
      accountId,
      transactionIds: ids,
    })
  }

  function editTransaction(transactionCreationDTO: TransactionCreationDTO, accountId: string): Promise<TransactionResultDTO> {
    return patch('transaction', {
      accountId,
      transaction: {
        ...transactionCreationDTO,
        date: format(transactionCreationDTO.date, 'yyyy-MM-dd'),
      },
    })
  }

  function confirmPreviewTransaction(accountId: string, transactionId: string, amount: number | null, date: Date | null): Promise<TransactionResultDTO> {
    return patch(`transaction/confirm`, {
      transactionID: transactionId,
      accountID: accountId,
      newAmount: amount,
      newDate: date ? format(date, 'yyyy-MM-dd') : null,
    })
  }

  return { findByDate, saveTransaction, deleteTransaction, editTransaction, findTransactionById, confirmPreviewTransaction }
}
