package fr.sacane.jmanager.domain.port.spi

/**
 * SPI Port: File reader for CSV import
 *
 * This interface must be implemented by the infrastructure layer
 * to provide file reading capabilities
 */
interface CsvFileReader {
    /**
     * Reads a CSV file and returns its content as a list of string arrays
     * Each array represents a row, with each element being a column value
     *
     * @param fileContent The raw content of the CSV file
     * @return List of rows, where each row is an array of column values
     */
    fun readCsvContent(fileContent: String): List<Array<String>>
}

