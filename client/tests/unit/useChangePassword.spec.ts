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

  it('shows error when fields are empty', async () => {
    const { changePassword, confirmPasswordError } = useChangePassword()
    await changePassword()
    expect(confirmPasswordError.value).toBe('Tous les champs sont requis')
  })

  it('shows error when new and confirm passwords do not match', async () => {
    const { currentPassword, newPassword, confirmPassword, changePassword, confirmPasswordError } = useChangePassword()
    currentPassword.value = 'old'
    newPassword.value = 'new1'
    confirmPassword.value = 'new2'
    await changePassword()
    expect(confirmPasswordError.value).toBe('Les mots de passe ne correspondent pas')
    expect(axios.patch).not.toHaveBeenCalled()
  })

  it('calls PATCH user/password with correct payload', async () => {
    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'oldpass'
    newPassword.value = 'newpass'
    confirmPassword.value = 'newpass'
    await changePassword()
    expect(axios.patch).toHaveBeenCalledWith(
      'http://localhost:8080/api/user/password',
      { currentPassword: 'oldpass', newPassword: 'newpass', confirmPassword: 'newpass' },
      expect.objectContaining({ withCredentials: true }),
    )
  })

  it('clears fields and shows success toast on success', async () => {
    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'oldpass'
    newPassword.value = 'newpass'
    confirmPassword.value = 'newpass'
    await changePassword()
    expect(toastSuccessMock).toHaveBeenCalledWith('Mot de passe modifié avec succès')
    expect(currentPassword.value).toBe('')
    expect(newPassword.value).toBe('')
    expect(confirmPassword.value).toBe('')
  })

  it('shows "incorrect current password" toast on 401', async () => {
    const axiosError = { response: { status: 401 }, isAxiosError: true }
    vi.mocked(axios.patch).mockRejectedValue(axiosError)
    vi.mocked(axios.isAxiosError).mockReturnValue(true)

    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'wrong'
    newPassword.value = 'newpass'
    confirmPassword.value = 'newpass'
    await changePassword()
    expect(toastErrorMock).toHaveBeenCalledWith('Mot de passe actuel incorrect')
  })

  it('shows validation error toast on 422', async () => {
    const axiosError = { response: { status: 422 }, isAxiosError: true }
    vi.mocked(axios.patch).mockRejectedValue(axiosError)
    vi.mocked(axios.isAxiosError).mockReturnValue(true)

    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'old'
    newPassword.value = 'newpass'
    confirmPassword.value = 'newpass'
    await changePassword()
    expect(toastErrorMock).toHaveBeenCalledWith('Le nouveau mot de passe est invalide')
  })

  it('shows generic error toast on unexpected error', async () => {
    vi.mocked(axios.patch).mockRejectedValue(new Error('Network error'))
    vi.mocked(axios.isAxiosError).mockReturnValue(false)

    const { currentPassword, newPassword, confirmPassword, changePassword } = useChangePassword()
    currentPassword.value = 'old'
    newPassword.value = 'newpass'
    confirmPassword.value = 'newpass'
    await changePassword()
    expect(toastErrorMock).toHaveBeenCalledWith('Une erreur est survenue. Veuillez réessayer.')
  })
})
