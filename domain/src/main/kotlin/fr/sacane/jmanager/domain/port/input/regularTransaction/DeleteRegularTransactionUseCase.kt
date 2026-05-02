package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.*

data class DeleteRegularTransactionCommand(
    val userId: UserId,
    val transactionId: String
) : Command<Boolean>

@Port(Side.APPLICATION)
interface DeleteRegularTransactionUseCase : CommandHandler<DeleteRegularTransactionCommand, Boolean> {
    override val commandClass get() = DeleteRegularTransactionCommand::class
}

@DomainService
class DeleteRegularTransactionService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val unitOfWork: UnitOfWorkTransactionProvider
) : DeleteRegularTransactionUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(
        command: DeleteRegularTransactionCommand
    ): Result<Boolean> {
        val userId = command.userId
        return unitOfWork.executeInTransaction(command.transactionId) {
            val deleted = regularTransactionRepository.deleteRegularTransaction(userId, RegularTransactionId(it))
            if (!deleted) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction ${command.transactionId} n'existe pas",
                    "domain.regular_transaction.delete.not_found"
                )
            }
            return@executeInTransaction success(true)
        }
    }
}
