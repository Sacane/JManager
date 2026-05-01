package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID
import java.util.logging.Logger

data class FindTransactionByIdQuery(
    val userId: UserId,
    val id: UUID
) : Query<Transaction>

@Port(Side.APPLICATION)
interface FindTransactionByIdUseCase : QueryHandler<FindTransactionByIdQuery, Transaction> {
    override val queryClass get() = FindTransactionByIdQuery::class
}

@DomainService
class FindTransactionByIdService(
    private val transactionRepository: TransactionRepository
) : FindTransactionByIdUseCase {

    companion object {
        private val logger = Logger.getLogger(FindTransactionByIdService::class.java.name)
    }

    override fun handle(query: FindTransactionByIdQuery): Result<Transaction> {
        logger.info("Request for a transaction with id ${query.id}")
        val transaction = transactionRepository.findTransactionById(query.id)
            ?: return domainFailure(
                ResultState.TRANSACTION_NOT_FOUND,
                "La transaction ${query.id} n'existe pas",
                "domain.transaction.find.not_found"
            )
        return success(transaction)
    }
}
