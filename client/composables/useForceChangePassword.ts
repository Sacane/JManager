import { LOADING_SCOPES } from '~/constants/loadingScopes'
import { passwordRuleProblem, policyViolationMessage } from '~/utils/passwordPolicy'

export default function useForceChangePassword() {
  const newPassword = ref('')
  const confirmPassword = ref('')
  /** Missing fields or a mismatch: shown under the confirmation. */
  const passwordError = ref<string | null>(null)
  /** The password rules: shown under the new password, where the checklist is. */
  const newPasswordError = ref<string | null>(null)

  const { post } = useQuery()
  const { withLoading, isScopeLoading } = useLoading()
  const toast = useJToast()
  const { clearMustChangePassword } = useConsent()
  const { policy: passwordPolicy } = usePasswordPolicy()

  const isSubmitting = computed(() => isScopeLoading(LOADING_SCOPES.password.forceChange))

  // Synchronous, so the error disappears with the keystroke that may fix it.
  watch(newPassword, () => {
    newPasswordError.value = null
  }, { flush: 'sync' })

  function validate(): boolean {
    if (!newPassword.value || !confirmPassword.value) {
      passwordError.value = 'Tous les champs sont requis'
      return false
    }
    if (newPassword.value !== confirmPassword.value) {
      passwordError.value = 'Les mots de passe ne correspondent pas'
      return false
    }
    passwordError.value = null

    // The address rule is left to the server, which knows the address.
    newPasswordError.value = passwordRuleProblem(newPassword.value, null, passwordPolicy.value)
    return !newPasswordError.value
  }

  async function submit(): Promise<void> {
    if (!validate()) return

    await withLoading(async () => {
      try {
        await post('user/password/force', {
          newPassword: newPassword.value,
          confirmPassword: confirmPassword.value,
        })
        clearMustChangePassword()
        navigateTo('/')
      } catch (error: unknown) {
        const payload = (error as { response?: { data?: unknown } } | null)?.response?.data
        const policyMessage = policyViolationMessage(payload, passwordPolicy.value)
        if (policyMessage) {
          newPasswordError.value = policyMessage
          return
        }
        toast.error('Une erreur est survenue. Veuillez réessayer.')
      }
    }, LOADING_SCOPES.password.forceChange)
  }

  return {
    newPassword,
    confirmPassword,
    passwordError: readonly(passwordError),
    newPasswordError: readonly(newPasswordError),
    isSubmitting,
    submit,
  }
}
