package fr.sacane.jmanager.application.api.setup

import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.input.transaction.BookTransactionUseCase
import fr.sacane.jmanager.application.State
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModel
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component

data class BookletTransaction(
    val bookletOwnerId: UserId,
    val bookletName: String,
    val transactions: List<Transaction>,
    val token: String,
)

@Component
class TransactionStateTestAdapter(
    private val transactionJpaRepository: TransactionJpaRepository,
    private val bookTransactionUseCase: BookTransactionUseCase
): State<BookletTransaction, Transaction> {
    @Transactional
    override fun get(): Collection<Transaction> {
        return transactionJpaRepository.findAll()
            .map { it.toModel() }
    }

    override fun clear() {
        transactionJpaRepository.deleteAll()
    }

    override fun init(initialState: Collection<BookletTransaction>) {
        initialState.forEach {
            it.transactions.forEach { tr ->
                bookTransactionUseCase.bookTransaction(SessionToken(it.token), it.bookletName, tr)
            }
        }
    }
}