package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.models.SessionToken
import java.util.UUID

data class GetMonthlyBookletStatsQuery(
    val bookletId: UUID,
    val year: Int,
    val token: SessionToken
)
