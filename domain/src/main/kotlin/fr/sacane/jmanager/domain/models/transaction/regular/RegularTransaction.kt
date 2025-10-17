package fr.sacane.jmanager.domain.models.transaction.regular

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.BaseTransaction
import java.time.LocalDate

enum class Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ONE_OFF
}

@JvmInline
value class RegularTransactionId(val value: String)

sealed interface RegularTransaction: BaseTransaction {
    val id: RegularTransactionId?
    val startDate: LocalDate
    val frequencyProperty: FrequencyProperty
    val associatedBooklets: List<Booklet>
}


// Monthly Transaction feature

data class MonthlyTransaction(
    override var label: String,
    override var amount: Amount,
    override var isIncome: Boolean,
    override val id: RegularTransactionId,
    override val startDate: LocalDate,
    override val frequencyProperty: FrequencyProperty,
    override var tag: Tag? = null,
    override val associatedBooklets: List<Booklet> = listOf(),
    val monthlyRepeatProperty: MonthlyRepeatProperty? = null
): RegularTransaction {
    init {
        require(monthlyRepeatProperty == null || monthlyRepeatProperty.repeatDay in 1..31) {
            "Repeat day must be between 1 and 31"
        }
    }
}

data class MonthlyRepeatProperty(
    val repeatDay: Int
)