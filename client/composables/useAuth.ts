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

  const login = async (user: UserAuth) => {
    const { data } = await useFetch(`${API_PATH}user/auth`).post(user).json<User>()
    if (data.value == null)
      return null
    const { token, refresh } = data.value.tokenPair
    const { username, email } = data.value.user
    return useLocalStorage('user', {
      username,
      email,
      token,
      refresh,
    })
  }
  const logout = () => {
    useLocalStorage('user', null)
  }
  const isAuthenticated = computed(() => user.value !== null)
  return { user: readonly(user), isAuthenticated, login, logout }
}
