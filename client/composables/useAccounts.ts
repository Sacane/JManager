import { API_PATH } from './../utils/request';
import axios from 'axios'
import useAuth from './useAuth';
import { AccountDTO } from '../types/index';
import useQuery from './useQuery';

export interface AccountFormatted{
    labelAccount: string,
    amount: string
  }

export default function useAccounts(){
    const accounts: Ref<Array<AccountDTO>> = ref([])
    const {user, defaultHeaders} = useAuth()
    const {get, post, deleteQuery} = useQuery()
    const accountFormatted = ref<AccountFormatted[]>([])

    async function fetch(): Promise<Array<AccountDTO>>  {
        try {
            const response = await axios.get(`${API_PATH}account/${user.value?.id}`,
            {
                headers: defaultHeaders.value
            })
            accounts.value = response.data
            return response.data
        }catch(error) {
            console.error(error)
            throw error
        }
    }
    
    async function createAccount(labelAccount: string, amount: number) {
        post('account/create', {
            id: user.value?.id,
            labelAccount,
            amount
        })
    }

    function format(accounts: Array<AccountDTO>) {
        accountFormatted.value = accounts.map(account => {
          return {
            id: account.id,
            labelAccount: account.labelAccount,
            amount: `${account.amount} €`,
          };
        });
    }

    async function updateAccount(account: AccountDTO, onUpdate: (acc: AccountDTO) => void){
      post('account/update/' + user.value?.id, account)
            .then((acc) => {
              onUpdate(acc)
            })
    }
    

    async function deleteAccount(id: number): Promise<any> {
        return deleteQuery(`account/${user.value?.id}/delete/${id}`, undefined)
    }

    async function findById(accountId: number) : Promise<AccountDTO> {
        try {
            return (await get(`account/user/${user.value?.id}/find/${accountId}`)).data
        }catch(error){
            console.error(error)
            throw error
        }
    }
    return {accounts, createAccount, fetch, deleteAccount, accountFormatted, format, updateAccount, findById}
}