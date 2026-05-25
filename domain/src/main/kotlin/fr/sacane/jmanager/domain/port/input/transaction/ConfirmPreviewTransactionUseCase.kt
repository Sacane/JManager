package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.port.input.MdcKeys
import fr.sacane.jmanager.domain.utils.*
import java.time.LocalDate
import java.util.UUID

data class ConfirmPreviewTransactionCommand(
    val userId: UserId,
    val bookletID: UUID,
    val transactionId: UUID,
    val newAmount: Amount? = null,
    val newDate: LocalDate? = null
) : Command<TransactionResumeResult>, MdcContextProvider {
    override fun mdcContext() = mapOf(
        MdcKeys.BOOKLET_ID to bookletID.toString(),
        MdcKeys.TRANSACTION_ID to transactionId.toString()
    )
}

@Port(Side.APPLICATION)
interface ConfirmPreviewTransactionUseCase : CommandHandler<ConfirmPreviewTransactionCommand, TransactionResumeResult> {
    override val commandClass get() = ConfirmPreviewTransactionCommand::class
}

@DomainService
class ConfirmPreviewTransactionService(
    private val transactionRepository: TransactionRepository,
    private val bookletRepository: BookletRepository,
    private val infraTransactionManager: UnitOfWorkTransactionProvider
) : ConfirmPreviewTransactionUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(command: ConfirmPreviewTransactionCommand): Result<TransactionResumeResult> {
        return infraTransactionManager.executeInTransaction(Unit) {
            val booklet = bookletRepository.findBookletByIdWithTransactions(command.bookletID)
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Booklet ${command.bookletID} not found",
                    "domain.transaction.confirm.booklet_not_found"
                )
            val transaction = booklet.findTransactionById(command.transactionId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Transaction not found",
                    "domain.transaction.confirm.transaction_not_found"
                )

            if (transaction.isNotPreview) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_ENTRY_ERROR,
                    "Transaction ${command.transactionId} is not preview",
                    "domain.transaction.confirm.not_preview"
                )
            }

            booklet.removeTransactionById(command.transactionId)
            if (command.newAmount != null) {
                transaction.amount = command.newAmount
            }
            if (command.newDate != null) {
                transaction.date = command.newDate
            }
            transaction.isPreview = false

            transactionRepository.save(command.bookletID, transaction)
                ?: return@executeInTransaction domainFailure(
                    ResultState.INFRASTRUCTURE_ERROR,
                    "Could not confirm transaction ${command.transactionId}",
                    "domain.transaction.confirm.save_failed"
                )

            booklet.addTransaction(transaction)
            bookletRepository.update(booklet)
            return@executeInTransaction success(TransactionResumeResult(transaction, booklet.amount))
        }
    }
}
