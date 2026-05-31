import type { AxiosError } from 'axios'
import { LOADING_SCOPES } from '~/constants/loadingScopes'

type VerifyResult = 'success' | 'expired' | 'not_found' | 'error'

const RESEND_COOLDOWN_SECONDS = 60

export default function useEmailVerification() {
  const { get, post } = useQuery()
  const { isScopeLoading, withLoading } = useLoading()

  const verifyResult = ref<VerifyResult | null>(null)
  const resendCooldown = ref(0)
  let cooldownInterval: ReturnType<typeof setInterval> | null = null

  const isVerifying = computed(() => isScopeLoading(LOADING_SCOPES.emailVerification.verify))
  const isResending = computed(() => isScopeLoading(LOADING_SCOPES.emailVerification.resend))
  const isResendOnCooldown = computed(() => resendCooldown.value > 0)

  async function verifyEmail(token: string): Promise<VerifyResult> {
    let result: VerifyResult = 'error'
    await withLoading(async () => {
      try {
        await get(`verify-email?token=${encodeURIComponent(token)}`)
        result = 'success'
      } catch (err) {
        const axiosError = err as AxiosError
        const status = axiosError.response?.status
        if (status === 410) {
          result = 'expired'
        } else if (status === 404) {
          result = 'not_found'
        } else {
          result = 'error'
        }
      }
    }, LOADING_SCOPES.emailVerification.verify)
    verifyResult.value = result
    return result
  }

  async function resendVerificationEmail(
    onSuccess?: () => void,
    onError?: (error: AxiosError) => void,
  ): Promise<void> {
    await withLoading(async () => {
      try {
        await post('verify-email/resend', undefined)
        startCooldown()
        onSuccess?.()
      } catch (err) {
        onError?.(err as AxiosError)
      }
    }, LOADING_SCOPES.emailVerification.resend)
  }

  function startCooldown(): void {
    resendCooldown.value = RESEND_COOLDOWN_SECONDS
    if (cooldownInterval) clearInterval(cooldownInterval)
    cooldownInterval = setInterval(() => {
      resendCooldown.value -= 1
      if (resendCooldown.value <= 0) {
        resendCooldown.value = 0
        if (cooldownInterval) {
          clearInterval(cooldownInterval)
          cooldownInterval = null
        }
      }
    }, 1000)
  }

  return {
    verifyResult: readonly(verifyResult),
    isVerifying,
    isResending,
    isResendOnCooldown,
    resendCooldown: readonly(resendCooldown),
    verifyEmail,
    resendVerificationEmail,
  }
}
