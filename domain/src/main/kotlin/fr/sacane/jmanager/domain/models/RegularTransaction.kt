package fr.sacane.jmanager.domain.models

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

class RegularTransaction (
    val id: RegularTransactionId,
    val label: String,
    val amount: Amount,
    val isIncome: Boolean,
    val tag: Tag = Tag("Aucune", isDefault = true),
    val regularity: Regularity = Regularity.MONTHLY,
    val startDate: LocalDate
) {
    private val _listOfOneOffDate = mutableListOf<LocalDate>()

    val oneOffDate: List<LocalDate>
        get() = _listOfOneOffDate.toList()

    fun addOneOffDate(date: LocalDate) {
        if(regularity == Regularity.ONE_OFF) {
            _listOfOneOffDate + date
        }
    }
}