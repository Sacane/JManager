package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.port.input.MdcKeys
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID
import org.slf4j.LoggerFactory

data class FindTransactionByIdQuery(
    val userId: UserId,
    val id: UUID
) : Query<Transaction>, MdcContextProvider {
    override fun mdcContext() = mapOf(MdcKeys.TRANSACTION_ID to id.toString())
}

@Port(Side.APPLICATION)
interface FindTransactionByIdUseCase : QueryHandler<FindTransactionByIdQuery, Transaction> {
    override val queryClass get() = FindTransactionByIdQuery::class
}

@DomainService
class FindTransactionByIdService(
    private val transactionRepository: TransactionRepository
) : FindTransactionByIdUseCase {

    companion object {
        private val log = LoggerFactory.getLogger(FindTransactionByIdService::class.java)
    }

    override fun handle(query: FindTransactionByIdQuery): Result<Transaction> {
        val transaction = transactionRepository.findTransactionById(query.id)
            ?: return domainFailure(
                ResultState.TRANSACTION_NOT_FOUND,
                "La transaction ${query.id} n'existe pas",
                "domain.transaction.find.not_found"
            )
        return success(transaction)
    }
}
