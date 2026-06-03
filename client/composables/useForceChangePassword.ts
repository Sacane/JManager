import { LOADING_SCOPES } from '~/constants/loadingScopes'

export default function useForceChangePassword() {
  const newPassword = ref('')
  const confirmPassword = ref('')
  const passwordError = ref<string | null>(null)

  const { post } = useQuery()
  const { withLoading, isScopeLoading } = useLoading()
  const toast = useJToast()
  const { clearMustChangePassword } = useConsent()

  const isSubmitting = computed(() => isScopeLoading(LOADING_SCOPES.password.forceChange))

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
    return true
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
      }
      catch {
        toast.error('Une erreur est survenue. Veuillez réessayer.')
      }
    }, LOADING_SCOPES.password.forceChange)
  }

  return {
    newPassword,
    confirmPassword,
    passwordError: readonly(passwordError),
    isSubmitting,
    submit,
  }
}
