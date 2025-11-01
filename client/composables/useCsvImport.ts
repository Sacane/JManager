import axios from 'axios'

export default function useCsvImport() {
  const config = useRuntimeConfig()
  const host = config.public.apiUrl

  /**
   * Valide un fichier CSV avant importation
   */
  async function validateCsvFile(bookletId: string, file: File): Promise<CsvValidationReportDTO> {
    const formData = new FormData()
    formData.append('file', file)

    try {
      const response = await axios.post(`${host}csv/validate/${bookletId}`, formData, {
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
   */
  async function importTransactionsFromCsv(
    bookletId: string,
    file: File,
    skipValidation: boolean = false,
  ): Promise<CsvImportResultDTO> {
    const formData = new FormData()
    formData.append('file', file)

    try {
      const response = await axios.post(
        `${host}csv/import/${bookletId}?skipValidation=${skipValidation}`,
        formData,
        {
          withCredentials: true,
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        },
      )
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
