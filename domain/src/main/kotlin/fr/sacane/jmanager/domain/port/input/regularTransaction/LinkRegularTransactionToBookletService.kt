package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.utils.*

@DomainService
class LinkRegularTransactionToBookletService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val session: SessionManager,
    private val unitOfWork: UnitOfWorkTransactionProvider
) : LinkRegularTransactionToBookletUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(
        command: LinkRegularTransactionToBookletCommand
    ): Result<RegularTransaction> = session.authenticate(command.token) { userId ->
        return@authenticate unitOfWork.executeInTransaction(command.transactionId) {
            val existing = regularTransactionRepository.getRegularTransactionById(userId, RegularTransactionId(it))
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction ${command.transactionId} n'existe pas",
                    "domain.regular_transaction.link.not_found"
                )
            if (existing.associatedBooklets.any { b -> b.id == command.bookletId }) {
                return@executeInTransaction domainFailure(
                    ResultState.BAD_REQUEST,
                    "Le livret ${command.bookletId} est déjà lié à cette transaction",
                    "domain.regular_transaction.link.already_linked"
                )
            }
            val updated = regularTransactionRepository.linkBooklet(userId, RegularTransactionId(it), command.bookletId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.NOT_FOUND,
                    "Le livret ${command.bookletId} est introuvable",
                    "domain.regular_transaction.link.booklet_not_found"
                )
            return@executeInTransaction success(updated)
        }
    }
}
