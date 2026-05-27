import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import useConsent from '../../composables/useConsent'

// ---------------------------------------------------------------------------
// Module mocks
// ---------------------------------------------------------------------------
const getMock = vi.fn()
const postMock = vi.fn()
vi.stubGlobal('useQuery', () => ({ get: getMock, post: postMock }))

vi.stubGlobal('useLoading', () => ({
  isScopeLoading: vi.fn(() => false),
  withLoading: async <T>(action: () => Promise<T>) => action(),
}))

// ---------------------------------------------------------------------------

describe('composables/useConsent', () => {
  beforeEach(() => {
    vi.clearAllMocks()

    // Reset Nuxt useState to initial values between tests
    vi.stubGlobal('useState', vi.fn((_key: string, init?: () => unknown) => ref(init ? init() : null)))
  })

  // -------------------------------------------------------------------------
  // checkConsentStatus
  // -------------------------------------------------------------------------
  describe('checkConsentStatus', () => {
    it('returns true when the API reports consentRequired = true', async () => {
      getMock.mockResolvedValue({ consentRequired: true })

      const { checkConsentStatus } = useConsent()
      const result = await checkConsentStatus()

      expect(result).toBe(true)
      expect(getMock).toHaveBeenCalledWith('user/me')
    })

    it('returns false when the API reports consentRequired = false', async () => {
      getMock.mockResolvedValue({ consentRequired: false })

      const { checkConsentStatus } = useConsent()
      const result = await checkConsentStatus()

      expect(result).toBe(false)
    })

    it('returns false and does not throw when the API call fails', async () => {
      getMock.mockRejectedValue(new Error('network error'))

      const { checkConsentStatus } = useConsent()
      const result = await checkConsentStatus()

      expect(result).toBe(false)
    })

    it('calls the API only once and returns cached result on subsequent calls', async () => {
      getMock.mockResolvedValue({ consentRequired: true })

      const { checkConsentStatus } = useConsent()
      await checkConsentStatus()
      const second = await checkConsentStatus()

      expect(getMock).toHaveBeenCalledTimes(1)
      expect(second).toBe(true)
    })
  })

  // -------------------------------------------------------------------------
  // submitConsent
  // -------------------------------------------------------------------------
  describe('submitConsent', () => {
    it('calls POST /user/consent with correct body', async () => {
      postMock.mockResolvedValue(undefined)

      const { submitConsent } = useConsent()
      await submitConsent()

      expect(postMock).toHaveBeenCalledWith('user/consent', {
        tosAccepted: true,
        privacyAccepted: true,
        tosVersion: '1.0',
      })
    })

    it('calls onSuccess callback after a successful POST', async () => {
      postMock.mockResolvedValue(undefined)
      const onSuccess = vi.fn()

      const { submitConsent } = useConsent()
      await submitConsent(onSuccess)

      expect(onSuccess).toHaveBeenCalledTimes(1)
    })

    it('sets consentRequired to false after a successful POST', async () => {
      postMock.mockResolvedValue(undefined)

      const { submitConsent, consentRequired } = useConsent()
      await submitConsent()

      expect(consentRequired.value).toBe(false)
    })

    it('calls onError and does not call onSuccess when the POST fails', async () => {
      const networkError = new Error('server error')
      postMock.mockRejectedValue(networkError)
      const onSuccess = vi.fn()
      const onError = vi.fn()

      const { submitConsent } = useConsent()
      await submitConsent(onSuccess, onError)

      expect(onError).toHaveBeenCalledTimes(1)
      expect(onSuccess).not.toHaveBeenCalled()
    })
  })

  // -------------------------------------------------------------------------
  // clearConsentCache
  // -------------------------------------------------------------------------
  describe('clearConsentCache', () => {
    it('resets consentChecked and consentRequired', async () => {
      getMock.mockResolvedValue({ consentRequired: true })

      const { checkConsentStatus, clearConsentCache, consentRequired, consentChecked } = useConsent()
      await checkConsentStatus() // primes the cache

      clearConsentCache()

      expect(consentChecked.value).toBe(false)
      expect(consentRequired.value).toBe(false)
    })
  })

  // -------------------------------------------------------------------------
  // canSubmit
  // -------------------------------------------------------------------------
  describe('canSubmit', () => {
    it('is false when neither checkbox is checked', () => {
      const { canSubmit } = useConsent()
      expect(canSubmit.value).toBe(false)
    })

    it('is false when only tosAccepted is true', () => {
      const { tosAccepted, canSubmit } = useConsent()
      tosAccepted.value = true
      expect(canSubmit.value).toBe(false)
    })

    it('is false when only privacyAccepted is true', () => {
      const { privacyAccepted, canSubmit } = useConsent()
      privacyAccepted.value = true
      expect(canSubmit.value).toBe(false)
    })

    it('is true when both checkboxes are checked', () => {
      const { tosAccepted, privacyAccepted, canSubmit } = useConsent()
      tosAccepted.value = true
      privacyAccepted.value = true
      expect(canSubmit.value).toBe(true)
    })
  })
})
