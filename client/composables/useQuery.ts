import axios from 'axios'
import { API_PATH } from '../utils/request';
import useAuth from './useAuth';

export default function useQuery() {
    const {defaultHeaders} = useAuth()

    async function get(url: string, authorized: boolean = true) {
        const response = await axios.get(`${API_PATH}` + url , {
            headers: defaultHeaders.value,
        })
        return response
    }
    async function deleteQuery(url: string, body: any | undefined){
      try{
        const response = await axios.delete(`${API_PATH}${url}`, {
          headers: defaultHeaders.value,
          data: body
        })
        return response.data
      }catch(error) {
        console.error(error)
        throw error
      }
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
 

    return {get, post, deleteQuery}
}