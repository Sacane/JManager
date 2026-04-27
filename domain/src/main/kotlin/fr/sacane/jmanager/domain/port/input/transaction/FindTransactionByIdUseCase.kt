package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.roleUser
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID
import java.util.logging.Logger

data class FindTransactionByIdQuery(
    val token: SessionToken,
    val id: UUID
) : Query<Transaction>

@Port(Side.APPLICATION)
interface FindTransactionByIdUseCase : QueryHandler<FindTransactionByIdQuery, Transaction> {
    override val queryClass get() = FindTransactionByIdQuery::class
}

@DomainService
class FindTransactionByIdService(
    private val transactionRepository: TransactionRepository,
    private val session: SessionManager
) : FindTransactionByIdUseCase {

    companion object {
        private val logger = Logger.getLogger(FindTransactionByIdService::class.java.name)
    }

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(query: FindTransactionByIdQuery): Result<Transaction> = session.authenticate(query.token, roleUser) {
        logger.info("Request for a transaction with id ${query.id}")
        val transaction = transactionRepository.findTransactionById(query.id)
            ?: return@authenticate domainFailure(
                ResultState.TRANSACTION_NOT_FOUND,
                "La transaction ${query.id} n'existe pas",
                "domain.transaction.find.not_found"
            )
        success(transaction)
    }
}
