interface User {
  name: string
  email: string
  token: string
}
export default function useAuth() {
  const user = useLocalStorage<User | null>('user', null)
  const login = (user: User) => {
    useLocalStorage('user', user)
  }
  const logout = () => {
    useLocalStorage('user', null)
  }
  const isAuthenticated = computed(() => user.value !== null)
  return { user: readonly(user), isAuthenticated, login, logout }
}
