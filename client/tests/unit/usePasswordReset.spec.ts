import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import usePasswordReset from '../../composables/usePasswordReset'

vi.mock('axios')

const API = 'http://localhost:8080/api/'

function refusal(status: number, errorKey?: string) {
  return { isAxiosError: true, response: { status, data: errorKey ? { errorKey } : {} } }
}

describe('usePasswordReset', () => {
  beforeEach(() => {
    vi.mocked(axios.post).mockReset()
    const states = new Map<string, ReturnType<typeof ref>>()
    vi.stubGlobal('useState', vi.fn((key: string, init?: () => unknown) => {
      if (!states.has(key)) states.set(key, ref(init ? init() : null))
      return states.get(key)
    }))
  })

  describe('requestReset', () => {
    it('sends the address and reports the request as sent', async () => {
      vi.mocked(axios.post).mockResolvedValue({ status: 202 })

      const outcome = await usePasswordReset().requestReset('johan@example.com')

      expect(axios.post).toHaveBeenCalledWith(`${API}password-reset/request`, { email: 'johan@example.com' })
      expect(outcome).toBe('sent')
    })

    it('reports too many requests apart from other failures', async () => {
      vi.mocked(axios.post).mockRejectedValueOnce(refusal(429)).mockRejectedValueOnce(new Error('network down'))
      const { requestReset } = usePasswordReset()

      expect(await requestReset('johan@example.com')).toBe('rate_limited')
      expect(await requestReset('johan@example.com')).toBe('error')
    })
  })

  describe('checkLink', () => {
    it('reports a usable link', async () => {
      vi.mocked(axios.post).mockResolvedValue({ status: 204 })

      expect(await usePasswordReset().checkLink('abc')).toBe('valid')
      expect(axios.post).toHaveBeenCalledWith(`${API}password-reset/validate`, { token: 'abc' })
    })

    it('tells an expired link from an invalid one', async () => {
      vi.mocked(axios.post)
        .mockRejectedValueOnce(refusal(400, 'domain.user.password_reset.token_expired'))
        .mockRejectedValueOnce(refusal(400, 'domain.user.password_reset.token_invalid'))
      const { checkLink } = usePasswordReset()

      expect(await checkLink('old')).toBe('expired')
      expect(await checkLink('bogus')).toBe('invalid')
    })

    // The link may still be good: the user should be able to try again rather than be told it is dead.
    it('reports the check as unavailable when the server cannot answer it now', async () => {
      vi.mocked(axios.post).mockRejectedValueOnce(refusal(429)).mockRejectedValueOnce(new Error('network down'))
      const { checkLink } = usePasswordReset()

      expect(await checkLink('abc')).toBe('unavailable')
      expect(await checkLink('abc')).toBe('unavailable')
    })
  })

  describe('confirmReset', () => {
    it('sends the token and both passwords', async () => {
      vi.mocked(axios.post).mockResolvedValue({ status: 204 })

      const outcome = await usePasswordReset().confirmReset('abc', 'brand-new-password', 'brand-new-password')

      expect(axios.post).toHaveBeenCalledWith(`${API}password-reset/confirm`, {
        token: 'abc',
        newPassword: 'brand-new-password',
        confirmPassword: 'brand-new-password',
      })
      expect(outcome).toEqual({ kind: 'reset' })
    })

    it('reports a link that died while the user was typing', async () => {
      vi.mocked(axios.post).mockRejectedValue(refusal(400, 'domain.user.password_reset.token_expired'))

      expect(await usePasswordReset().confirmReset('abc', 'a', 'a')).toEqual({ kind: 'expired' })
    })

    it('hands back the server refusal of a password for the page to place on its field', async () => {
      const payload = { errorKey: 'domain.user.password.policy_violation', reasons: ['equals_email'] }
      vi.mocked(axios.post).mockRejectedValue({ isAxiosError: true, response: { status: 400, data: payload } })

      expect(await usePasswordReset().confirmReset('abc', 'a', 'a')).toEqual({ kind: 'refused', payload })
    })

    it('reports too many attempts', async () => {
      vi.mocked(axios.post).mockRejectedValue(refusal(429))

      expect(await usePasswordReset().confirmReset('abc', 'a', 'a')).toEqual({ kind: 'rate_limited' })
    })
  })

  // Carried from the sign-in form without ever entering the URL.
  it('hands over the address typed on the sign-in form once', () => {
    usePasswordReset().rememberEmail('johan@example.com')

    const { takeRememberedEmail } = usePasswordReset()
    expect(takeRememberedEmail()).toBe('johan@example.com')
    expect(takeRememberedEmail()).toBe('')
  })
})
