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
      amount,
      currency,
    }
    return post('account', booklet)
  }

  async function deleteAccount(id: number): Promise<any> {
    return deleteQuery(`account/${id}`, undefined)
  }

  async function findById(accountId: number): Promise<BookletDTO> {
    return get(`account/${accountId}`)
  }
  return { createAccount: createBooklet, fetch: findAllBooklet, deleteAccount, accountFormatted, findById }
}
