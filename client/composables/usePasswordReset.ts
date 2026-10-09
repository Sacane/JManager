import axios from 'axios'
import { LOADING_SCOPES } from '~/constants/loadingScopes'

export type ResetRequestOutcome = 'sent' | 'rate_limited' | 'error'
export type ResetLinkStatus = 'valid' | 'expired' | 'invalid' | 'unavailable'
/** `refused`: a password the server refused (mismatch, policy); the payload carries its error key. */
export type ResetConfirmOutcome
  = | { kind: 'reset' }
    | { kind: 'expired' }
    | { kind: 'invalid' }
    | { kind: 'refused', payload: unknown }
    | { kind: 'rate_limited' }
    | { kind: 'error' }

const TOKEN_EXPIRED = 'domain.user.password_reset.token_expired'
const TOKEN_INVALID = 'domain.user.password_reset.token_invalid'

interface Refusal {
  status: number | null
  errorKey: string | null
  payload: unknown
}

function refusalOf(error: unknown): Refusal {
  const response = (error as { response?: { status?: unknown, data?: unknown } } | null)?.response
  const data = response?.data
  const errorKey = (data as { errorKey?: unknown } | null)?.errorKey
  return {
    status: typeof response?.status === 'number' ? response.status : null,
    errorKey: typeof errorKey === 'string' ? errorKey : null,
    payload: data,
  }
}

/**
 * The anonymous password reset flow: request a link, check it, set the new password.
 *
 * Calls axios directly rather than useQuery: these pages are used signed out, and useQuery reacts to
 * a 401 or 403 by refreshing the session and signing out.
 */
export default function usePasswordReset() {
  const { apiUrl } = useRuntimeConfig().public
  const { withLoading, isScopeLoading } = useLoading()
  const isRequesting = computed(() => isScopeLoading(LOADING_SCOPES.passwordReset.request))
  const isCheckingLink = computed(() => isScopeLoading(LOADING_SCOPES.passwordReset.check))
  const isConfirming = computed(() => isScopeLoading(LOADING_SCOPES.passwordReset.confirm))
  // Never in the URL: an address is personal data, and URLs end up in history and logs.
  const rememberedEmail = useState<string>('password-reset:email', () => '')

  function rememberEmail(email: string): void {
    rememberedEmail.value = email.trim()
  }

  /** The address typed on the sign-in form, handed over once. */
  function takeRememberedEmail(): string {
    const email = rememberedEmail.value
    rememberedEmail.value = ''
    return email
  }

  async function requestReset(email: string): Promise<ResetRequestOutcome> {
    let outcome: ResetRequestOutcome = 'error'
    await withLoading(async () => {
      try {
        await axios.post(`${apiUrl}password-reset/request`, { email })
        outcome = 'sent'
      } catch (error) {
        outcome = refusalOf(error).status === 429 ? 'rate_limited' : 'error'
      }
    }, LOADING_SCOPES.passwordReset.request)
    return outcome
  }

  async function checkLink(token: string): Promise<ResetLinkStatus> {
    let status: ResetLinkStatus = 'unavailable'
    await withLoading(async () => {
      try {
        await axios.post(`${apiUrl}password-reset/validate`, { token })
        status = 'valid'
      } catch (error) {
        const { errorKey } = refusalOf(error)
        if (errorKey === TOKEN_EXPIRED) status = 'expired'
        else if (errorKey === TOKEN_INVALID) status = 'invalid'
      }
    }, LOADING_SCOPES.passwordReset.check)
    return status
  }

  async function confirmReset(token: string, newPassword: string, confirmPassword: string): Promise<ResetConfirmOutcome> {
    let outcome: ResetConfirmOutcome = { kind: 'error' }
    await withLoading(async () => {
      try {
        await axios.post(`${apiUrl}password-reset/confirm`, { token, newPassword, confirmPassword })
        outcome = { kind: 'reset' }
      } catch (error) {
        const { status, errorKey, payload } = refusalOf(error)
        if (status === 429) outcome = { kind: 'rate_limited' }
        else if (errorKey === TOKEN_EXPIRED) outcome = { kind: 'expired' }
        else if (errorKey === TOKEN_INVALID) outcome = { kind: 'invalid' }
        else if (status === 400) outcome = { kind: 'refused', payload }
      }
    }, LOADING_SCOPES.passwordReset.confirm)
    return outcome
  }

  return {
    isRequesting,
    isCheckingLink,
    isConfirming,
    requestReset,
    checkLink,
    confirmReset,
    rememberEmail,
    takeRememberedEmail,
  }
}
