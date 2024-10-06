package fr.sacane.jmanager.domain.models

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class Transaction(
    val id: Long?,
    var label: String,
    var date: LocalDate,
    var amount: Amount,
    var isIncome: Boolean,
    var tag: Tag = Tag("Aucune", isDefault = true),
    var position: Int = 0,
    var lastModified: LocalDateTime = LocalDateTime.now()
) {


    fun updateFromOther(other: Transaction): Boolean {
        if(other.id != this.id) return false
        this.label = other.label
        this.date = other.date
        this.amount = other.amount
        this.isIncome = other.isIncome
        this.tag = other.tag
        this.position = other.position
        return true
    }

    override fun toString(): String {
        return """
            label: $label
            date: $date
            value: $amount
            isIncome: $isIncome
            position: $position
            tag: $tag
            lastModified: $lastModified
        """.trimIndent()
    }

    fun <T> exportAmountValues(function: (BigDecimal, Boolean) -> T): T{
        return function(amount.amount, isIncome)
    }
}