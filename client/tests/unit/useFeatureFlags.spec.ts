import type { FeatureKey } from '../../constants/featureKeys'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import useFeatureFlags from '../../composables/useFeatureFlags'

// ---------------------------------------------------------------------------
// Module-level mocks
// ---------------------------------------------------------------------------
const getMock = vi.fn()
const patchMock = vi.fn()
vi.stubGlobal('useQuery', () => ({ get: getMock, patch: patchMock }))

vi.stubGlobal('useLoading', () => ({
  isScopeLoading: vi.fn(() => false),
  withLoading: async <T>(action: () => Promise<T>) => action(),
}))

// ---------------------------------------------------------------------------

describe('composables/useFeatureFlags', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // Reset useState to empty array between tests
    vi.stubGlobal('useState', vi.fn((_key: string, init?: () => unknown) => ref(init ? init() : null)))
  })

  // -------------------------------------------------------------------------
  // fetchFlags
  // -------------------------------------------------------------------------
  describe('fetchFlags', () => {
    it('populates flags on success', async () => {
      getMock.mockResolvedValue([
        { key: 'EMAIL_VERIFICATION', enabled: false },
        { key: 'EMAIL_VERIFICATION_SIMPLE_USER_REGISTRATION', enabled: true },
      ])

      const { flags, fetchFlags } = useFeatureFlags()
      await fetchFlags()

      expect(flags.value).toHaveLength(2)
      expect(getMock).toHaveBeenCalledWith('feature-flags')
    })

    it('leaves flags empty and does not throw when API fails', async () => {
      getMock.mockRejectedValue(new Error('network error'))

      const { flags, fetchFlags } = useFeatureFlags()
      await expect(fetchFlags()).resolves.toBeUndefined()
      expect(flags.value).toHaveLength(0)
    })

    it('does not update flags when API returns undefined', async () => {
      getMock.mockResolvedValue(undefined)

      const { flags, fetchFlags } = useFeatureFlags()
      await fetchFlags()

      expect(flags.value).toHaveLength(0)
    })
  })

  // -------------------------------------------------------------------------
  // isEnabled
  // -------------------------------------------------------------------------
  describe('isEnabled', () => {
    it('returns true for an enabled flag', async () => {
      getMock.mockResolvedValue([{ key: 'EMAIL_VERIFICATION', enabled: true }])

      const { isEnabled, fetchFlags } = useFeatureFlags()
      await fetchFlags()

      expect(isEnabled('EMAIL_VERIFICATION' as FeatureKey)).toBe(true)
    })

    it('returns false for a disabled flag', async () => {
      getMock.mockResolvedValue([{ key: 'EMAIL_VERIFICATION', enabled: false }])

      const { isEnabled, fetchFlags } = useFeatureFlags()
      await fetchFlags()

      expect(isEnabled('EMAIL_VERIFICATION' as FeatureKey)).toBe(false)
    })

    it('returns false for an unknown key', async () => {
      getMock.mockResolvedValue([{ key: 'EMAIL_VERIFICATION', enabled: true }])

      const { isEnabled, fetchFlags } = useFeatureFlags()
      await fetchFlags()

      expect(isEnabled('UNKNOWN_KEY' as FeatureKey)).toBe(false)
    })

    it('returns false when flags have not been loaded yet', () => {
      const { isEnabled } = useFeatureFlags()
      expect(isEnabled('EMAIL_VERIFICATION' as FeatureKey)).toBe(false)
    })
  })

  // -------------------------------------------------------------------------
  // toggleFlag
  // -------------------------------------------------------------------------
  describe('toggleFlag', () => {
    it('calls PATCH with the correct key and enabled value', async () => {
      getMock.mockResolvedValue([{ key: 'EMAIL_VERIFICATION', enabled: false }])
      patchMock.mockResolvedValue({ key: 'EMAIL_VERIFICATION', enabled: true })

      const { fetchFlags, toggleFlag } = useFeatureFlags()
      await fetchFlags()
      await toggleFlag('EMAIL_VERIFICATION' as FeatureKey, true)

      expect(patchMock).toHaveBeenCalledWith(
        'admin/feature-flags/EMAIL_VERIFICATION',
        { enabled: true },
      )
    })

    it('updates the local flag state after a successful toggle', async () => {
      getMock.mockResolvedValue([{ key: 'EMAIL_VERIFICATION', enabled: false }])
      patchMock.mockResolvedValue({ key: 'EMAIL_VERIFICATION', enabled: true })

      const { flags, fetchFlags, toggleFlag } = useFeatureFlags()
      await fetchFlags()
      expect(flags.value.find(f => f.key === 'EMAIL_VERIFICATION')?.enabled).toBe(false)

      await toggleFlag('EMAIL_VERIFICATION' as FeatureKey, true)
      expect(flags.value.find(f => f.key === 'EMAIL_VERIFICATION')?.enabled).toBe(true)
    })

    it('returns the updated flag', async () => {
      getMock.mockResolvedValue([{ key: 'EMAIL_VERIFICATION', enabled: false }])
      patchMock.mockResolvedValue({ key: 'EMAIL_VERIFICATION', enabled: true })

      const { fetchFlags, toggleFlag } = useFeatureFlags()
      await fetchFlags()

      const result = await toggleFlag('EMAIL_VERIFICATION' as FeatureKey, true)
      expect(result).toEqual({ key: 'EMAIL_VERIFICATION', enabled: true })
    })

    it('throws when the PATCH call fails', async () => {
      getMock.mockResolvedValue([{ key: 'EMAIL_VERIFICATION', enabled: false }])
      patchMock.mockRejectedValue(new Error('forbidden'))

      const { fetchFlags, toggleFlag } = useFeatureFlags()
      await fetchFlags()

      await expect(toggleFlag('EMAIL_VERIFICATION' as FeatureKey, true)).rejects.toThrow()
    })
  })
})
