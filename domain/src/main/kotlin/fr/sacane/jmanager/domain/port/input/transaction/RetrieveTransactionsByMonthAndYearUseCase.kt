package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.*
import java.time.Month

data class RetrieveTransactionsByMonthAndYearQuery(
    val token: SessionToken,
    val month: Month,
    val year: Int,
    val bookletLabel: String
) : Query<List<Transaction>>

@Port(Side.APPLICATION)
interface RetrieveTransactionsByMonthAndYearUseCase : QueryHandler<RetrieveTransactionsByMonthAndYearQuery, List<Transaction>> {
    override val queryClass get() = RetrieveTransactionsByMonthAndYearQuery::class
}

@DomainService
class RetrieveTransactionsByMonthAndYearService(
    private val transactionRepository: TransactionRepository,
    private val session: SessionManager
) : RetrieveTransactionsByMonthAndYearUseCase {

    override fun handle(query: RetrieveTransactionsByMonthAndYearQuery): Result<List<Transaction>> = session.authenticate(query.token) {
        success(transactionRepository.findBookletByLabelWithTransactions(query.bookletLabel, it)?.retrieveTransactionsSortedByDate(query.month, query.year)
            ?: return@authenticate domainFailure(
                state = ResultState.BOOKLET_NOT_FOUND,
                "Aucun compte ne correspond au label indiqué",
                "domain.transaction.retrieve.booklet_not_found"
            )
        )
    }
}
