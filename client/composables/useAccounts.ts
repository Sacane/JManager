import { API_PATH } from './../utils/request';
import axios from 'axios'
import useAuth from './useAuth';
import { AccountDTO } from '../types/index';
import useQuery from './useQuery';


export default function useAccounts(){
    const accounts: Ref<Array<AccountDTO>> = ref([])
    const {user, defaultHeaders} = useAuth()
    const {get, post} = useQuery()
    get(`user/accounts/get/${user.value?.id}`)
    .then(data => accounts.value = data.data)
    .catch(err => console.error(err))

    async function createAccount(labelAccount: string, amount: number) {
        try {
            const response = await post(`${API_PATH}account/create`, {
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