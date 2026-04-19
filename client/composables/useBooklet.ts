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
}

export type RegenerationType = 'PREVISIONAL' | 'VIRTUAL' | 'NONE'

export interface RegenerateTransactionsResponseDTO {
  transactions: TransactionResultDTO[]
  type: RegenerationType
}

export interface BookletDateRangeQuery {
  startDate?: string
  endDate?: string
}

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
  ): Promise<BookletTransactionsDTO> {
    return get(`booklet/${bookletId}/transactions`, {
      month,
      year,
      ...dateRange,
    })
  }

  async function regenerateDeletedPrevisionalTransactions(
    bookletId: string,
    month: number,
    year: number,
  ): Promise<RegenerateTransactionsResponseDTO> {
    return post(`booklet/${bookletId}/transactions/regenerate?month=${month}&year=${year}`, undefined)
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
    regenerateDeletedPrevisionalTransactions,
  }
}
