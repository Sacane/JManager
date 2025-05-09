import type { AxiosError } from 'axios'

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
    const errorData = axiosError.response?.data as { message?: string }
    toast.add({
      severity: 'error',
      summary: title,
      detail: errorData?.message || 'Une erreur inconnue est survenue',
      life: 3000,
    })
  }
  return { success, warn, error, errorAxios }
}
