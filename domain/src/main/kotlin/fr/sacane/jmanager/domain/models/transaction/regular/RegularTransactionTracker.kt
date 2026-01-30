package fr.sacane.jmanager.domain.models.transaction.regular

import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class RegularTransactionTracker(
    val id: Long? = null,
    val regularTransactionId: RegularTransactionId,
    val bookletId: UUID,
    val lastGeneratedDate: LocalDate,
    val numberOfGeneratedTransaction: Int = 0,
    val excludedMonths: Set<YearMonth> = emptySet()
)
