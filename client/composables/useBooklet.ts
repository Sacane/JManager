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

  async function findById(accountId: string): Promise<BookletDTO> {
    return get(`booklet/${accountId}`)
  }

  async function findByIdMonthAndYear(
    accountId: string,
    month: number,
    year: number,
    dateRange: BookletDateRangeQuery = {},
  ): Promise<BookletReport> {
    return get(`booklet/report/${accountId}`, {
      month,
      year,
      ...dateRange,
    })
  }

  async function findBalancesByIdMonthAndYear(
    accountId: string,
    month: number,
    year: number,
    dateRange: BookletDateRangeQuery = {},
  ): Promise<BookletBalancesDTO> {
    return get(`booklet/${accountId}/balances`, {
      month,
      year,
      ...dateRange,
    })
  }

  async function findTransactionsByIdMonthAndYear(
    accountId: string,
    month: number,
    year: number,
    dateRange: BookletDateRangeQuery = {},
  ): Promise<BookletTransactionsDTO> {
    return get(`booklet/${accountId}/transactions`, {
      month,
      year,
      ...dateRange,
    })
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
  }
}
