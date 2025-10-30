import type { AxiosError } from 'axios'
import { ref } from 'vue'

export interface UserDTO {
  id: string
  username: string
  email: string | null
  createdDate: string | null
  roles: string[]
}

export interface PageDTO<T> {
  content: T[]
  pageNumber: number
  pageSize: number
  totalElements: number
  totalPages: number
}

export default function useAdmin() {
  const { get } = useQuery()
  const users = ref<UserDTO[]>([])
  const totalUsers = ref(0)
  const totalPages = ref(0)
  const currentPage = ref(0)
  const pageSize = ref(10)
  const isLoading = ref(false)

  async function fetchUsers(
    page: number = 0,
    size: number = 10,
    onSuccess?: () => void,
    onError?: (error: AxiosError) => void,
  ) {
    isLoading.value = true
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
    } finally {
      isLoading.value = false
    }
  }

  return {
    users,
    totalUsers,
    totalPages,
    currentPage,
    pageSize,
    isLoading,
    fetchUsers,
  }
}
