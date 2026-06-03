import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ref } from 'vue'
import useForceChangePassword from '../../composables/useForceChangePassword'

const postMock = vi.fn()
const withLoadingMock = vi.fn(async (action: () => Promise<unknown>) => action())
const isScopeLoadingMock = vi.fn(() => false)
const toastErrorMock = vi.fn()
const navigateToMock = vi.fn()
const clearMustChangePasswordMock = vi.fn()

vi.stubGlobal('navigateTo', navigateToMock)

vi.stubGlobal('useQuery', () => ({
  post: postMock,
  get: vi.fn(),
  put: vi.fn(),
  patch: vi.fn(),
  delete: vi.fn(),
}))

vi.stubGlobal('useLoading', () => ({
  withLoading: withLoadingMock,
  isScopeLoading: isScopeLoadingMock,
}))

vi.stubGlobal('useJToast', () => ({
  success: vi.fn(),
  warn: vi.fn(),
  error: toastErrorMock,
  errorAxios: vi.fn(),
}))

vi.stubGlobal('useConsent', () => ({
  mustChangePassword: ref(true),
  clearMustChangePassword: clearMustChangePasswordMock,
  checkConsentStatus: vi.fn(),
  tosAccepted: ref(false),
  privacyAccepted: ref(false),
  canSubmit: ref(false),
  isSubmitting: ref(false),
  tosVersion: '1.0',
  consentRequired: ref(false),
  consentChecked: ref(false),
  emailVerified: ref(true),
  userEmail: ref(null),
  submitConsent: vi.fn(),
  clearConsentCache: vi.fn(),
}))

describe('composables/useForceChangePassword', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    postMock.mockResolvedValue(undefined)
    withLoadingMock.mockImplementation(async (action: () => Promise<unknown>) => action())
  })

  it('does not call the API when passwords are empty', async () => {
    const { submit } = useForceChangePassword()
    await submit()
    expect(postMock).not.toHaveBeenCalled()
  })

  it('shows error when fields are empty', async () => {
    const { submit, passwordError } = useForceChangePassword()
    await submit()
    expect(passwordError.value).toBe('Tous les champs sont requis')
  })

  it('shows error when passwords do not match', async () => {
    const { newPassword, confirmPassword, submit, passwordError } = useForceChangePassword()
    newPassword.value = 'password1'
    confirmPassword.value = 'password2'
    await submit()
    expect(passwordError.value).toBe('Les mots de passe ne correspondent pas')
    expect(postMock).not.toHaveBeenCalled()
  })

  it('clears passwordError when validation passes', async () => {
    const { newPassword, confirmPassword, submit, passwordError } = useForceChangePassword()
    newPassword.value = 'mypassword'
    confirmPassword.value = 'different'
    await submit()
    expect(passwordError.value).toBe('Les mots de passe ne correspondent pas')

    confirmPassword.value = 'mypassword'
    await submit()
    expect(passwordError.value).toBeNull()
  })

  it('calls POST user/password/force with correct payload on valid submit', async () => {
    const { newPassword, confirmPassword, submit } = useForceChangePassword()
    newPassword.value = 'newpass123'
    confirmPassword.value = 'newpass123'
    await submit()
    expect(postMock).toHaveBeenCalledWith('user/password/force', {
      newPassword: 'newpass123',
      confirmPassword: 'newpass123',
    })
  })

  it('calls clearMustChangePassword and navigates to / on success', async () => {
    const { newPassword, confirmPassword, submit } = useForceChangePassword()
    newPassword.value = 'newpass123'
    confirmPassword.value = 'newpass123'
    await submit()
    expect(clearMustChangePasswordMock).toHaveBeenCalledTimes(1)
    expect(navigateToMock).toHaveBeenCalledWith('/')
  })

  it('shows error toast when API throws', async () => {
    postMock.mockRejectedValue(new Error('Server error'))
    const { newPassword, confirmPassword, submit } = useForceChangePassword()
    newPassword.value = 'newpass123'
    confirmPassword.value = 'newpass123'
    await submit()
    expect(toastErrorMock).toHaveBeenCalledWith('Une erreur est survenue. Veuillez réessayer.')
    expect(navigateToMock).not.toHaveBeenCalled()
  })
})
