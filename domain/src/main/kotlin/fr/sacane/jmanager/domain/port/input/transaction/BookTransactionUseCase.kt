package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.*
import org.slf4j.LoggerFactory

data class BookTransactionCommand(
    val userId: UserId,
    val bookletLabel: String,
    val transaction: Transaction
) : Command<TransactionResumeResult>

@Port(Side.APPLICATION)
interface BookTransactionUseCase : CommandHandler<BookTransactionCommand, TransactionResumeResult> {
    override val commandClass get() = BookTransactionCommand::class
}

@DomainService
class BookTransactionService(
    private val transactionRepository: TransactionRepository,
    private val bookletRepository: BookletRepository,
    private val infraTransactionManager: UnitOfWorkTransactionProvider,
    private val tagRepository: TagRepository
) : BookTransactionUseCase {

    companion object {
        private val log = LoggerFactory.getLogger(BookTransactionService::class.java)
    }

    override fun handle(command: BookTransactionCommand): Result<TransactionResumeResult> {
        val userId = command.userId
        return infraTransactionManager.executeInTransaction(command.transaction) {
            if (command.transaction.amount.isNegative()) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_ENTRY_ERROR,
                    "Le montant de la transaction ne peut pas être négatif",
                    "domain.transaction.book.negative_amount"
                )
            }
            val booklet = bookletRepository.findBookletByLabelWithTransactions(userId, command.bookletLabel)
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
            val toSaveTransaction = if (newTr.tag == null) {
                newTr.copy(tag = tagRepository.defaultTag())
            } else newTr
            booklet.addTransaction(toSaveTransaction)
            bookletRepository.update(booklet)
            log.info("Transaction created: id={}, bookletId={}", toSaveTransaction.id, booklet.id)
            success(TransactionResumeResult(toSaveTransaction, booklet.amount))
        }
    }
}
