import useAuth from './useAuth';
import {AccountDTO} from '../types/index';
import useQuery from './useQuery';

export interface AccountFormatted{
    labelAccount: string,
    amount: string
  }

export default function useAccounts(){
    const accounts: Ref<Array<AccountDTO>> = ref([])
    const {user} = useAuth()
    const {get, post, deleteQuery} = useQuery()
    const accountFormatted = ref<AccountFormatted[]>([])

    async function fetch(): Promise<Array<AccountDTO>>  {
        return get(`account/${user.value?.id}`)

    }

    async function createAccount(labelAccount: string, amount: string): Promise<any> {
        console.log(user.value?.id)
        return post('account/create', {
            id: user.value?.id,
            labelAccount,
            amount
        })
    }

    async function updateAccount(account: AccountDTO, onUpdate: (acc: AccountDTO) => void, onFailure: (e: Error) => void = e => console.error(e)){
      post('account/update/' + user.value?.id, account)
        .then((acc) => {
            onUpdate(acc)
        }).catch(e => onFailure(e))
    }


    async function deleteAccount(id: number): Promise<any> {
        return deleteQuery(`account/${user.value?.id}/delete/${id}`, undefined)
    }

    async function findById(accountId: number) : Promise<AccountDTO> {
        return get(`account/user/${user.value?.id}/find/${accountId}`)
    }
    return {accounts: readonly(accounts), createAccount, fetch, deleteAccount, accountFormatted, updateAccount, findById}
}
