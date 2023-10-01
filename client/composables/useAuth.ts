import { API_PATH } from './../utils/request';
import axios, { AxiosError } from 'axios'
import { useToast } from 'primevue/usetoast';
import useJToast from './useJToast';

export interface UserAuth {
  username: string
  password: string
}
interface User {
  id: string
  username: string
  email: string
  token: string
  refreshToken: string
}
interface UserRegister {
  username: string
  email: string
  password: string,
  confirmPassword: string
}

export default function useAuth() {
  const user: Ref<User | null> = ref(null)
  const storedUser: User | undefined = JSON.parse(localStorage.getItem('user') as string)
  const toast = useJToast()
  const isAuthenticated = ref<boolean>(false)
  if (storedUser) {
    user.value = storedUser
    isAuthenticated.value = true
  }
  else {
    user.value = null
    isAuthenticated.value = false
  }

  async function login(userAuth: UserAuth) {
    try {
      const response = await axios.post(`${API_PATH}user/auth`, userAuth)
      console.log(userAuth)
      user.value = response.data
      isAuthenticated.value = true
      navigateTo('/')
      localStorage.setItem('user', JSON.stringify(user.value))
    }
    catch (e: any) {
      console.error(e.toString())
    }
  }
  const defaultHeaders = computed(() => ({
    Authorization: `Bearer ${user.value?.token}`,
    Accept: 'application/json',
  }))
  async function logout() {
    const config = {
      headers: defaultHeaders.value,
    }
    try {
      await axios.post(`${API_PATH}user/logout/${user?.value?.id}`, null, config)
      user.value = null
      isAuthenticated.value = false
      navigateTo('/login')
      localStorage.removeItem('user')
    }
    catch (e: any) {
      handleError(e)
    }
  }

  async function tryRefresh() {
    const config = {
      headers: {
        Authorization: `Bearer ${user.value?.refreshToken}`,
        Accept: 'application/json'
      }
    }
    try {
      const response = await axios.post(`${API_PATH}user/auth/refresh/${user.value?.id}`, null, config)
      user.value = response.data
    }catch(e: any) {
      navigateTo('/login')
      console.error(e.toString())
    }
  }
  async function register(registeredUser: UserRegister) {
    const config = {
      headers: {
        Authorization: `Bearer ${user.value?.refreshToken}`,
        Accept: 'application/json'
      }
    }
    try {
      const response = await axios.post(`${API_PATH}user/create`, registeredUser, config)
    }catch(e: any) {
      navigateTo('/login')
      console.error(e.toString())
    }
  }

  
  function handleError(error: Error) {
    if(axios.isAxiosError(error)){
      const axiosError = error as AxiosError<any, any>
      const status = axiosError.response?.data.status
      if(status === 307) {
        tryRefresh()
        return
      } else if(status === 401) {
        navigateTo('/login')
        return
      }
      toast.error(axiosError.response?.data.message)
    }
    throw error
  }

  return { user: readonly(user), isAuthenticated, login, logout, defaultHeaders, tryRefresh, register}
}
