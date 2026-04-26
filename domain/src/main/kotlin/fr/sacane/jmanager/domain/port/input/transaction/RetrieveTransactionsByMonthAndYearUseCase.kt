package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import fr.sacane.jmanager.domain.utils.*
import java.time.Month

data class RetrieveTransactionsByMonthAndYearQuery(
    val token: SessionToken,
    val month: Month,
    val year: Int,
    val bookletLabel: String
)

@Port(Side.APPLICATION)
interface RetrieveTransactionsByMonthAndYearUseCase {
    fun handle(query: RetrieveTransactionsByMonthAndYearQuery): Result<List<Transaction>>
}

@DomainService
class RetrieveTransactionsByMonthAndYearService(
    private val transactionRepository: TransactionRepository,
    private val session: SessionManager
) : RetrieveTransactionsByMonthAndYearUseCase {

    private fun <S> domainNotFound(detail: String, key: String): Result<S> {
        return failure(ResultState.NOT_FOUND, DomainError(ResultState.NOT_FOUND.code, key, detail))
    }

    override fun handle(query: RetrieveTransactionsByMonthAndYearQuery): Result<List<Transaction>> = session.authenticate(query.token) {
        success(transactionRepository.findBookletByLabelWithTransactions(query.bookletLabel, it)?.retrieveTransactionsSortedByDate(query.month, query.year)
            ?: return@authenticate domainNotFound(
                "Aucun compte ne correspond au label indiqué",
                "domain.transaction.retrieve.booklet_not_found"
            )
        )
    }
}
