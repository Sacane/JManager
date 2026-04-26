package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.DailyTrendStatsOutput
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface GetDailyTrendStatsUseCase {
    fun handle(query: GetDailyTrendStatsQuery): Result<DailyTrendStatsOutput>
}
