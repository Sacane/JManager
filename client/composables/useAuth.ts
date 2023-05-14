import axios from 'axios'
import { API_PATH } from '../utils/request'
export interface UserAuth {
  username: string
  password: string
}
interface User {
  id: string
  username: string
  email: string
  token: string
}

export default function useAuth() {
  const user = useLocalStorage<User | null>('user', null)

  async function login(userAuth: UserAuth) {
    try {
      const response = await axios.post(`${API_PATH}user/auth`, userAuth)
      user.value = response.data
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
    user.value = null
    await axios.post(`${API_PATH}user/logout/${user.value?.id}`, {
      headers: defaultHeaders.value,
    })
  }

  const isAuthenticated = computed(() => user.value != null)
  return { user: readonly(user), isAuthenticated, login, logout, defaultHeaders }
}
