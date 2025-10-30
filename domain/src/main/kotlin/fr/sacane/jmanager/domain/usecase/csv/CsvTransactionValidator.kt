package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.csv.CsvLineResult
import fr.sacane.jmanager.domain.models.csv.CsvTransactionLine
import fr.sacane.jmanager.domain.models.defaultTags
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Validateur et convertisseur de lignes CSV en transactions
 */
class CsvTransactionValidator {

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        private const val MAX_LABEL_LENGTH = 200
    }

    /**
     * Converts a CSV line to Transaction without validation (assumes data is already validated)
     * Use this method when the CSV has been validated by CsvFileValidator first
     *
     * @param line The CSV line to convert
     * @param availableTags Available tags for the user
     * @return CsvLineResult.Success with the transaction
     */
    fun convertToTransaction(line: CsvTransactionLine, availableTags: List<Tag>): CsvLineResult {
        val date = LocalDate.parse(line.date.trim(), DATE_FORMATTER)
        val label = line.label.trim()

        val (amount, isIncome) = if (line.depense.isNotBlank()) {
            val value = BigDecimal(line.depense.trim().replace(',', '.'))
            Pair(Amount(value), false)
        } else {
            val value = BigDecimal(line.recette.trim().replace(',', '.'))
            Pair(Amount(value), true)
        }

        val tag = findOrDefaultTag(line.tag, availableTags)

        return CsvLineResult.Success(
            Transaction(
                id = null,
                label = label,
                date = date,
                amount = amount,
                isIncome = isIncome,
                tag = tag
            )
        )
    }

    /**
     * Valide et convertit une ligne CSV en Transaction
     * Use this when you need both validation and conversion (e.g., when not using CsvFileValidator first)
     *
     * @param line La ligne CSV à valider
     * @param availableTags Les tags disponibles pour l'utilisateur
     * @return CsvLineResult.Success avec la transaction ou CsvLineResult.Error avec les erreurs
     */
    fun validateAndConvert(line: CsvTransactionLine, availableTags: List<Tag>): CsvLineResult {
        val errors = mutableListOf<String>()

        val date = validateDate(line.date, errors)
        val label = validateLabel(line.label, errors)
        val (amount, isIncome) = validateAmounts(line.depense, line.recette, errors)
        val tag = validateTag(line.tag, availableTags)

        return if (errors.isEmpty() && date != null && label != null && amount != null) {
            CsvLineResult.Success(
                Transaction(
                    id = null,
                    label = label,
                    date = date,
                    amount = amount,
                    isIncome = isIncome,
                    tag = tag
                )
            )
        } else {
            CsvLineResult.Error(line.lineNumber, errors)
        }
    }

    private fun findOrDefaultTag(tagStr: String, availableTags: List<Tag>): Tag {
        if (tagStr.isBlank()) {
            return Tag.noneTag()
        }

        val trimmedTag = tagStr.trim()
        val matchingTag = availableTags.firstOrNull {
            it.label.equals(trimmedTag, ignoreCase = true)
        }

        if (matchingTag != null) {
            return matchingTag
        }

        val defaultTag = defaultTags.firstOrNull {
            it.label.equals(trimmedTag, ignoreCase = true)
        }

        return defaultTag ?: Tag.noneTag()
    }

    private fun validateDate(dateStr: String, errors: MutableList<String>): LocalDate? {
        if (dateStr.isBlank()) {
            errors.add("La date est obligatoire")
            return null
        }

        return try {
            LocalDate.parse(dateStr.trim(), DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            errors.add("Format de date invalide. Format attendu: jj-MM-aaaa (exemple: 15-01-2025)")
            null
        }
    }

    private fun validateLabel(labelStr: String, errors: MutableList<String>): String? {
        if (labelStr.isBlank()) {
            errors.add("Le libellé est obligatoire")
            return null
        }

        val trimmedLabel = labelStr.trim()
        if (trimmedLabel.length > MAX_LABEL_LENGTH) {
            errors.add("Le libellé ne peut pas dépasser $MAX_LABEL_LENGTH caractères")
            return null
        }

        return trimmedLabel
    }

    /**
     * Valide que soit dépense soit recette est renseigné, mais pas les deux
     * @return Pair<Amount?, Boolean> où Boolean indique si c'est une recette (true) ou dépense (false)
     */
    private fun validateAmounts(depenseStr: String, recetteStr: String, errors: MutableList<String>): Pair<Amount?, Boolean> {
        val depenseEmpty = depenseStr.isBlank()
        val recetteEmpty = recetteStr.isBlank()

        // Les deux sont vides
        if (depenseEmpty && recetteEmpty) {
            errors.add("Vous devez renseigner soit une dépense soit une recette")
            return Pair(null, false)
        }

        // Les deux sont remplis
        if (!depenseEmpty && !recetteEmpty) {
            errors.add("Vous ne pouvez pas renseigner à la fois une dépense et une recette")
            return Pair(null, false)
        }

        // Validation de la dépense
        if (!depenseEmpty) {
            val amount = parseAmount(depenseStr.trim(), errors, "dépense")
            return Pair(amount, false)
        }

        // Validation de la recette
        val amount = parseAmount(recetteStr.trim(), errors, "recette")
        return Pair(amount, true)
    }

    private fun parseAmount(amountStr: String, errors: MutableList<String>, fieldName: String): Amount? {
        return try {
            // Remplacer la virgule par un point pour le parsing
            val normalizedStr = amountStr.replace(',', '.')
            val value = BigDecimal(normalizedStr)

            if (value < BigDecimal.ZERO) {
                errors.add("Le montant de la $fieldName ne peut pas être négatif")
                return null
            }

            Amount(value)
        } catch (e: NumberFormatException) {
            errors.add("Format de montant invalide pour la $fieldName. Utilisez des nombres avec point ou virgule (exemple: 123.45 ou 123,45)")
            null
        }
    }

    private fun validateTag(tagStr: String, availableTags: List<Tag>): Tag {
        if (tagStr.isBlank()) {
            return Tag.noneTag()
        }

        val trimmedTag = tagStr.trim()

        // Chercher d'abord dans les tags disponibles (incluant les tags par défaut)
        val matchingTag = availableTags.firstOrNull {
            it.label.equals(trimmedTag, ignoreCase = true)
        }

        if (matchingTag != null) {
            return matchingTag
        }

        // Si pas trouvé, chercher dans les tags par défaut
        val defaultTag = defaultTags.firstOrNull {
            it.label.equals(trimmedTag, ignoreCase = true)
        }

        // Si toujours pas trouvé, retourner le tag "Aucune"
        return defaultTag ?: Tag.noneTag()
    }
}

