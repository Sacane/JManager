import type { AxiosError } from 'axios'
import { computed, ref } from 'vue'
import { LOADING_SCOPES } from '~/constants/loadingScopes'

export interface UserDTO {
  id: string
  username: string
  subscriptionPlan: string
  email: string | null
  createdDate: string | null
  roles: string[]
}

export interface AdminCreateUserRequest {
  username: string
  password: string
  confirmPassword: string
  email: string
}

export default function useAdmin() {
  const { get, post } = useQuery()
  const loadingScope = LOADING_SCOPES.admin.fetchUsers
  const { isScopeLoading, withLoading } = useLoading()
  const users = ref<UserDTO[]>([])
  const totalUsers = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(10)
  const isLoading = computed(() => isScopeLoading(loadingScope))

  async function fetchUsers(
    page: number = 0,
    size: number = 10,
    onSuccess?: () => void,
    onError?: (error: AxiosError) => void,
  ) {
    await withLoading(async () => {
      try {
        const response: PageDTO<UserDTO> = await get('admin/users', {
          page,
          size,
        })

        if (response) {
          users.value = response.content
          totalUsers.value = response.totalElements
          totalPages.value = response.totalPages
          currentPage.value = response.pageNumber
          pageSize.value = response.pageSize
          onSuccess?.()
        }
      } catch (error) {
        onError?.(error as AxiosError)
      }
    }, loadingScope)
  }

  async function createUser(
    payload: AdminCreateUserRequest,
    onSuccess: () => void,
    onError: (error: AxiosError) => void,
  ) {
    await withLoading(async () => {
      try {
        await post('admin/users', payload)
        onSuccess()
      } catch (error) {
        onError(error as AxiosError)
      }
    }, LOADING_SCOPES.admin.createUser)
  }

  return {
    users,
    totalUsers,
    totalPages,
    currentPage,
    pageSize,
    isLoading,
    fetchUsers,
    createUser,
  }
}
