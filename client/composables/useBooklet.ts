import useQuery from './useQuery'

export interface BookletFormatted {
  label: string
  amount: string
}

export interface BookletBalancesDTO {
  label: string
  realSold: string
  previewSold: string
}

export interface BookletTransactionsDTO {
  transactions: TransactionResultDTO[]
  hasRegenerableTransactions: boolean
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
}

export type RegenerationType = 'PREVISIONAL' | 'VIRTUAL' | 'NONE'

export interface RegenerateTransactionsResponseDTO {
  transactions: TransactionResultDTO[]
  type: RegenerationType
}

/**
 * A deleted occurrence the user could restore.
 *
 * Several entries can share the same `regularTransactionId` when the recurrence produces more than
 * one occurrence in the month (weekly, daily). The backend tracks exclusion per regular transaction
 * and month, never per occurrence, so restoring any one of them restores the whole group.
 */
export interface RegenerableTransactionDTO {
  regularTransactionId: string
  label: string
  value: string
  currency: string
  isIncome: boolean
  date: string
  tagDTO: TagDTO | null
}

export interface BookletDateRangeQuery {
  startDate?: string
  endDate?: string
}

/**
 * Order applied by the backend to every transaction of the period, before pagination.
 * Omitting it keeps the legacy order: confirmed transactions first, then previsional ones.
 */
export type TransactionSortDirection = 'ASCENDING' | 'DESCENDING'

/**
 * Field the backend orders the whole period by, before pagination. `EXPENSE` / `INCOME` order
 * by amount within their own kind and push the other kind to the end of the list.
 * Only meaningful together with a {@link TransactionSortDirection}; defaults to `DATE` server-side.
 */
export type TransactionSortField = 'DATE' | 'LABEL' | 'EXPENSE' | 'INCOME'

export default function useBooklet() {
  const { get, post, deleteQuery } = useQuery()
  const BookletFormatted = ref<BookletFormatted[]>([])

  async function findAllBooklet(): Promise<Array<BookletDTO>> {
    return get(`booklet`).then()
  }

  async function createBooklet(label: string, amount: number, currency: string): Promise<any> {
    const booklet: BookletCreationRequest = {
      label,
      amount,
      currency,
    }
    return post('booklet', booklet)
  }

  async function deleteBooklet(id: string): Promise<any> {
    return deleteQuery(`booklet/${id}`, undefined)
  }

  async function findById(bookletId: string): Promise<BookletDTO> {
    return get(`booklet/${bookletId}`)
  }

  async function findByIdMonthAndYear(
    bookletId: string,
    month: number,
    year: number,
    dateRange: BookletDateRangeQuery = {},
  ): Promise<BookletReport> {
    return get(`booklet/report/${bookletId}`, {
      month,
      year,
      ...dateRange,
    })
  }

  async function findBalancesByIdMonthAndYear(
    bookletId: string,
    month: number,
    year: number,
    dateRange: BookletDateRangeQuery = {},
  ): Promise<BookletBalancesDTO> {
    return get(`booklet/${bookletId}/balances`, {
      month,
      year,
      ...dateRange,
    })
  }

  async function findTransactionsByIdMonthAndYear(
    bookletId: string,
    month: number,
    year: number,
    dateRange: BookletDateRangeQuery = {},
    page: number = 0,
    size: number = 10,
    sortDirection?: TransactionSortDirection,
    sortField?: TransactionSortField,
  ): Promise<BookletTransactionsDTO> {
    return get(`booklet/${bookletId}/transactions`, {
      month,
      year,
      ...dateRange,
      page,
      size,
      ...(sortDirection ? { sortDirection } : {}),
      ...(sortField ? { sortField } : {}),
    })
  }

  async function findRegenerableTransactions(
    bookletId: string,
    month: number,
    year: number,
  ): Promise<RegenerableTransactionDTO[]> {
    return get(`booklet/${bookletId}/transactions/regenerable`, { month, year })
  }

  async function regenerateDeletedPrevisionalTransactions(
    bookletId: string,
    month: number,
    year: number,
    regularTransactionIds: string[],
  ): Promise<RegenerateTransactionsResponseDTO> {
    return post(
      `booklet/${bookletId}/transactions/regenerate?month=${month}&year=${year}`,
      { regularTransactionIds },
    )
  }

  return {
    createBooklet,
    fetch: findAllBooklet,
    deleteBooklet,
    BookletFormatted,
    findById,
    findByIdMonthAndYear,
    findBalancesByIdMonthAndYear,
    findTransactionsByIdMonthAndYear,
    findRegenerableTransactions,
    regenerateDeletedPrevisionalTransactions,
  }
}
