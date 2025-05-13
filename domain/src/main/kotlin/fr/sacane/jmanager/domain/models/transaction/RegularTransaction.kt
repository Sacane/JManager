package fr.sacane.jmanager.domain.models.transaction

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import java.time.LocalDate

enum class Regularity {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    ONE_OFF
}

@JvmInline
value class RegularTransactionId(val value: String)

data class RegularTransaction (
    val id: RegularTransactionId,
    val startDate: LocalDate,
    override var label: String,
    override var amount: Amount,
    override var isIncome: Boolean,
    override var tag: Tag = Tag("Aucune", isDefault = true),
    val regularity: Regularity = Regularity.MONTHLY
): BaseTransaction() {

}