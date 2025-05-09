import type { AxiosError } from 'axios'
import axios from 'axios'
import type { Ref } from 'vue'
import { jwtDecode } from 'jwt-decode'

export interface UserAuth {
  username: string
  password: string
}
interface User {
  id: string
  username: string
  email: string
  role: string
}
interface UserRegister {
  username: string
  email: string
  password: string
  confirmPassword: string
}

export default function useAuth() {
  const user: Ref<User | null> = ref(null)
  const storedUser: User | undefined = JSON.parse(localStorage.getItem('user') as string)
  const isAuthenticated = ref<boolean>(false)
  const config = useRuntimeConfig()
  const host = config.public.websocketUrl
  if (storedUser) {
    user.value = storedUser
    isAuthenticated.value = true
  } else {
    user.value = null
    isAuthenticated.value = false
  }

  async function login(userAuth: UserAuth, onError: (e: AxiosError) => void = e => console.error(e)) {
    try {
      const response = await axios.post(`${host}user/auth`, userAuth, { withCredentials: true })
      // user.value = response.data
      const result = response.data.token
      const decoded = jwtDecode<{ sub: string, username: string, role: string }>(result)
      user.value = {
        id: decoded.sub,
        username: decoded.username,
        role: decoded.role,
        email: '',
      }
      localStorage.setItem('user', JSON.stringify(user.value))
      isAuthenticated.value = true
      navigateTo('/')
    } catch (e: any) {
      onError(e)
    }
  }
  async function logout() {
    const config = {
      withCredentials: true,
    }
    try {
      await axios.post(`${host}user/logout`, null, config)
      user.value = null
      isAuthenticated.value = false
      navigateTo('/login')
      localStorage.removeItem('user')
    } catch (e: any) {
      handleError(e)
    }
  }

  async function tryRefresh() {
    try {
      const response = await axios.post(`${host}user/auth/refresh/${user.value?.id}`, null)
      user.value = response.data
    } catch (e: any) {
      isAuthenticated.value = false
      navigateTo('/login')
      console.error(e.toString())
    }
  }
  async function register(registeredUser: UserRegister, onSuccess: () => void = () => console.log('success'), onError: (e: AxiosError) => void = e => console.error(e)) {
    try {
      await axios.post(`${host}user/create`, registeredUser)
      onSuccess()
    } catch (e: any) {
      onError(e)
    }
  }

  function handleError(error: Error) {
    if (axios.isAxiosError(error)) {
      const axiosError = error as AxiosError<any, any>
      const status = axiosError.response?.data.status
      if (status === 307) {
        tryRefresh().then()
        return
      } else if (status === 401) {
        isAuthenticated.value = false
        navigateTo('/login')
        return
      }
      // toast.error(axiosError.response?.data.message)
    }
    throw error
  }

  return { user: readonly(user), isAuthenticated, login, logout, register }
}
