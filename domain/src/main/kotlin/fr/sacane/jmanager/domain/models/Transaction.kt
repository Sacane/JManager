package fr.sacane.jmanager.domain.models

import java.math.BigDecimal
import java.time.LocalDate

class Transaction(
    val id: Long?,
    var label: String,
    var date: LocalDate,
    var expenses: Amount,
    var income: Amount,
    var sold: Amount,
    var category: Tag = Tag("Aucune"),
    var position: Int = 0
) {
    fun updateSoldStartingWith(start: Amount) {
        sold = start.plus(income).minus(expenses)
    }

    private fun updateSoldFromIncomeAndExpenses(expenses: Amount, income: Amount) {
        sold = sold.plus(this.expenses)
            .plus(income)
            .minus(expenses)
            .minus(this.income)
    }

    fun updateFromOther(other: Transaction): Boolean {
        if(other.id != this.id) return false
        updateSoldFromIncomeAndExpenses(other.expenses, other.income)
        this.label = other.label
        this.date = other.date
        this.expenses = other.expenses
        this.income = other.income
        this.category = other.category
        return true
    }

    override fun toString(): String {
        return """
            label: $label
            date: $date
            expenses: $expenses
            income: $income
            sold: $sold
            position: $position
        """.trimIndent()
    }

    fun <T> exportAmountValues(function: (BigDecimal, BigDecimal, BigDecimal) -> T): T{
        return expenses.applyOnValue { expenseValue ->
            income.applyOnValue { incomeValue ->
                sold.applyOnValue { soldValue ->
                    function(expenseValue, incomeValue, soldValue)
                }
            }
        }
    }
}