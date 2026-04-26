package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.utils.*

data class GetRegularTransactionByIdQuery(
    val token: SessionToken,
    val transactionId: String
)

@Port(Side.APPLICATION)
interface GetRegularTransactionByIdUseCase {
    fun handle(query: GetRegularTransactionByIdQuery): Result<RegularTransaction>
}

@DomainService
class GetRegularTransactionByIdService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val session: SessionManager
) : GetRegularTransactionByIdUseCase {

    private fun <S> domainFailure(state: ResultState, detail: String, key: String): Result<S> {
        return failure(state, DomainError(state.code, key, detail))
    }

    override fun handle(
        query: GetRegularTransactionByIdQuery
    ): Result<RegularTransaction> = session.authenticate(query.token) {
        val result = regularTransactionRepository.getRegularTransactionById(
            it,
            RegularTransactionId(query.transactionId)
        ) ?: return@authenticate domainFailure(
            ResultState.TRANSACTION_NOT_FOUND,
            "La transaction ${query.transactionId} n'existe pas",
            "domain.regular_transaction.get_by_id.not_found"
        )
        return@authenticate success(result)
    }
}
