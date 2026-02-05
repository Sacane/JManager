package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.port.spi.CsvFileReader
import org.springframework.stereotype.Component

@Component
class NativeCsvFileReader : CsvFileReader {

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

        for (i in line.indices) {
            val char = line[i]

            when {
                char == '"' -> {
                    insideQuotes = !insideQuotes
                }
                char == ',' && !insideQuotes -> {
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
}
