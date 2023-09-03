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
    
    try{
        const response = axios.get(`user/accounts/get/${user.value?.id}`,
        {
            headers: defaultHeaders.value
        })
        
    }catch(error) {
        console.error(error)
    }

    async function fetch(): Promise<Array<AccountDTO>>  {
        try {
            const response = await axios.get(`${API_PATH}user/accounts/get/${user.value?.id}`,
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
    

    async function deleteAccount(id: number): Promise<any> {
        return deleteQuery(`user/${user.value?.id}/account/delete/${id}`, undefined)
    }
    return {accounts, createAccount, fetch, deleteAccount, accountFormatted, format}
}