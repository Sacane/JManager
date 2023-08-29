import { API_PATH } from './../utils/request';
import axios from 'axios'
import useAuth from './useAuth';
import { AccountDTO } from '../types/index';
import useQuery from './useQuery';


export default function useAccounts(){
    const accounts: Ref<Array<AccountDTO>> = ref([])
    const {user, defaultHeaders} = useAuth()
    const {get, post, deleteQuery} = useQuery()
    
    try{
        const response = axios.get(`user/accounts/get/${user.value?.id}`,
        {
            headers: defaultHeaders.value
        })
        
    }catch(error) {
        console.error(error)
    }

    async function fetch() {
        try {
            const response = await axios.get(`${API_PATH}user/accounts/get/${user.value?.id}`,
            {
                headers: defaultHeaders.value
            })
            accounts.value = response.data
        }catch(error) {
            console.error(error)
        }
    }
    
    async function createAccount(labelAccount: string, amount: number) {
        post('account/create', {
            id: user.value?.id,
            labelAccount,
            amount
        })
    }

    async function deleteAccount(id: number) {
        deleteQuery(`user/${user.value?.id}/account/delete/${id}`, undefined)
    }

    return {accounts, createAccount, fetch, deleteAccount}
}