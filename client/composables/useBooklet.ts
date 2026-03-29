import useQuery from './useQuery'

export interface AccountFormatted {
  labelAccount: string
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
  const accountFormatted = ref<AccountFormatted[]>([])

  async function findAllBooklet(): Promise<Array<BookletDTO>> {
    return get(`account`).then()
  }

  async function createBooklet(labelAccount: string, amount: number, currency: string): Promise<any> {
    const booklet: BookletCreationRequest = {
      labelAccount,
      amount,
      currency,
    }
    return post('account', booklet)
  }

  async function deleteAccount(id: string): Promise<any> {
    return deleteQuery(`account/${id}`, undefined)
  }

  async function findById(accountId: string): Promise<BookletDTO> {
    return get(`account/${accountId}`)
  }

  async function findByIdMonthAndYear(
    accountId: string,
    month: number,
    year: number,
    dateRange: BookletDateRangeQuery = {},
  ): Promise<BookletReport> {
    return get(`account/report/${accountId}`, {
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
    return get(`account/${accountId}/balances`, {
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
    return get(`account/${accountId}/transactions`, {
      month,
      year,
      ...dateRange,
    })
  }

  return {
    createAccount: createBooklet,
    fetch: findAllBooklet,
    deleteAccount,
    accountFormatted,
    findById,
    findByIdMonthAndYear,
    findBalancesByIdMonthAndYear,
    findTransactionsByIdMonthAndYear,
  }
}
