package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.models.SessionToken
import java.time.LocalDate
import java.util.UUID

data class GetPrevisionalTransactionsQuery(
    val token: SessionToken,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val bookletId: UUID? = null
)
