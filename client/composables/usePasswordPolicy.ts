import type { PasswordPolicy } from '~/utils/passwordPolicy'
import axios from 'axios'

// Several password fields can mount at once (a form has two); they share one request.
let inFlight: Promise<void> | null = null

function isPasswordPolicy(value: unknown): value is PasswordPolicy {
  if (!value || typeof value !== 'object') return false
  const candidate = value as Partial<PasswordPolicy>
  return typeof candidate.minLength === 'number'
    && typeof candidate.maxLength === 'number'
    && Array.isArray(candidate.rules)
}

/**
 * The password policy published by the API, loaded once and shared by every screen that asks for a new
 * password. Stays `null` until loaded, or when it cannot be: the checklist is then not shown, and the
 * server, which enforces the policy anyway, remains the authority.
 */
export default function usePasswordPolicy() {
  const policy = useState<PasswordPolicy | null>('password:policy', () => null)

  async function load(): Promise<void> {
    if (policy.value) return
    if (!inFlight) {
      inFlight = (async () => {
        try {
          const { apiUrl } = useRuntimeConfig().public
          const response = await axios.get(`${apiUrl}password-policy`)
          if (isPasswordPolicy(response.data)) policy.value = response.data
        } catch {
          // Left empty: the next screen that needs it tries again.
        } finally {
          inFlight = null
        }
      })()
    }
    await inFlight
  }

  return { policy: readonly(policy), load }
}
