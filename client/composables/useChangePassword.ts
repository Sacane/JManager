import axios from 'axios'
import { LOADING_SCOPES } from '~/constants/loadingScopes'

export default function useChangePassword() {
  const config = useRuntimeConfig()
  const host = config.public.apiUrl

  const currentPassword = ref('')
  const newPassword = ref('')
  const confirmPassword = ref('')
  const confirmPasswordError = ref<string | null>(null)

  const { withLoading, isScopeLoading } = useLoading()
  const toast = useJToast()

  const isSubmitting = computed(() => isScopeLoading(LOADING_SCOPES.password.change))

  function validate(): boolean {
    if (!currentPassword.value || !newPassword.value || !confirmPassword.value) {
      confirmPasswordError.value = 'Tous les champs sont requis'
      return false
    }
    if (newPassword.value !== confirmPassword.value) {
      confirmPasswordError.value = 'Les mots de passe ne correspondent pas'
      return false
    }
    confirmPasswordError.value = null
    return true
  }

  function clearFields(): void {
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    confirmPasswordError.value = null
  }

  async function changePassword(): Promise<void> {
    if (!validate()) return

    await withLoading(async () => {
      try {
        await axios.patch(`${host}user/password`, {
          currentPassword: currentPassword.value,
          newPassword: newPassword.value,
          confirmPassword: confirmPassword.value,
        }, {
          withCredentials: true,
          headers: { 'Content-Type': 'application/json' },
        })
        clearFields()
        toast.success('Mot de passe modifié avec succès')
      } catch (error) {
        if (axios.isAxiosError(error)) {
          const status = error.response?.status
          if (status === 401) {
            toast.error('Mot de passe actuel incorrect')
            return
          }
          if (status === 422) {
            toast.error('Le nouveau mot de passe est invalide')
            return
          }
        }
        toast.error('Une erreur est survenue. Veuillez réessayer.')
      }
    }, LOADING_SCOPES.password.change)
  }

  return {
    currentPassword,
    newPassword,
    confirmPassword,
    confirmPasswordError: readonly(confirmPasswordError),
    isSubmitting,
    changePassword,
  }
}
