package fr.sacane.jmanager.domain.usecase.csv

import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.csv.CsvLineResult
import fr.sacane.jmanager.domain.models.csv.CsvTransactionLine
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Validateur et convertisseur de lignes CSV en transactions
 */
@UseCase
class CsvTransactionValidator {

    companion object {
        private const val MAX_LABEL_LENGTH = 200
    }

    /**
     * Converts a CSV line to Transaction without validation (assumes data is already validated)
     * Use this method when the CSV has been validated by CsvFileValidator first
     *
     * @param line The CSV line to convert
     * @param availableTags Available tags for the user
     * @param month Optional month (1-12) to use when date contains only day
     * @param year Optional year to use when date contains only day
     * @return CsvLineResult.Success with the transaction
     */
    fun convertToTransaction(line: CsvTransactionLine, availableTags: List<Tag>, month: Int? = null, year: Int? = null): CsvLineResult {
        val date = CsvValidationUtils.parseDate(line.date.trim(), month, year)!!
        val label = line.label.trim()

        val (amount, isIncome) = if (line.depense.isNotBlank()) {
            val value = BigDecimal(line.depense.trim().replace(',', '.'))
            Pair(Amount(value), false)
        } else {
            val value = BigDecimal(line.recette.trim().replace(',', '.'))
            Pair(Amount(value), true)
        }

        val tag = CsvValidationUtils.validateTag(line.tag, availableTags)

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
     * @param month Optional month (1-12) to use when date contains only day
     * @param year Optional year to use when date contains only day
     * @return CsvLineResult.Success avec la transaction ou CsvLineResult.Error avec les erreurs
     */
    fun validateAndConvert(line: CsvTransactionLine, availableTags: List<Tag>, month: Int? = null, year: Int? = null): CsvLineResult {
        val errors = mutableListOf<String>()

        val date = validateDate(line.date, errors, month, year)
        val label = validateLabel(line.label, errors)
        val (amount, isIncome) = validateAmounts(line.depense, line.recette, errors)
        val tag = CsvValidationUtils.validateTag(line.tag, availableTags)

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

    private fun validateDate(dateStr: String, errors: MutableList<String>, month: Int?, year: Int?): LocalDate? {
        if (dateStr.isBlank()) {
            errors.add("La date est obligatoire")
            return null
        }

        val date = CsvValidationUtils.parseDate(dateStr, month, year)
        if (date == null) {
            if (month != null && year != null) {
                errors.add("Format de date invalide. Formats acceptés: jj-MM-aaaa (exemple: 15-01-2025) ou jj seul (exemple: 15)")
            } else {
                errors.add("Format de date invalide. Format attendu: jj-MM-aaaa (exemple: 15-01-2025)")
            }
        }
        return date
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

        if (depenseEmpty && recetteEmpty) {
            errors.add("Vous devez renseigner soit une dépense soit une recette")
            return Pair(null, false)
        }

        if (!depenseEmpty && !recetteEmpty) {
            errors.add("Vous ne pouvez pas renseigner à la fois une dépense et une recette")
            return Pair(null, false)
        }

        if (!depenseEmpty) {
            val amount = CsvValidationUtils.parseAmount(depenseStr.trim(), errors, "dépense")
            return Pair(amount, false)
        }

        val amount = CsvValidationUtils.parseAmount(recetteStr.trim(), errors, "recette")
        return Pair(amount, true)
    }
}

