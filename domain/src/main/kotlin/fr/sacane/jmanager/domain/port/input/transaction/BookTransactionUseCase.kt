package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.TagRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.utils.*
import java.util.logging.Logger

data class BookTransactionCommand(
    val token: SessionToken,
    val bookletLabel: String,
    val transaction: Transaction
)

@Port(Side.APPLICATION)
interface BookTransactionUseCase {
    fun handle(command: BookTransactionCommand): Result<TransactionResumeResult>
}

@DomainService
class BookTransactionService(
    private val transactionRepository: TransactionRepository,
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val infraTransactionManager: UnitOfWorkTransactionProvider,
    private val tagRepository: TagRepository
) : BookTransactionUseCase {

    companion object {
        private val logger = Logger.getLogger(BookTransactionService::class.java.name)
    }

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(command: BookTransactionCommand): Result<TransactionResumeResult> = session.authenticate(command.token) { id ->
        return@authenticate infraTransactionManager.executeInTransaction(command.transaction) {
            logger.info("Request for a transaction with id $id")
            val booklet = bookletRepository.findBookletByLabelWithTransactions(id, command.bookletLabel)
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "Le livret ${command.bookletLabel} n'existe pas",
                    "domain.transaction.book.booklet_not_found"
                )
            val newTr = transactionRepository.save(booklet.id!!, command.transaction)
                ?: return@executeInTransaction domainFailure(
                    ResultState.INFRASTRUCTURE_ERROR,
                    "Erreur est survenu lors de la transaction",
                    "domain.transaction.book.infrastructure_error"
                )
            if (command.transaction.amount.isNegative()) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_ENTRY_ERROR,
                    "Le montant de la transaction ne peut pas être négatif",
                    "domain.transaction.book.negative_amount"
                )
            }
            val toSaveTransaction = if (newTr.tag == null) {
                newTr.copy(tag = tagRepository.defaultTag())
            } else newTr
            booklet.addTransaction(toSaveTransaction)
            bookletRepository.update(booklet)
            logger.info("Transaction $newTr has been created, the booklet sold has been updated : $booklet")
            success(TransactionResumeResult(newTr, booklet.amount))
        }
    }
}
