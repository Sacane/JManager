package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.utils.*

data class DeleteRegularTransactionsCommand(
    val token: SessionToken,
    val transactionIds: List<String>
)

@Port(Side.APPLICATION)
interface DeleteRegularTransactionsUseCase {
    fun handle(command: DeleteRegularTransactionsCommand): Result<List<String>>
}

@DomainService
class DeleteRegularTransactionsService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val session: SessionManager,
    private val unitOfWork: UnitOfWorkTransactionProvider
) : DeleteRegularTransactionsUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(
        command: DeleteRegularTransactionsCommand
    ): Result<List<String>> = session.authenticate(command.token) { userId ->
        if (command.transactionIds.isEmpty()) {
            return@authenticate domainFailure(
                ResultState.TRANSACTION_ENTRY_ERROR,
                "Aucune transaction régulière à supprimer",
                "domain.regular_transaction.delete.bulk.empty_selection"
            )
        }

        val distinctIds = command.transactionIds.distinct()
        return@authenticate unitOfWork.executeInTransaction(distinctIds) { ids ->
            val missingId = ids.firstOrNull {
                regularTransactionRepository.getRegularTransactionById(userId, RegularTransactionId(it)) == null
            }

            if (missingId != null) {
                return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction $missingId n'existe pas",
                    "domain.regular_transaction.delete.bulk.not_found"
                )
            }

            ids.forEach {
                regularTransactionRepository.deleteRegularTransaction(userId, RegularTransactionId(it))
            }

            return@executeInTransaction success(ids)
        }
    }
}
