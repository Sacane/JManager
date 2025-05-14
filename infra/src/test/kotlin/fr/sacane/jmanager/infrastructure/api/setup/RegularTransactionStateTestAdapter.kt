package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.RegularTransaction
import fr.sacane.jmanager.domain.port.api.RegularTransactionFeature
import fr.sacane.jmanager.infrastructure.State
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
    private val regularTransactionFeature: RegularTransactionFeature
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
            it.transactions.forEach { tr ->
                regularTransactionFeature.bookRegularTransaction(
                    token = it.token,
                    startDate = tr.startDate,
                    label = tr.label,
                    amount = tr.amount,
                    isIncome = tr.isIncome,
                    tag = tr.tag,
                    regularity = tr.regularity
                )
            }
        }
    }
}