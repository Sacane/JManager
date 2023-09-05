package fr.sacane.jmanager.domain.models

import java.time.LocalDate

class Sheet(
    val id: Long?,
    val label: String,
    val date: LocalDate,
    val expenses: Double,
    val income: Double,
    private var accountAmount: Double,
    val category: Category = CategoryFactory.DEFAULT_CATEGORY,
    var position: Int = 0
) {
    val sold: Double
        get() = accountAmount
    fun updateSoldStartingWith(start: Double) {
        accountAmount = start.plus(income).minus(expenses).also { println( "$it -> $start" ) }
    }

    override fun toString(): String {
        return """
            label: $label
            date: $date
            expenses: $expenses
            income: $income
            accountAmount: $accountAmount
            position: $position
        """.trimIndent()
    }
}