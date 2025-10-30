package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.port.spi.CsvFileReader
import org.springframework.stereotype.Component

@Component
class InMemoryCsvFileReader : CsvFileReader {

    override fun readCsvContent(fileContent: String): List<Array<String>> {
        if (fileContent.isBlank()) {
            return emptyList()
        }

        return fileContent.lines()
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
