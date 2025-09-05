package fr.sacane.jmanager.domain.models.transaction.regular

import fr.sacane.jmanager.domain.models.Amount
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
}

class MonthlyTransaction(
    override var label: String,
    override var amount: Amount,
    override var isIncome: Boolean,
    override val id: RegularTransactionId,
    override val startDate: LocalDate,
    override val frequencyProperty: FrequencyProperty,
    override var tag: Tag = Tag("Aucune", isDefault = true),
): RegularTransaction