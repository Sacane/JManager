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
  const storedUser: User = JSON.parse(localStorage.getItem('user') as string)
  const isAuthenticated = ref<boolean>(false)
  if (storedUser) {
    user.value = storedUser
    isAuthenticated.value = true
  }

  async function login(userAuth: UserAuth) {
    try {
      const response = await axios.post(`${API_PATH}user/auth`, userAuth)
      user.value = response.data
      isAuthenticated.value = true
      localStorage.setItem('user', JSON.stringify(user.value))
      navigateTo('/')
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
    axios.post(`${API_PATH}user/logout/${user?.value?.id}`, null, config).catch((e: any) => console.error(e.toString())).finally(() => {
      navigateTo('/')
      user.value = null
      localStorage.removeItem('user')
      isAuthenticated.value = false
    })
  }

  return { user: readonly(user), isAuthenticated, login, logout, defaultHeaders }
}
