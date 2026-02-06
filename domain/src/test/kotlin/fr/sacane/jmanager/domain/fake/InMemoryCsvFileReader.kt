package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.port.spi.CsvFileReader

class InMemoryCsvFileReader : CsvFileReader {

    companion object {
        private const val UTF8_BOM = '\uFEFF'
    }

    override fun readCsvContent(fileContent: String): List<Array<String>> {
        if (fileContent.isBlank()) {
            return emptyList()
        }

        // Remove UTF-8 BOM if present
        val cleanContent = if (fileContent.isNotEmpty() && fileContent[0] == UTF8_BOM) {
            fileContent.substring(1)
        } else {
            fileContent
        }

        return cleanContent.lines()
            .filter { it.isNotBlank() }
            .map { line ->
                parseCsvLine(line)
            }
    }

    private fun parseCsvLine(line: String): Array<String> {
        val result = mutableListOf<String>()
        val currentField = StringBuilder()
        var insideQuotes = false
        val separator = detectSeparator(line)

        for (i in line.indices) {
            val char = line[i]

            when {
                char == '"' -> {
                    insideQuotes = !insideQuotes
                }
                char == separator && !insideQuotes -> {
                    result.add(currentField.toString())
                    currentField.clear()
                }
                else -> {
                    currentField.append(char)
                }
            }
        }

        result.add(currentField.toString())

        return result.toTypedArray()
    }

    private fun detectSeparator(line: String): Char {
        var insideQuotes = false
        var commaCount = 0
        var semicolonCount = 0

        for (i in line.indices) {
            val char = line[i]
            when {
                char == '"' -> insideQuotes = !insideQuotes
                !insideQuotes && char == ',' -> commaCount++
                !insideQuotes && char == ';' -> semicolonCount++
            }
        }

        return if (semicolonCount > commaCount) ';' else ','
    }
}
