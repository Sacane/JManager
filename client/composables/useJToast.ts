import type { AxiosError } from 'axios'
import { extractErrorCode, getErrorMessageByCode } from '~/utils/errorCodeMap'

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
  function errorAxios(axiosError: AxiosError, title: string = 'Erreur') {
    const code = extractErrorCode(axiosError.response?.data)
    const mappedError = getErrorMessageByCode(code)
    toast.add({
      severity: 'error',
      summary: title === 'Erreur' ? mappedError.summary : title,
      detail: mappedError.detail,
      life: 3000,
    })
  }
  return { success, warn, error, errorAxios }
}
