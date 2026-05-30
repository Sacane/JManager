import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import axios from 'axios'
import useAppVersion from '../../composables/useAppVersion'

vi.mock('axios')

vi.stubGlobal('useRuntimeConfig', vi.fn(() => ({
  public: { apiUrl: 'http://localhost:8080/api/' },
})))

describe('composables/useAppVersion', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal('useState', vi.fn((_key: string, init?: () => unknown) => ref(init ? init() : null)))
  })

  describe('fetchVersion', () => {
    it('sets version from build.version on success', async () => {
      vi.mocked(axios.get).mockResolvedValue({ data: { build: { version: '1.14.0' } } })

      const { version, fetchVersion } = useAppVersion()
      await fetchVersion()

      expect(version.value).toBe('1.14.0')
    })

    it('calls the actuator/info endpoint with the correct URL', async () => {
      vi.mocked(axios.get).mockResolvedValue({ data: { build: { version: '1.0.0' } } })

      const { fetchVersion } = useAppVersion()
      await fetchVersion()

      expect(axios.get).toHaveBeenCalledWith('http://localhost:8080/actuator/info')
    })

    it('leaves version null when API fails', async () => {
      vi.mocked(axios.get).mockRejectedValue(new Error('network error'))

      const { version, fetchVersion } = useAppVersion()
      await fetchVersion()

      expect(version.value).toBeNull()
    })

    it('leaves version null when build.version is absent from the response', async () => {
      vi.mocked(axios.get).mockResolvedValue({ data: {} })

      const { version, fetchVersion } = useAppVersion()
      await fetchVersion()

      expect(version.value).toBeNull()
    })
  })
})
