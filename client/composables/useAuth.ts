import type { AxiosError } from 'axios'
import axios from 'axios'
import { jwtDecode } from 'jwt-decode'
import { extractErrorCode } from '~/utils/errorCodeMap'

export interface UserAuth {
  email: string
  password: string
}
interface User {
  id: string
  username: string
  email: string
  roles: string[]
}
export interface UserRegister {
  username: string
  email: string
  password: string
  confirmPassword: string
  tosAccepted: boolean
  tosVersion: string | null
  privacyAccepted: boolean
}

interface RefreshOptions {
  redirectOnFailure?: boolean
}

let refreshRequestInFlight: Promise<boolean> | null = null

export default function useAuth() {
  const user = useState<User | null>('auth:user', () => null)
  const isAuthenticated = useState<boolean>('auth:isAuthenticated', () => false)
  const isSessionInitialized = useState<boolean>('auth:sessionInitialized', () => false)
  const isRefreshInProgress = useState<boolean>('auth:isRefreshInProgress', () => false)
  const isAdmin = computed(() => user.value?.roles.includes('ADMIN') ?? false)
  const config = useRuntimeConfig()
  const host = config.public.apiUrl

  function userFromToken(token: string): User {
    const decoded = jwtDecode<{ sub: string, username: string, role: string, ADMIN: boolean, USER: boolean }>(token)
    const roles = []
    if (decoded.ADMIN) {
      roles.push('ADMIN')
    }
    if (decoded.USER) {
      roles.push('USER')
    }
    return {
      id: decoded.sub,
      username: decoded.username,
      roles,
      email: '',
    }
  }

  function applyAuthenticatedUser(accessToken: string) {
    user.value = userFromToken(accessToken)
    isAuthenticated.value = true
  }

  function clearAuthState() {
    user.value = null
    isAuthenticated.value = false
  }

  async function login(userAuth: UserAuth, onError: (e: AxiosError) => void = e => console.error(e)) {
    try {
      const response = await axios.post(`${host}user/auth`, userAuth, { withCredentials: true })
      applyAuthenticatedUser(response.data.token)
      isSessionInitialized.value = true
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
    } catch (e: any) {
      console.warn(e)
    } finally {
      clearAuthState()
      isSessionInitialized.value = true
      navigateTo('/login')
    }
  }

  async function tryRefresh(options: RefreshOptions = {}): Promise<boolean> {
    const { redirectOnFailure = true } = options

    if (refreshRequestInFlight) {
      return refreshRequestInFlight
    }

    refreshRequestInFlight = (async () => {
      isRefreshInProgress.value = true
      try {
        const response = await axios.post(`${host}user/auth/refresh`, null, {
          withCredentials: true,
        })
        applyAuthenticatedUser(response.data.token)
        return true
      } catch (e: any) {
        clearAuthState()
        if (redirectOnFailure) {
          navigateTo('/login')
        }
        return false
      } finally {
        isRefreshInProgress.value = false
        isSessionInitialized.value = true
        refreshRequestInFlight = null
      }
    })()

    return refreshRequestInFlight
  }

  async function initializeSession(): Promise<boolean> {
    if (isSessionInitialized.value) {
      return isAuthenticated.value
    }
    return tryRefresh({ redirectOnFailure: false })
  }

  // eslint-disable-next-line no-console
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
      const httpStatus = axiosError.response?.status
      const domainCode = extractErrorCode(axiosError.response?.data)
      const legacyStatus = axiosError.response?.data?.status

      if (domainCode === 1 || legacyStatus === 307) {
        tryRefresh().then()
        return
      } else if (httpStatus === 401 || httpStatus === 403) {
        clearAuthState()
        isSessionInitialized.value = true
        navigateTo('/login')
        return
      }
      // toast.error(axiosError.response?.data.message)
    }
    throw error
  }

  return { user: readonly(user), isAuthenticated: readonly(isAuthenticated), login, logout, register, isAdmin, tryRefresh, initializeSession }
}
