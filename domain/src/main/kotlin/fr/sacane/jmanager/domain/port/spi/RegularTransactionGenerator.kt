package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction

@Port(Side.INFRASTRUCTURE)
interface RegularTransactionGenerator {
    fun generateMissingRegularTransactions(regularTransactions: List<RegularTransaction>): Result<RegularTransactionGenerationReport>
}


data class RegularTransactionGenerationReport(
    val numberOfGenerated: Int
)