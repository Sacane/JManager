package fr.sacane.jmanager.domain.models

import java.time.LocalDate
import java.time.LocalDateTime

data class Transaction(
    val id: Long?,
    var label: String,
    var date: LocalDate,
    private var _amount: Amount,
    var isIncome: Boolean,
    var tag: Tag = Tag("Aucune", isDefault = true),
    var lastModified: LocalDateTime = LocalDateTime.now(),
    var isPreview: Boolean = false,
    val fromSubscriptionFrom: SubscriptionFrom = NotFromSubscription()
) {
    val amount: Amount
        get() = _amount

    val isNotPreview: Boolean
    get() = !isPreview

    fun updateFromOther(other: Transaction): Boolean {
        if(other.id != this.id) return false
        this.label = other.label
        this.date = other.date
        this._amount = other._amount
        this.isIncome = other.isIncome
        this.tag = other.tag
        this.isPreview = other.isPreview
        return true
    }

    override fun toString(): String {
        return """
            label: $label
            date: $date
            value: $_amount
            isIncome: $isIncome
            tag: $tag
            lastModified: $lastModified
        """.trimIndent()
    }
}