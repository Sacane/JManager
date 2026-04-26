package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.models.SessionToken
import java.time.LocalDate
import java.util.UUID

data class GetCategoryDistributionQuery(
    val token: SessionToken,
    val bookletId: UUID? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)
