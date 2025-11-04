import axios from 'axios'

export default function useCsvImport() {
  const config = useRuntimeConfig()
  const host = config.public.apiUrl

  /**
   * Validates a CSV file before import
   *
   * @param bookletId - Target booklet ID
   * @param file - CSV file to validate
   * @param month - Optional month (1-12) for dates with day only
   * @param year - Optional year for dates with day only
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
      console.error('Error during validation:', error)
      throw error
    }
  }

  /**
   * Imports transactions from a CSV file
   *
   * @param bookletId - Target booklet ID
   * @param file - CSV file to import
   * @param skipValidation - If true, skips validation (assumes it has already been done)
   * @param month - Optional month (1-12) for dates with day only
   * @param year - Optional year for dates with day only
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
      console.error('Error during import:', error)
      throw error
    }
  }

  /**
   * Exports transactions to CSV format
   *
   * @param transactionIds - List of transaction IDs to export
   * @returns Blob containing the CSV file
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
      console.error('Error during export:', error)
      throw error
    }
  }

  /**
   * Downloads a CSV export file of transactions
   *
   * @param transactionIds - List of transaction IDs to export
   * @param filename - Filename (optional, auto-generated if not provided)
   */
  async function downloadCsvExport(transactionIds: string[], filename?: string): Promise<void> {
    try {
      const blob = await exportTransactionsToCsv(transactionIds)

      const downloadFilename = filename || `transactions_export_${new Date().toISOString().slice(0, 10)}.csv`

      const downloadUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = downloadFilename
      document.body.appendChild(link)
      link.click()

      document.body.removeChild(link)
      window.URL.revokeObjectURL(downloadUrl)
    } catch (error) {
      console.error('Error during download:', error)
      throw error
    }
  }

  return {
    validateCsvFile,
    importTransactionsFromCsv,
    downloadCsvExport,
  }
}
