package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.Frequency
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.infrastructure.State
import fr.sacane.jmanager.infrastructure.spi.adapters.DataJpaRegularTransactionRepositoryAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.toDomain
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

data class OwnerRegularTransaction(
    val ownerId: UserId,
    val transactions: List<RegularTransaction>,
    val token: String,
)

@Component
class RegularTransactionStateTestAdapter(
    private val regularTransactionJpaRepository: RegularTransactionJpaRepository,
    private val regularTransactionRepositoryAdapter: DataJpaRegularTransactionRepositoryAdapter
): State<OwnerRegularTransaction, RegularTransaction> {
    @Transactional
    override fun get(): Collection<RegularTransaction> {
        return regularTransactionJpaRepository.findAll()
            .map { it.toDomain() }
    }

    override fun clear() {
         regularTransactionJpaRepository.deleteAll()
    }

    override fun init(initialState: Collection<OwnerRegularTransaction>) {
        initialState.forEach {
            it.transactions.forEach { transaction ->
                regularTransactionRepositoryAdapter.saveRegularTransaction(
                    userId = it.ownerId,
                    startDate = transaction.startDate,
                    label = transaction.label,
                    amount = transaction.amount,
                    isIncome = transaction.isIncome,
                    tag = transaction.tag,
                    frequency = Frequency.MONTHLY
                )
            }
        }
    }
}