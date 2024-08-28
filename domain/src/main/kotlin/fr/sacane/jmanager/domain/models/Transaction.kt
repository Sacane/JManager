package fr.sacane.jmanager.domain.models

import java.math.BigDecimal
import java.time.LocalDate

class Transaction(
    val id: Long?,
    var label: String,
    var date: LocalDate,
    var value: Amount,
    var isIncome: Boolean,
    var sold: Amount,
    var tag: Tag = Tag("Aucune", isDefault = true),
    var position: Int = 0
) {
    fun updateSoldStartingWith(start: Amount) {
        sold = if(isIncome) start.plus(value) else start.minus(value)
    }

    private fun updateSoldFromIncomeAndExpenses(value: Amount, isIncome: Boolean) {
        sold = if(isIncome) sold.plus(value) else sold.minus(value)
    }

    fun updateFromOther(other: Transaction): Boolean {
        if(other.id != this.id) return false
        updateSoldFromIncomeAndExpenses(other.value, other.isIncome)
        this.label = other.label
        this.date = other.date
        this.value = other.value
        this.isIncome = other.isIncome
        this.tag = other.tag
        return true
    }

    override fun toString(): String {
        return """
            label: $label
            date: $date
            value: $value
            isIncome: $isIncome
            sold: $sold
            position: $position
            tag: $tag
        """.trimIndent()
    }

    fun <T> exportAmountValues(function: (BigDecimal, Boolean, BigDecimal) -> T): T{
        return value.applyOnValue { expenseValue ->
            sold.applyOnValue { soldValue ->
                function(expenseValue, isIncome, soldValue)
            }
        }
    }
}