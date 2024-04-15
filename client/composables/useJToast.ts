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

  function info(message: string) {
    toast.add({
      severity: 'info',
      summary: 'Information',
      detail: message,
      life: 3000,
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
    toast.add({
      severity: 'error',
      summary: title,
      detail: axiosError.response?.data.message,
      life: 3000,
    })
  }
  return { success, info, warn, error, errorAxios }
}
