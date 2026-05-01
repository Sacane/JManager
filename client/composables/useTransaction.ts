import { format } from 'date-fns'

export interface TransactionDeletionDTO {
  deletedIds: string[]
  amount: string
  excludedVirtualTransactions: string[]
}

export default function useTransaction() {
  const { deleteQuery, post, get, patch } = useQuery()
  const dateUse = useDate()

  async function findByDate(month: string | undefined, year: number, bookletLabel: string) {
    return get('transaction', {
      month: month ?? dateUse.monthFromNumber(new Date().getMonth() + 1),
      year,
      bookletLabel,
    })
  }
  async function findTransactionById(id: string): Promise<TransactionResultDTO> {
    return get(`transaction/${id}`)
  }

  function saveTransaction(bookletLabel: string, transactionCreationDTO: TransactionCreationDTO): Promise<TransactionResultDTO> {
    return post('transaction', {
      bookletLabel,
      transactionResult: {
        ...transactionCreationDTO,
        date: format(transactionCreationDTO.date, 'yyyy-MM-dd'),
      },
    })
  }

  function deleteTransaction(
    bookletId: string,
    ids: Array<string>,
    virtualTransactions?: VirtualTransactionDescriptor[],
  ): Promise<TransactionDeletionDTO> {
    return deleteQuery(`transaction`, {
      bookletId,
      transactionIds: ids,
      virtualTransactions: virtualTransactions ?? [],
    })
  }

  function editTransaction(transactionCreationDTO: TransactionCreationDTO, bookletId: string): Promise<TransactionResultDTO> {
    return patch('transaction', {
      bookletId,
      transaction: {
        ...transactionCreationDTO,
        date: format(transactionCreationDTO.date, 'yyyy-MM-dd'),
      },
    })
  }

  function confirmPreviewTransaction(bookletId: string, transactionId: string, amount: number | null, date: Date | null): Promise<TransactionResultDTO> {
    return patch(`transaction/confirm`, {
      transactionID: transactionId,
      bookletID: bookletId,
      newAmount: amount,
      newDate: date ? format(date, 'yyyy-MM-dd') : null,
    })
  }

  function confirmVirtualTransaction(
    bookletId: string,
    regularTransactionId: string,
    sourceMonth: number,
    sourceYear: number,
    label: string,
    amount: number,
    date: Date,
    isIncome: boolean,
    tagId?: string,
    tagIsDefault?: boolean,
  ): Promise<TransactionResultDTO> {
    return post('transaction/virtual/confirm', {
      bookletId,
      regularTransactionId,
      sourceMonth,
      sourceYear,
      label,
      amount,
      date: format(date, 'yyyy-MM-dd'),
      isIncome,
      tagId: tagId ?? null,
      tagIsDefault: tagIsDefault ?? false,
    })
  }

  return { findByDate, saveTransaction, deleteTransaction, editTransaction, findTransactionById, confirmPreviewTransaction, confirmVirtualTransaction }
}
