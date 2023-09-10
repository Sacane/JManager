package fr.sacane.jmanager.domain.models

import java.time.LocalDate

class Sheet(
    val id: Long?,
    var label: String,
    val date: LocalDate,
    var expenses: Double,
    var income: Double,
    var sold: Double,
    var category: Category = CategoryFactory.DEFAULT_CATEGORY,
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