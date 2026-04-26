package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.TrendStatsOutput
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface GetTrendStatsUseCase {
    fun handle(query: GetTrendStatsQuery): Result<TrendStatsOutput>
}
