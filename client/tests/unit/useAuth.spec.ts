import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import useAuth from '../../composables/useAuth'

vi.mock('axios')
vi.mock('jwt-decode', () => ({ jwtDecode: vi.fn() }))

describe('composables/useAuth', () => {
  describe('register', () => {
    beforeEach(() => {
      vi.clearAllMocks()
    })

    it('posts username, email, password and confirmPassword to the create endpoint', async () => {
      vi.mocked(axios.post).mockResolvedValue({ data: {} })
      const onSuccess = vi.fn()
      const { register } = useAuth()

      await register(
        { username: 'alice', email: 'alice@example.com', password: 'secret123', confirmPassword: 'secret123' },
        onSuccess,
      )

      expect(axios.post).toHaveBeenCalledWith(
        expect.stringContaining('user/create'),
        { username: 'alice', email: 'alice@example.com', password: 'secret123', confirmPassword: 'secret123' },
      )
      expect(onSuccess).toHaveBeenCalledOnce()
    })

    it('calls onError and does not call onSuccess when the request fails', async () => {
      const networkError = new Error('Network error')
      vi.mocked(axios.post).mockRejectedValue(networkError)
      const onSuccess = vi.fn()
      const onError = vi.fn()
      const { register } = useAuth()

      await register(
        { username: 'alice', email: 'alice@example.com', password: 'secret123', confirmPassword: 'secret123' },
        onSuccess,
        onError,
      )

      expect(onError).toHaveBeenCalledWith(networkError)
      expect(onSuccess).not.toHaveBeenCalled()
    })
  })
})
