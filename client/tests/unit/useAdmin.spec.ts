import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import useAdmin from '../../composables/useAdmin'

describe('composables/useAdmin', () => {
  const get = vi.fn()
  const post = vi.fn()

  beforeEach(() => {
    get.mockReset()
    post.mockReset()

    vi.stubGlobal('useQuery', () => ({ get, post }))

    vi.stubGlobal('useLoading', () => {
      const activeScope = ref<string | null>(null)

      return {
        isScopeLoading: (scope: string) => activeScope.value === scope,
        withLoading: async <T>(action: () => Promise<T>, scope: string) => {
          activeScope.value = scope
          try {
            return await action()
          } finally {
            activeScope.value = null
          }
        },
      }
    })
  })

  it('fetchUsers populates pagination data on success', async () => {
    get.mockResolvedValue({
      content: [
        {
          id: 'u-1',
          username: 'john',
          email: 'john@test.fr',
          createdDate: '2026-03-17T00:00:00Z',
          roles: ['USER'],
        },
      ],
      pageNumber: 1,
      pageSize: 20,
      totalElements: 42,
      totalPages: 3,
    })

    const { users, currentPage, pageSize, totalPages, totalUsers, fetchUsers } = useAdmin()
    const onSuccess = vi.fn()

    await fetchUsers(1, 20, onSuccess)

    expect(onSuccess).toHaveBeenCalledTimes(1)
    expect(users.value).toHaveLength(1)
    expect(currentPage.value).toBe(1)
    expect(pageSize.value).toBe(20)
    expect(totalUsers.value).toBe(42)
    expect(totalPages.value).toBe(3)
    expect(get).toHaveBeenCalledWith('admin/users', { page: 1, size: 20 })
  })

  it('fetchUsers calls onError and leaves loading false after failure', async () => {
    const error = new Error('boom')
    get.mockRejectedValue(error)

    const { fetchUsers, isLoading } = useAdmin()
    const onError = vi.fn()

    expect(isLoading.value).toBe(false)
    await fetchUsers(0, 10, undefined, onError)

    expect(onError).toHaveBeenCalledTimes(1)
    expect(isLoading.value).toBe(false)
  })

  it('createUser calls POST admin/users and invokes onSuccess on success', async () => {
    post.mockResolvedValue({ id: 'u-1', username: 'beta', subscriptionPlan: 'BETA_TESTER' })

    const { createUser } = useAdmin()
    const onSuccess = vi.fn()
    const onError = vi.fn()

    await createUser({ username: 'beta', password: 'pass', confirmPassword: 'pass', email: 'beta@example.com' }, onSuccess, onError)

    expect(post).toHaveBeenCalledWith('admin/users', {
      username: 'beta',
      password: 'pass',
      confirmPassword: 'pass',
      email: 'beta@example.com',
    })
    expect(onSuccess).toHaveBeenCalledTimes(1)
    expect(onError).not.toHaveBeenCalled()
  })

  it('createUser calls onError when the request fails', async () => {
    const error = new Error('network error')
    post.mockRejectedValue(error)

    const { createUser } = useAdmin()
    const onSuccess = vi.fn()
    const onError = vi.fn()

    await createUser({ username: 'beta', password: 'pass', confirmPassword: 'pass', email: 'beta@example.com' }, onSuccess, onError)

    expect(onError).toHaveBeenCalledTimes(1)
    expect(onSuccess).not.toHaveBeenCalled()
  })
})
