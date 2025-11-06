import useQuery from './useQuery'

export interface AccountFormatted {
  labelAccount: string
  amount: string
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
      amount: amount.toFixed(2),
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

  async function findByIdMonthAndYear(accountId: string, month: number, year: number): Promise<BookletReport> {
    return get(`account/report/${accountId}`, {
      month,
      year,
    })
  }
  return { createAccount: createBooklet, fetch: findAllBooklet, deleteAccount, accountFormatted, findById, findByIdMonthAndYear }
}
