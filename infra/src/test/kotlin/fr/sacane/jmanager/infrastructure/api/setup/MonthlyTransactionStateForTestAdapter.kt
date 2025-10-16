package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.infrastructure.State
import fr.sacane.jmanager.infrastructure.spi.adapters.regular.RegularTransactionRepositoryDataJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.repositories.MonthlyTransactionResourceJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

data class BookletMonthlyTransactionInput(
    val userId: UserId,
    val bookletID: Long,
    val regularTransaction: MonthlyTransaction
)

@Component
class MonthlyTransactionStateForTestAdapter(
    private val regularTransactionAdapter: RegularTransactionRepositoryDataJpaAdapter,
    private val monthlyTransactionJpaRepository: MonthlyTransactionResourceJpaRepository
): State<BookletMonthlyTransactionInput, RegularTransaction> {
    override fun get(): Collection<RegularTransaction> {
        return monthlyTransactionJpaRepository.findAll().map { it.toDomain() }
    }

    @Transactional
    override fun init(initialState: Collection<BookletMonthlyTransactionInput>) {
        initialState.forEach {
            regularTransactionAdapter.saveMonthlyRegularTransaction(
                it.userId,
                it.regularTransaction,
                listOf(it.bookletID)
            )
        }
    }

    override fun clear() {
        monthlyTransactionJpaRepository.deleteAll()
    }

}