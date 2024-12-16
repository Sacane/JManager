package fr.sacane.jmanager.infrastructure.api.setup

import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Transaction
import fr.sacane.jmanager.domain.port.api.TransactionFeature
import fr.sacane.jmanager.infrastructure.State
import fr.sacane.jmanager.infrastructure.spi.adapters.SqlTransactionAdapter
import fr.sacane.jmanager.infrastructure.spi.adapters.toModel
import fr.sacane.jmanager.infrastructure.spi.repositories.TransactionJpaRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Component
import java.util.UUID

data class AccountTransaction(
    val account: Account,
    val transactions: List<Transaction>,
    val token: UUID = UUID.randomUUID(),
)

@Component
class TransactionStateTestAdapter(
    private val transactionJpaRepository: TransactionJpaRepository,
    private val sqlTransactionAdapter: SqlTransactionAdapter,
    private val transactionFeature: TransactionFeature
): State<AccountTransaction, Transaction> {
    @Transactional
    override fun get(): Collection<Transaction> {
        return transactionJpaRepository.findAll()
            .map { it.toModel() }
    }

    override fun clear() {
        transactionJpaRepository.deleteAll()
    }

    override fun init(initialState: Collection<AccountTransaction>) {
        initialState.forEach {
            it.transactions.forEach { tr ->
                sqlTransactionAdapter.persist(
                    it.account.owner!!.id,
                    accountLabel = it.account.label,
                    transaction = tr
                )
                transactionFeature.bookTransaction(it.account.owner!!.id, it.token, it.account.label, tr)
            }
        }
    }
}