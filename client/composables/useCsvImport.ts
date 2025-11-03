import axios from 'axios'

export default function useCsvImport() {
  const config = useRuntimeConfig()
  const host = config.public.apiUrl

  /**
   * Valide un fichier CSV avant importation
   *
   * @param bookletId - ID du livret cible
   * @param file - Fichier CSV à valider
   * @param month - Mois optionnel (1-12) pour les dates avec jour seul
   * @param year - Année optionnelle pour les dates avec jour seul
   */
  async function validateCsvFile(
    bookletId: string,
    file: File,
    month?: number,
    year?: number
  ): Promise<CsvValidationReportDTO> {
    const formData = new FormData()
    formData.append('file', file)

    const params = new URLSearchParams()
    if (month !== undefined) {
      params.append('month', month.toString())
    }
    if (year !== undefined) {
      params.append('year', year.toString())
    }

    const queryString = params.toString()
    const url = `${host}csv/validate/${bookletId}${queryString ? `?${queryString}` : ''}`

    try {
      const response = await axios.post(url, formData, {
        withCredentials: true,
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      return response.data
    } catch (error) {
      console.error('Erreur lors de la validation:', error)
      throw error
    }
  }

  /**
   * Importe les transactions d'un fichier CSV
   *
   * @param bookletId - ID du livret cible
   * @param file - Fichier CSV à importer
   * @param skipValidation - Si true, saute la validation (assume qu'elle a déjà été faite)
   * @param month - Mois optionnel (1-12) pour les dates avec jour seul
   * @param year - Année optionnelle pour les dates avec jour seul
   */
  async function importTransactionsFromCsv(
    bookletId: string,
    file: File,
    skipValidation: boolean = false,
    month?: number,
    year?: number,
  ): Promise<CsvImportResultDTO> {
    const formData = new FormData()
    formData.append('file', file)

    const params = new URLSearchParams()
    params.append('skipValidation', skipValidation.toString())
    if (month !== undefined) {
      params.append('month', month.toString())
    }
    if (year !== undefined) {
      params.append('year', year.toString())
    }

    const queryString = params.toString()
    const url = `${host}csv/import/${bookletId}?${queryString}`

    try {
      const response = await axios.post(url, formData, {
        withCredentials: true,
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      })
      return response.data
    } catch (error) {
      console.error('Erreur lors de l\'importation:', error)
      throw error
    }
  }

  return {
    validateCsvFile,
    importTransactionsFromCsv,
  }
}
