import axios from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import useChangePassword from '../../composables/useChangePassword'

vi.mock('axios')

const withLoadingMock = vi.fn(async (action: () => Promise<unknown>) => action())
const toastErrorMock = vi.fn()
const toastSuccessMock = vi.fn()

vi.stubGlobal('useLoading', () => ({
  withLoading: withLoadingMock,
  isScopeLoading: vi.fn(() => false),
}))

vi.stubGlobal('useJToast', () => ({
  success: toastSuccessMock,
  warn: vi.fn(),
  error: toastErrorMock,
  errorAxios: vi.fn(),
}))

vi.stubGlobal('useRuntimeConfig', () => ({ public: { apiUrl: 'http://localhost:8080/api/' } }))

/**
 * Backend failures of `PATCH user/password`, read off `ChangePasswordUseCase` and the state to
 * HTTP mapping in `ApiMappingExtensions` / `ProblemDetailHandler`.
 */
function rejectWith(status: number, errorKey: string, detail: string) {
  vi.mocked(axios.isAxiosError).mockReturnValue(true)
  vi.mocked(axios.patch).mockRejectedValue({
    isAxiosError: true,
    response: { status, data: { code: 0, errorKey, detail } },
  })
}

function fill(composable: ReturnType<typeof useChangePassword>) {
  composable.currentPassword.value = 'ancien'
  composable.newPassword.value = 'nouveau'
  composable.confirmPassword.value = 'nouveau'
}

describe('composables/useChangePassword field level failures', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    withLoadingMock.mockImplementation(async (action: () => Promise<unknown>) => action())
    vi.mocked(axios.patch).mockResolvedValue({ data: undefined, status: 204 })
    vi.mocked(axios.isAxiosError).mockReturnValue(false)
  })

  // USER_UNAUTHORIZED -> ForbiddenException -> 403. The client mapped 401 to this message, but 401
  // is PASSWORD_NOT_MATCH, so a wrong current password was reported as an unexpected error while a
  // mismatched confirmation was reported as a wrong current password.
  it('puts a wrong current password on the current password field', async () => {
    const composable = useChangePassword()
    fill(composable)
    rejectWith(403, 'domain.user.password.invalid_credentials', 'Le mot de passe actuel est incorrect')

    await composable.changePassword()

    expect(composable.fieldErrors.currentPassword).toMatch(/actuel/i)
    expect(composable.fieldErrors.newPassword).toBeNull()
    expect(toastErrorMock).not.toHaveBeenCalled()
  })

  // PASSWORD_UNCHANGED -> InvalidRequestException -> 400. It used to fall through to
  // "Une erreur est survenue. Veuillez réessayer."
  it('puts an unchanged password on the new password field', async () => {
    const composable = useChangePassword()
    fill(composable)
    rejectWith(400, 'domain.user.password.unchanged', 'Le nouveau mot de passe doit être différent de l\'ancien')

    await composable.changePassword()

    expect(composable.fieldErrors.newPassword).toMatch(/différent/i)
    expect(composable.fieldErrors.currentPassword).toBeNull()
    expect(toastErrorMock).not.toHaveBeenCalled()
  })

  // PASSWORD_NOT_MATCH -> UnauthorizedRequestException -> 401.
  it('puts a mismatched confirmation on the confirmation field', async () => {
    const composable = useChangePassword()
    fill(composable)
    rejectWith(401, 'domain.user.password.mismatch', 'Les mots de passe ne correspondent pas')

    await composable.changePassword()

    expect(composable.fieldErrors.confirmPassword).toMatch(/correspondent pas/i)
    expect(composable.fieldErrors.currentPassword).toBeNull()
  })

  // Nothing the user can fix in a field: the toast is the right channel.
  it('keeps reporting a missing user by toast', async () => {
    const composable = useChangePassword()
    fill(composable)
    rejectWith(404, 'domain.user.password.user_not_found', 'L\'utilisateur est introuvable')

    await composable.changePassword()

    expect(toastErrorMock).toHaveBeenCalled()
    expect(composable.fieldErrors.currentPassword).toBeNull()
    expect(composable.fieldErrors.newPassword).toBeNull()
    expect(composable.fieldErrors.confirmPassword).toBeNull()
  })

  it('keeps reporting an unknown failure by toast', async () => {
    const composable = useChangePassword()
    fill(composable)
    vi.mocked(axios.isAxiosError).mockReturnValue(false)
    vi.mocked(axios.patch).mockRejectedValue(new Error('network down'))

    await composable.changePassword()

    expect(toastErrorMock).toHaveBeenCalled()
  })

  it('clears every field error on a successful change', async () => {
    const composable = useChangePassword()
    fill(composable)
    rejectWith(403, 'domain.user.password.invalid_credentials', 'x')
    await composable.changePassword()
    expect(composable.fieldErrors.currentPassword).not.toBeNull()

    vi.mocked(axios.isAxiosError).mockReturnValue(false)
    vi.mocked(axios.patch).mockResolvedValue({ data: undefined, status: 204 })
    fill(composable)
    await composable.changePassword()

    expect(composable.fieldErrors.currentPassword).toBeNull()
    expect(toastSuccessMock).toHaveBeenCalled()
  })

  it('reports each missing field on the field itself', async () => {
    const composable = useChangePassword()
    composable.currentPassword.value = ''
    composable.newPassword.value = ''
    composable.confirmPassword.value = ''

    await composable.changePassword()

    expect(composable.fieldErrors.currentPassword).not.toBeNull()
    expect(composable.fieldErrors.newPassword).not.toBeNull()
    expect(composable.fieldErrors.confirmPassword).not.toBeNull()
    expect(axios.patch).not.toHaveBeenCalled()
  })

  it('reports a local mismatch on the confirmation field without calling the API', async () => {
    const composable = useChangePassword()
    composable.currentPassword.value = 'ancien'
    composable.newPassword.value = 'nouveau'
    composable.confirmPassword.value = 'autre'

    await composable.changePassword()

    expect(composable.fieldErrors.confirmPassword).toMatch(/correspondent pas/i)
    expect(composable.fieldErrors.newPassword).toBeNull()
    expect(axios.patch).not.toHaveBeenCalled()
  })
})
