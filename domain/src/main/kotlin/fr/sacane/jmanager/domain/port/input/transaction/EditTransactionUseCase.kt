package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.port.input.MdcKeys
import fr.sacane.jmanager.domain.utils.*
import java.time.LocalDateTime
import java.util.UUID

data class EditTransactionCommand(
    val userId: UserId,
    val bookletID: UUID,
    val transaction: Transaction
) : Command<TransactionResumeResult>, MdcContextProvider {
    override fun mdcContext() = buildMap {
        put(MdcKeys.BOOKLET_ID, bookletID.toString())
        transaction.id?.let { put(MdcKeys.TRANSACTION_ID, it.toString()) }
    }
}

@Port(Side.APPLICATION)
interface EditTransactionUseCase : CommandHandler<EditTransactionCommand, TransactionResumeResult> {
    override val commandClass get() = EditTransactionCommand::class
}

@DomainService
class EditTransactionService(
    private val transactionRepository: TransactionRepository,
    private val bookletRepository: BookletRepository,
    private val infraTransactionManager: UnitOfWorkTransactionProvider
) : EditTransactionUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    private fun <S> domainNotFound(detail: String, key: String): Result<S> {
        return domainFailure(ResultState.NOT_FOUND, detail, key)
    }

    override fun handle(command: EditTransactionCommand): Result<TransactionResumeResult> {
        return infraTransactionManager.executeInTransaction(command.transaction) {
            if (command.transaction.id == null) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_ENTRY_ERROR,
                    "L'ID de la transaction est null",
                    "domain.transaction.edit.id_missing"
                )
            }
            val registeredBooklet = bookletRepository.findBookletByIdWithTransactions(command.bookletID)
                ?: return@executeInTransaction domainNotFound(
                    "Le livret ${command.bookletID} n'existe pas",
                    "domain.transaction.edit.booklet_not_found"
                )
            val transactionFromDatabase = registeredBooklet.findTransactionById(command.transaction.id)?.copy()
                ?: return@executeInTransaction domainNotFound(
                    "Aucune transaction n'existe avec l'ID suivant : ${command.transaction.id}",
                    "domain.transaction.edit.transaction_not_found"
                )
            transactionFromDatabase.updateFromOther(command.transaction)
            transactionFromDatabase.lastModified = LocalDateTime.now()
            transactionRepository.save(registeredBooklet.id!!, transactionFromDatabase)
                ?: return@executeInTransaction domainFailure(
                    state = ResultState.INVALID,
                    "Une erreur est survenue lors de la mise à jour de la transaction ${transactionFromDatabase.id}",
                    "domain.transaction.edit.save_failed"
                )
            registeredBooklet.removeTransactionById(command.transaction.id)
            registeredBooklet.addTransaction(transactionFromDatabase)
            bookletRepository.update(registeredBooklet)
            success(TransactionResumeResult(transactionFromDatabase, registeredBooklet.amount))
        }
    }
}
