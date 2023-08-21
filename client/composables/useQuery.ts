import axios from 'axios'
import { API_PATH } from '../utils/request';
import useAuth from './useAuth';

export default function useQuery() {
    const {defaultHeaders} = useAuth()

    async function get(url: string, authorized: boolean = true) {
        return await axios.get(`${API_PATH}` + url , {
            headers: defaultHeaders.value
        })
    }

    async function post(url: string) {
        return await axios.post(`${API_PATH}` + url , {
            headers: defaultHeaders.value
        })
    }

    return {get, post}
}