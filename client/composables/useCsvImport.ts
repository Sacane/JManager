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
    year?: number,
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

  /**
   * Exporte les transactions au format CSV
   *
   * @param transactionIds - Liste des IDs de transactions à exporter
   * @returns Blob contenant le fichier CSV
   */
  async function exportTransactionsToCsv(transactionIds: string[]): Promise<Blob> {
    const url = `${host}csv/export`

    try {
      const response = await axios.post(
        url,
        { transactionIds },
        {
          withCredentials: true,
          responseType: 'blob',
          headers: {
            'Content-Type': 'application/json',
          },
        },
      )
      return response.data
    } catch (error) {
      console.error('Erreur lors de l\'export:', error)
      throw error
    }
  }

  /**
   * Télécharge un fichier CSV d'export de transactions
   *
   * @param transactionIds - Liste des IDs de transactions à exporter
   * @param filename - Nom du fichier (optionnel, généré automatiquement si non fourni)
   */
  async function downloadCsvExport(transactionIds: string[], filename?: string): Promise<void> {
    try {
      const blob = await exportTransactionsToCsv(transactionIds)

      // Générer un nom de fichier avec la date si non fourni
      const downloadFilename = filename || `transactions_export_${new Date().toISOString().slice(0, 10)}.csv`

      // Créer un lien de téléchargement temporaire
      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = downloadFilename
      document.body.appendChild(link)
      link.click()

      // Nettoyer
      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)
    } catch (error) {
      console.error('Erreur lors du téléchargement:', error)
      throw error
    }
  }

  return {
    validateCsvFile,
    importTransactionsFromCsv,
    downloadCsvExport,
  }
}
