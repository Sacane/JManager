import axios from 'axios'
import { LOADING_SCOPES } from '~/constants/loadingScopes'

export default function useChangePassword() {
  const config = useRuntimeConfig()
  const host = config.public.apiUrl

  const currentPassword = ref('')
  const newPassword = ref('')
  const confirmPassword = ref('')

  const fieldErrors = reactive<{
    currentPassword: string | null
    newPassword: string | null
    confirmPassword: string | null
  }>({ currentPassword: null, newPassword: null, confirmPassword: null })

  /**
   * Backend failures that name a field, keyed on the `errorKey` the API returns.
   *
   * Keyed on the error key rather than on the HTTP status because a status is shared: 400 covers
   * every invalid request and 401 every unauthorized one. Mapping on status is what made the
   * previous version announce the wrong cause — it treated 401 as a wrong current password, while
   * a wrong current password is USER_UNAUTHORIZED, mapped to 403, and 401 is the confirmation not
   * matching.
   */
  const FIELD_BY_ERROR_KEY: Record<string, { field: keyof typeof fieldErrors, message: string }> = {
    'domain.user.password.invalid_credentials': {
      field: 'currentPassword',
      message: 'Le mot de passe actuel est incorrect',
    },
    'domain.user.password.unchanged': {
      field: 'newPassword',
      message: 'Le nouveau mot de passe doit être différent de l\'ancien',
    },
    'domain.user.password.mismatch': {
      field: 'confirmPassword',
      message: 'Les mots de passe ne correspondent pas',
    },
  }

  function clearFieldErrors(): void {
    fieldErrors.currentPassword = null
    fieldErrors.newPassword = null
    fieldErrors.confirmPassword = null
  }

  const { withLoading, isScopeLoading } = useLoading()
  const toast = useJToast()

  const isSubmitting = computed(() => isScopeLoading(LOADING_SCOPES.password.change))

  function validate(): boolean {
    clearFieldErrors()

    if (!currentPassword.value) fieldErrors.currentPassword = 'Indiquez votre mot de passe actuel'
    if (!newPassword.value) fieldErrors.newPassword = 'Indiquez un nouveau mot de passe'
    if (!confirmPassword.value) fieldErrors.confirmPassword = 'Confirmez le nouveau mot de passe'

    if (fieldErrors.currentPassword || fieldErrors.newPassword || fieldErrors.confirmPassword) {
      return false
    }

    if (newPassword.value !== confirmPassword.value) {
      fieldErrors.confirmPassword = 'Les mots de passe ne correspondent pas'
      return false
    }

    return true
  }

  function clearFields(): void {
    currentPassword.value = ''
    newPassword.value = ''
    confirmPassword.value = ''
    clearFieldErrors()
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
          const errorKey = (error.response?.data as { errorKey?: string } | undefined)?.errorKey
          const known = errorKey ? FIELD_BY_ERROR_KEY[errorKey] : undefined
          if (known) {
            fieldErrors[known.field] = known.message
            return
          }
        }
        // Nothing the user can fix in a field — a missing account, a network failure.
        toast.error('Une erreur est survenue. Veuillez réessayer.')
      }
    }, LOADING_SCOPES.password.change)
  }

  return {
    currentPassword,
    newPassword,
    confirmPassword,
    fieldErrors,
    isSubmitting,
    changePassword,
  }
}
