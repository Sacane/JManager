package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.utils.*

@DomainService
class UpdateRegularTransactionService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val session: SessionManager,
    private val unitOfWork: UnitOfWorkTransactionProvider
) : UpdateRegularTransactionUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(
        command: UpdateRegularTransactionCommand
    ): Result<RegularTransaction> = session.authenticate(command.token) { userId ->
        return@authenticate unitOfWork.executeInTransaction(command.regularTransaction) {
            val updated = regularTransactionRepository.updateRegularTransaction(userId, it, command.bookletIds)
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction ${it.id} n'existe pas",
                    "domain.regular_transaction.update.not_found"
                )
            return@executeInTransaction success(updated)
        }
    }
}
