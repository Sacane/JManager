package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.transaction.RegularTransaction
import fr.sacane.jmanager.domain.port.spi.RegularTransactionRepository
import org.springframework.stereotype.Repository

@Repository
class DataJpaRegularTransactionRepositoryAdapter: RegularTransactionRepository {
    override fun saveRegularTransaction(regularTransaction: RegularTransaction): RegularTransaction {
        TODO("Not yet implemented")
    }
}