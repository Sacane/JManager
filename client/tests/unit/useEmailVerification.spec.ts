import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import useEmailVerification from '../../composables/useEmailVerification'

const getMock = vi.fn()
const postMock = vi.fn()
vi.stubGlobal('useQuery', () => ({ get: getMock, post: postMock }))
vi.stubGlobal('useLoading', () => ({
  isScopeLoading: vi.fn(() => false),
  withLoading: async <T>(action: () => Promise<T>) => action(),
}))

describe('composables/useEmailVerification', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('useState', vi.fn((_key: string, init?: () => unknown) => ref(init ? init() : null)))
  })

  describe('verifyEmail', () => {
    it('returns "success" when the API responds 200', async () => {
      getMock.mockResolvedValue(undefined)

      const { verifyEmail, verifyResult } = useEmailVerification()
      const result = await verifyEmail('valid-token')

      expect(result).toBe('success')
      expect(verifyResult.value).toBe('success')
    })

    it('returns "expired" when the API responds 410', async () => {
      getMock.mockRejectedValue({ response: { status: 410 } })

      const { verifyEmail } = useEmailVerification()
      const result = await verifyEmail('expired-token')

      expect(result).toBe('expired')
    })

    it('returns "not_found" when the API responds 404', async () => {
      getMock.mockRejectedValue({ response: { status: 404 } })

      const { verifyEmail } = useEmailVerification()
      const result = await verifyEmail('unknown-token')

      expect(result).toBe('not_found')
    })

    it('returns "error" on unexpected API failure', async () => {
      getMock.mockRejectedValue({ response: { status: 500 } })

      const { verifyEmail } = useEmailVerification()
      const result = await verifyEmail('bad-token')

      expect(result).toBe('error')
    })

    it('calls GET verify-email with URL-encoded token', async () => {
      getMock.mockResolvedValue(undefined)

      const { verifyEmail } = useEmailVerification()
      await verifyEmail('tok en+test')

      expect(getMock).toHaveBeenCalledWith('verify-email?token=tok%20en%2Btest')
    })
  })

  describe('resendVerificationEmail', () => {
    it('calls POST verify-email/resend', async () => {
      postMock.mockResolvedValue(undefined)

      const { resendVerificationEmail } = useEmailVerification()
      await resendVerificationEmail()

      expect(postMock).toHaveBeenCalledWith('verify-email/resend', undefined)
    })

    it('calls onSuccess when POST succeeds', async () => {
      postMock.mockResolvedValue(undefined)
      const onSuccess = vi.fn()

      const { resendVerificationEmail } = useEmailVerification()
      await resendVerificationEmail(onSuccess)

      expect(onSuccess).toHaveBeenCalledTimes(1)
    })

    it('calls onError when POST fails', async () => {
      const networkError = new Error('network')
      postMock.mockRejectedValue(networkError)
      const onError = vi.fn()

      const { resendVerificationEmail } = useEmailVerification()
      await resendVerificationEmail(undefined, onError)

      expect(onError).toHaveBeenCalledTimes(1)
    })

    it('starts cooldown after successful resend', async () => {
      postMock.mockResolvedValue(undefined)
      vi.useFakeTimers()

      const { resendVerificationEmail, isResendOnCooldown, resendCooldown } = useEmailVerification()
      await resendVerificationEmail()

      expect(isResendOnCooldown.value).toBe(true)
      expect(resendCooldown.value).toBe(60)

      vi.advanceTimersByTime(60_000)
      expect(resendCooldown.value).toBe(0)
      expect(isResendOnCooldown.value).toBe(false)

      vi.useRealTimers()
    })
  })
})
