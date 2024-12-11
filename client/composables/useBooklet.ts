import useAuth from './useAuth'
import useQuery from './useQuery'

export interface AccountFormatted {
  labelAccount: string
  amount: string
}

export default function useBooklet() {
  const { user } = useAuth()
  const { get, post, deleteQuery } = useQuery()
  const accountFormatted = ref<AccountFormatted[]>([])

  async function findAllBooklet(): Promise<Array<AccountDTO>> {
    return get(`account/${user.value?.id}`)
  }

  async function createBooklet(labelAccount: string, amount: number, currency: string): Promise<any> {
    const booklet: BookletCreationRequest = {
      id: user.value?.id,
      labelAccount,
      amount,
      currency,
    }
    return post('account', booklet)
  }

  async function deleteAccount(id: number): Promise<any> {
    return deleteQuery(`account/${user.value?.id}/${id}`, undefined)
  }

  async function findById(accountId: number): Promise<AccountDTO> {
    return get(`account/${accountId}/user/${user.value?.id}`)
  }
  return { createAccount: createBooklet, fetch: findAllBooklet, deleteAccount, accountFormatted, findById }
}
