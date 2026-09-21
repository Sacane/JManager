import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import useChangePassword from '../../composables/useChangePassword'

vi.mock('axios')

const withLoadingMock = vi.fn(async (action: () => Promise<unknown>) => action())
const isScopeLoadingMock = vi.fn(() => false)
const toastSuccessMock = vi.fn()
const toastErrorMock = vi.fn()

vi.stubGlobal('useLoading', () => ({
  withLoading: withLoadingMock,
  isScopeLoading: isScopeLoadingMock,
}))

vi.stubGlobal('useJToast', () => ({
  success: toastSuccessMock,
  warn: vi.fn(),
  error: toastErrorMock,
  errorAxios: vi.fn(),
}))

vi.stubGlobal('useRuntimeConfig', () => ({ public: { apiUrl: 'http://localhost:8080/api/' } }))

describe('composables/useChangePassword', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    withLoadingMock.mockImplementation(async (action: () => Promise<unknown>) => action())
    vi.mocked(axios.patch).mockResolvedValue({ data: undefined, status: 204 })
    vi.mocked(axios.isAxiosError).mockReturnValue(false)
  })

  it('does not call the API when fields are empty', async () => {
    const { changePassword } = useChangePassword()
    await changePassword()
    expect(axios.patch).not.toHaveBeenCalled()
  })

  // UX-20 replaced the single `confirmPasswordError` with one message per field: a form that puts
  // "tous les champs sont requis" under the confirmation does not say which one is missing.
  it('reports every empty field on the field itself', async () => {
    const { changePassword, fieldErrors } = useChangePassword()
    await changePassword()
    expect(fieldErrors.currentPassword).toBe('Indiquez votre mot de passe actuel')
    expect(fieldErrors.newPassword).toBe('Indiquez un nouveau mot de passe')
    expect(fieldErrors.confirmPassword).toBe('Confirmez le nouveau mot de passe')
  })

  it('shows error when new and confirm passwords do not match', async () => {
    const { currentPassword, newPassword, confirmPassword, changePassword, fieldErrors } = useChangePassword()
    currentPassword.value = 'old'
    newPassword.value = 'new1'
    confirmPassword.value = 'new2'
    await changePassword()
    expect(fieldErrors.confirmPassword).toBe('Les mots de passe ne correspondent pas')
    expect(axios.patch).not.toHaveBeenCalled()
  })

  it('calls PATCH user/password with correct payload', async () => {
    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'oldpass'
    newPassword.value = 'new-password-123'
    confirmPassword.value = 'new-password-123'
    await changePassword()
    expect(axios.patch).toHaveBeenCalledWith(
      'http://localhost:8080/api/user/password',
      { currentPassword: 'oldpass', newPassword: 'new-password-123', confirmPassword: 'new-password-123' },
      expect.objectContaining({ withCredentials: true }),
    )
  })

  it('clears fields and shows success toast on success', async () => {
    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'oldpass'
    newPassword.value = 'new-password-123'
    confirmPassword.value = 'new-password-123'
    await changePassword()
    expect(toastSuccessMock).toHaveBeenCalledWith('Mot de passe modifié avec succès')
    expect(currentPassword.value).toBe('')
    expect(newPassword.value).toBe('')
    expect(confirmPassword.value).toBe('')
  })

  // These two used to pin a mapping that did not match the backend. A wrong current password is
  // USER_UNAUTHORIZED, mapped to 403 — never 401, which is the confirmation not matching. And no
  // domain state produces 422 at all, so that branch was unreachable. UX-20 replaced the status
  // based mapping with one keyed on errorKey; the per-field outcomes are covered in
  // useChangePassword-field-errors.spec.ts. What remains asserted here is that a status carrying
  // no known error key still reaches the user.
  it('falls back to the generic toast when a failure carries no known error key', async () => {
    const axiosError = { response: { status: 401, data: {} }, isAxiosError: true }
    vi.mocked(axios.patch).mockRejectedValue(axiosError)
    vi.mocked(axios.isAxiosError).mockReturnValue(true)

    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'wrong'
    newPassword.value = 'new-password-123'
    confirmPassword.value = 'new-password-123'
    await changePassword()
    expect(toastErrorMock).toHaveBeenCalledWith('Une erreur est survenue. Veuillez réessayer.')
  })

  it('falls back to the generic toast on an unmapped error key', async () => {
    const axiosError = { response: { status: 400, data: { errorKey: 'domain.user.something.else' } }, isAxiosError: true }
    vi.mocked(axios.patch).mockRejectedValue(axiosError)
    vi.mocked(axios.isAxiosError).mockReturnValue(true)

    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'old'
    newPassword.value = 'new-password-123'
    confirmPassword.value = 'new-password-123'
    await changePassword()
    expect(toastErrorMock).toHaveBeenCalledWith('Une erreur est survenue. Veuillez réessayer.')
  })

  it('shows generic error toast on unexpected error', async () => {
    vi.mocked(axios.patch).mockRejectedValue(new Error('Network error'))
    vi.mocked(axios.isAxiosError).mockReturnValue(false)

    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'old'
    newPassword.value = 'new-password-123'
    confirmPassword.value = 'new-password-123'
    await changePassword()
    expect(toastErrorMock).toHaveBeenCalledWith('Une erreur est survenue. Veuillez réessayer.')
  })
})
