package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.infrastructure.State
import fr.sacane.jmanager.infrastructure.spi.adapters.regular.RegularTransactionRepositoryDataJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.RegularTransactionResourceJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.util.UUID

data class BookletRegularTransactionInput(
    val userId: UserId,
    val bookletID: String,
    val regularTransaction: RegularTransaction
)

@Component
class RegularTransactionStateForTestAdapter(
    private val regularTransactionAdapter: RegularTransactionRepositoryDataJpaAdapter,
    private val regularTransactionJpaRepository: RegularTransactionResourceJpaRepository
): State<BookletRegularTransactionInput, RegularTransaction> {
    override fun get(): Collection<RegularTransaction> {
        return regularTransactionJpaRepository.findAllWithBooklets().map { it.toDomain() }
    }

    @Transactional
    override fun init(initialState: Collection<BookletRegularTransactionInput>) {
        initialState.forEach {
            regularTransactionAdapter.saveRegularTransaction(
                it.userId,
                it.regularTransaction,
                listOf(UUID.fromString(it.bookletID))
            )
        }
    }

    override fun clear() {
        regularTransactionJpaRepository.deleteAll()
    }
}

