import { useFetch } from '@vueuse/core'
import { API_PATH } from '../utils/request'
interface User {
  user: {
    username: string
    email: string
  }
  tokenPair: {
    token: string
    refresh: string
  }
}
export interface UserAuth {
  username: string
  password: string
}
interface UserStorage {
  username: string
  email: string
  token: string
  refresh: string
}
export default function useAuth() {
  const user = useLocalStorage<UserStorage | null>('user', null)

  const login = (user: UserAuth) => {
    const { response } = useFetch(`${API_PATH}/auth/login`).post(user).json()
    const userResponse = response.value as User
    console.log(userResponse)
    useLocalStorage('user', {
      username: userResponse.user.username,
      email: userResponse.user.email,
      token: userResponse.tokenPair.token,
      refresh: userResponse.tokenPair.refresh,
    })
  }
  const logout = () => {
    useLocalStorage('user', null)
  }
  const isAuthenticated = computed(() => user.value !== null)
  return { user: readonly(user), isAuthenticated, login, logout }
}
