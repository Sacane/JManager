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
    var lastModified: LocalDateTime = LocalDateTime.now(),
    var isPreview: Boolean = false
) {
    fun updateFromOther(other: Transaction): Boolean {
        if(other.id != this.id) return false
        this.label = other.label
        this.date = other.date
        this.amount = other.amount
        this.isIncome = other.isIncome
        this.tag = other.tag
        this.isPreview = other.isPreview
        return true
    }

    override fun toString(): String {
        return """
            label: $label
            date: $date
            value: $amount
            isIncome: $isIncome
            tag: $tag
            lastModified: $lastModified
        """.trimIndent()
    }

    fun <T> exportAmountValues(function: (BigDecimal, Boolean) -> T): T{
        return function(amount.amount, isIncome)
    }
}