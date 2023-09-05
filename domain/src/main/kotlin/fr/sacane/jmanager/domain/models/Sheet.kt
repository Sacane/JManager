package fr.sacane.jmanager.domain.models

import java.time.LocalDate

class Sheet(
    val id: Long?,
    val label: String,
    val date: LocalDate,
    val expenses: Double,
    val income: Double,
    var sold: Double,
    val category: Category = CategoryFactory.DEFAULT_CATEGORY,
    var position: Int = 0
) {

    fun updateSoldStartingWith(start: Double) {
        sold = start.plus(income).minus(expenses)
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
}