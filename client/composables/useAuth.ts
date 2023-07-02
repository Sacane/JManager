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
  const user: Ref<User | null> = ref(null)
  const storedUser: User | undefined = JSON.parse(localStorage.getItem('user') as string)
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
      navigateTo('/')
      localStorage.removeItem('user')
    }
    catch (e: any) {
      console.error(e.toString())
    }
  }

  return { user: readonly(user), isAuthenticated, login, logout }
}
