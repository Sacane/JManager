import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import useAuth from '../../composables/useAuth'
import useConsent from '../../composables/useConsent'

vi.mock('axios')
vi.mock('jwt-decode', () => ({ jwtDecode: vi.fn() }))

const getMock = vi.fn()

/**
 * The consent and forced-password-change gates are decided by one `/me` call whose answer is cached
 * for the whole SPA session. Signing in as someone else in the same tab must not inherit it.
 */
describe('consent gate after an account switch', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    // One store for every useState key, shared across composables as it is in the app.
    const states = new Map<string, ReturnType<typeof ref>>()
    vi.stubGlobal('useState', vi.fn((key: string, init?: () => unknown) => {
      if (!states.has(key)) states.set(key, ref(init ? init() : null))
      return states.get(key)
    }))
    vi.stubGlobal('useQuery', () => ({ get: getMock, post: vi.fn() }))
    // The real composable, in place of the global stub: the cache it keeps is what is under test.
    vi.stubGlobal('useConsent', useConsent)

    const { jwtDecode } = await import('jwt-decode')
    vi.mocked(jwtDecode).mockReturnValue({ sub: 'uuid-1', username: 'alice', ADMIN: false, USER: true } as ReturnType<typeof jwtDecode>)
    vi.mocked(axios.post).mockResolvedValue({ data: { token: 'fake.jwt.token' } })
  })

  it('asks again for the new account instead of reusing the previous answer', async () => {
    getMock.mockResolvedValueOnce({ consentRequired: false, mustChangePassword: false })
    await useConsent().checkConsentStatus()

    await useAuth().logout()
    await useAuth().login({ email: 'bob@example.com', password: 'secret-password-1' })

    getMock.mockResolvedValueOnce({ consentRequired: true, mustChangePassword: true })
    const { checkConsentStatus, mustChangePassword } = useConsent()
    expect(await checkConsentStatus()).toBe(true)
    expect(mustChangePassword.value).toBe(true)
    expect(getMock).toHaveBeenCalledTimes(2)
  })

  // Signing out alone must drop it: the next visitor is not necessarily the one who just left.
  it('drops the previous answer on sign-out', async () => {
    getMock.mockResolvedValueOnce({ consentRequired: false, mustChangePassword: true })
    await useConsent().checkConsentStatus()

    await useAuth().logout()

    const { consentChecked, mustChangePassword } = useConsent()
    expect(consentChecked.value).toBe(false)
    expect(mustChangePassword.value).toBe(false)
  })
})
