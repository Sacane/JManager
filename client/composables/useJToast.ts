import type { AxiosError } from 'axios'
import { extractDiagnosticContext, getErrorMessageByCode } from '~/utils/errorCodeMap'

export default function useJToast() {
  const toast = useToast()
  function success(message: string, life: number = 3000) {
    toast.add({
      severity: 'success',
      summary: 'Succès',
      detail: message,
      life,
    })
  }

  function warn(message: string) {
    toast.add({
      severity: 'warn',
      summary: 'Warning',
      detail: message,
      life: 3000,
    })
  }

  function error(message: string) {
    toast.add({
      severity: 'error',
      summary: 'Une erreur est survenue',
      detail: message,
      life: 3000,
    })
  }
  function errorAxios(axiosError: AxiosError, title?: string) {
    const data = axiosError.response?.data
    const { requestId, userId, code } = extractDiagnosticContext(data)
    const mappedError = getErrorMessageByCode(code)
    toast.add({
      severity: 'error',
      summary: title ?? mappedError.summary,
      detail: mappedError.detail,
      life: requestId ? 8000 : 3000,
      requestId,
      userId,
      code,
    })
  }
  return { success, warn, error, errorAxios }
}
