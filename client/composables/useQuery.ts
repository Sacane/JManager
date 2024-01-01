import axios, { AxiosError } from 'axios'
import { API_PATH } from '../utils/request';
import useAuth from './useAuth';

export default function useQuery() {
    const {defaultHeaders, tryRefresh, logout} = useAuth()
    const toast = useJToast()

    async function get(url: string, params: any | undefined = undefined) {
      try{
        const response = await axios.get(`${API_PATH}` + url , {
            headers: defaultHeaders.value,
            params: params
        })
        return response.data
      }catch(error: any) {
        handleError(error)
      }
    }
    async function deleteQuery(url: string, body: any | undefined){
      try{
        const response = await axios.delete(`${API_PATH}${url}`, {
          headers: defaultHeaders.value,
          data: body
        })
        return response.data
      }catch(error: any) {
        handleError(error)
      }
  }

    async function post(url: string, body: any | undefined) {
        try{
            const response = await axios.post(`${API_PATH}${url}`, body, {
                headers: defaultHeaders.value,
            })
            return response.data
        }catch(error: any) {
            handleError(error)
        }
    }

    function handleError(error: Error) {
      if(axios.isAxiosError(error)){
        const axiosError = error as AxiosError<any, any>
        const status = axiosError.response?.data.status
        const message = axiosError.response?.data.message
        if(status === 307) {
          tryRefresh()
          return
        } else if(status === 401) {
          toast.error(message)
          localStorage.removeItem('user')
          navigateTo('/login')
          logout()
          return
        }
        toast.error(message)
      }
      throw error
    }
 

    return {get, post, deleteQuery, handleError}
}