import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import useAuth from '../../composables/useAuth'

vi.mock('axios')
vi.mock('jwt-decode', () => ({ jwtDecode: vi.fn() }))

describe('composables/useAuth deleteAccount', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('navigateTo', vi.fn())
  })

  it('calls the account deletion endpoint with the session cookie', async () => {
    vi.mocked(axios.delete).mockResolvedValue({ status: 204 })

    const { deleteAccount } = useAuth()
    await deleteAccount()

    expect(axios.delete).toHaveBeenCalledWith(
      expect.stringContaining('user/me'),
      expect.objectContaining({ withCredentials: true }),
    )
  })

  it('reports success and signs the user out', async () => {
    vi.mocked(axios.delete).mockResolvedValue({ status: 204 })

    const auth = useAuth()
    const succeeded = await auth.deleteAccount()

    expect(succeeded).toBe(true)
    expect(auth.isAuthenticated.value).toBe(false)
    expect(auth.user.value).toBeNull()
  })

  it('redirects to the login page once the account is gone', async () => {
    const navigateTo = vi.fn()
    vi.stubGlobal('navigateTo', navigateTo)
    vi.mocked(axios.delete).mockResolvedValue({ status: 204 })

    await useAuth().deleteAccount()

    expect(navigateTo).toHaveBeenCalledWith('/login')
  })

  // A failed deletion must not look like a success: the session stays, and the caller decides
  // what to tell the user.
  it('keeps the session when the deletion fails', async () => {
    const navigateTo = vi.fn()
    vi.stubGlobal('navigateTo', navigateTo)
    vi.mocked(axios.delete).mockRejectedValue(new Error('boom'))

    const auth = useAuth()
    const succeeded = await auth.deleteAccount()

    expect(succeeded).toBe(false)
    expect(navigateTo).not.toHaveBeenCalled()
  })
})
