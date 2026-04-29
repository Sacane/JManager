package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.output.repository.TagRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.utils.*
import java.time.LocalDate
import java.util.UUID

data class ConfirmVirtualTransactionCommand(
    val token: SessionToken,
    val bookletId: UUID,
    val regularTransactionId: RegularTransactionId,
    val sourceMonth: Int,
    val sourceYear: Int,
    val label: String,
    val amount: Amount,
    val date: LocalDate,
    val isIncome: Boolean,
    val tagId: UUID? = null
) : Command<TransactionResumeResult>

@Port(Side.APPLICATION)
interface ConfirmVirtualTransactionUseCase : CommandHandler<ConfirmVirtualTransactionCommand, TransactionResumeResult> {
    override val commandClass get() = ConfirmVirtualTransactionCommand::class
}

@DomainService
class ConfirmVirtualTransactionService(
    private val transactionRepository: TransactionRepository,
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val trackerRepository: RegularTransactionTrackerRepository,
    private val tagRepository: TagRepository,
    private val infraTransactionManager: UnitOfWorkTransactionProvider
) : ConfirmVirtualTransactionUseCase {

    override fun handle(command: ConfirmVirtualTransactionCommand): Result<TransactionResumeResult> = session.authenticate(command.token) {
        return@authenticate infraTransactionManager.executeInTransaction(Any()) {
            val booklet = bookletRepository.findBookletByIdWithTransactions(command.bookletId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Booklet ${command.bookletId} not found",
                    "domain.virtual_transaction.confirm.booklet_not_found"
                )

            val tag = tagRepository.defaultTag()

            val transaction = Transaction(
                id = null,
                label = command.label,
                date = command.date,
                amount = command.amount,
                isIncome = command.isIncome,
                isPreview = false,
                tag = tag,
                regularTransactionId = command.regularTransactionId
            )

            val savedTransaction = transactionRepository.save(command.bookletId, transaction)
                ?: return@executeInTransaction domainFailure(
                    ResultState.INFRASTRUCTURE_ERROR,
                    "Could not save transaction",
                    "domain.virtual_transaction.confirm.save_failed"
                )

            booklet.addTransaction(savedTransaction)
            bookletRepository.update(booklet)

            trackerRepository.markMonthAsExcluded(
                regularTransactionId = command.regularTransactionId,
                bookletId = command.bookletId,
                year = command.sourceYear,
                month = java.time.Month.of(command.sourceMonth)
            )

            success(TransactionResumeResult(savedTransaction, booklet.amount))
        }
    }
}
