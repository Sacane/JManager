package fr.sacane.jmanager.domain.models.transaction

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import java.time.LocalDate
import java.time.LocalDateTime

data class Transaction(
    val id: Long?,
    override var label: String,
    var date: LocalDate,
    override var amount: Amount,
    override var isIncome: Boolean,
    var lastModified: LocalDateTime = LocalDateTime.now(),
    var isPreview: Boolean = false,
    override var tag: Tag? = null,
    val regularTransactionId: RegularTransactionId? = null
): BaseTransaction {
    val isNotPreview: Boolean
    get() = !isPreview

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
}