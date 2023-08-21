import { API_PATH } from './../utils/request';
import axios from 'axios'
import useAuth from './useAuth';
import { AccountDTO } from '../types/index';


export default function useAccounts(){
    const accounts: Ref<Array<AccountDTO>> = ref([])
    const {user, defaultHeaders} = useAuth()

    axios.get(`${API_PATH}user/accounts/get/${user.value?.id}`, {
        headers: defaultHeaders.value
    })
    .then(data => accounts.value = data.data)
    .catch(err => console.error(err))

    async function createAccount(labelAccount: string, amount: number) {
        try {
            const response = await axios.post(`${API_PATH}account/create`, {
                id: user.value?.id,
                labelAccount,
                amount
            })
        }catch(e: any) {
            console.error(e.toString())
        }
        
    }

    return {accounts: readonly(accounts), createAccount}
}