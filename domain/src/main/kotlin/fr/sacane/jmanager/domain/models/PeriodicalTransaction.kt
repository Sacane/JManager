package fr.sacane.jmanager.domain.models

import java.time.LocalDate
import java.util.UUID

enum class PeriodicalType {
    MONTHLY, YEARLY
}

data class PeriodicalTransaction (
    val periodicalTypeId: UUID = UUID.randomUUID(),
    val label: String,
    val amount: Amount,
    val periodicalType: PeriodicalType,
    val tag: Tag,
    val linkedAccountId: Long,
    val beginDate: LocalDate
)
