package fr.sacane.jmanager.domain.models.transaction.regular

import java.time.LocalDate

data class RegularTransactionTracker(
    val id: Long? = null,
    val regularTransactionId: RegularTransactionId,
    val bookletId: Long,
    val lastGeneratedDate: LocalDate
)