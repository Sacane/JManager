import type { AxiosError } from 'axios'
import { LOADING_SCOPES } from '~/constants/loadingScopes'

interface UserStatusDTO {
  consentRequired: boolean
  emailVerified?: boolean
  email?: string | null
}

/** Current TOS version — update here when terms are revised. */
export const CURRENT_TOS_VERSION = '1.0'

export default function useConsent() {
  // useState persists across navigations in the same SPA session — HTTP call made only once.
  const consentChecked = useState<boolean>('consent:checked', () => false)
  const consentRequired = useState<boolean>('consent:required', () => false)
  // Default true to avoid banner flash before the first /me call completes.
  const emailVerified = useState<boolean>('consent:emailVerified', () => true)
  const userEmail = useState<string | null>('consent:userEmail', () => null)

  const tosAccepted = ref(false)
  const privacyAccepted = ref(false)
  const canSubmit = computed(() => tosAccepted.value && privacyAccepted.value)

  const { get, post } = useQuery()
  const { isScopeLoading, withLoading } = useLoading()
  const isSubmitting = computed(() => isScopeLoading(LOADING_SCOPES.consent.submit))

  /**
   * Checks whether the current user must go through the first-login consent gate.
   * Caches the result so only one HTTP call is made per session.
   */
  async function checkConsentStatus(): Promise<boolean> {
    if (consentChecked.value) return consentRequired.value
    try {
      const data = await get('user/me') as UserStatusDTO | undefined
      consentRequired.value = data?.consentRequired ?? false
      emailVerified.value = data?.emailVerified ?? true
      userEmail.value = data?.email ?? null
    } catch {
      // Fail open: if the check fails, don't block the user
      consentRequired.value = false
    }
    consentChecked.value = true
    return consentRequired.value
  }

  /**
   * Records the user's explicit GDPR consent.
   * On success: `consentRequired` is cleared and `onSuccess` is called.
   * On failure: `onError` is called with the axios error.
   */
  async function submitConsent(
    onSuccess?: () => void,
    onError?: (error: AxiosError) => void,
  ): Promise<void> {
    await withLoading(async () => {
      try {
        await post('user/consent', {
          tosAccepted: true,
          privacyAccepted: true,
          tosVersion: CURRENT_TOS_VERSION,
        })
        consentRequired.value = false
        consentChecked.value = true
        onSuccess?.()
      } catch (error) {
        onError?.(error as AxiosError)
      }
    }, LOADING_SCOPES.consent.submit)
  }

  /**
   * Resets the cached consent state.
   * Call this after logout so the next user gets a fresh check.
   */
  function clearConsentCache(): void {
    consentChecked.value = false
    consentRequired.value = false
    emailVerified.value = true
    userEmail.value = null
  }

  return {
    tosAccepted,
    privacyAccepted,
    canSubmit,
    isSubmitting,
    tosVersion: CURRENT_TOS_VERSION,
    consentRequired: readonly(consentRequired),
    consentChecked: readonly(consentChecked),
    emailVerified: readonly(emailVerified),
    userEmail: readonly(userEmail),
    checkConsentStatus,
    submitConsent,
    clearConsentCache,
  }
}
