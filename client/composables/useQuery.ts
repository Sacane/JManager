import axios from 'axios'
import { API_PATH } from '../utils/request';
import useAuth from './useAuth';
import { AccountDTO } from '../types/index';

export default function useQuery() {
    const {defaultHeaders} = useAuth()

    async function get(url: string, authorized: boolean = true) {
        const response = await axios.get(`${API_PATH}` + url , {
            headers: defaultHeaders.value,
        })
        return response
    }

    async function post(url: string, body: any | undefined) {
        try{
            const response = await axios.post(`${API_PATH}${url}`, body, {
                headers: defaultHeaders.value,
            })
            return response.data
        }catch(error) {
            console.error(error)
            throw error
        }
    }

    return {get, post}
}