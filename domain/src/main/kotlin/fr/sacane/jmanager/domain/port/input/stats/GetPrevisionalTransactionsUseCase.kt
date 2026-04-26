package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.PrevisionalTransactionsOutput
import fr.sacane.jmanager.domain.utils.Result

@Port(Side.APPLICATION)
interface GetPrevisionalTransactionsUseCase {
    fun handle(query: GetPrevisionalTransactionsQuery): Result<PrevisionalTransactionsOutput>
}
