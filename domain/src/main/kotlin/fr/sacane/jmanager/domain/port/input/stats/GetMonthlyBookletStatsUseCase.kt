package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.MonthlyBookletStatsOutput
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.utils.Result
import java.util.UUID

@Port(Side.APPLICATION)
interface GetMonthlyBookletStatsUseCase {
    fun getMonthlyBookletStats(bookletId: UUID, year: Int, token: SessionToken): Result<MonthlyBookletStatsOutput>
}
