package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import java.time.Month

class InMemoryRegularTransactionGeneratorRepository: RegularTransactionGenerator {
    override fun generateMissingPrevisionalTransactions(
        bookletId: Long,
        regularTransactions: List<RegularTransaction>,
        targetMonth: Month,
        targetYear: Int
    ): List<Transaction> {
        TODO("Not yet implemented")
    }


}
