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
import java.time.Month

data class RetrieveTransactionsByMonthAndYearQuery(
    val userId: UserId,
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
    private val transactionRepository: TransactionRepository
) : RetrieveTransactionsByMonthAndYearUseCase {

    override fun handle(query: RetrieveTransactionsByMonthAndYearQuery): Result<List<Transaction>> {
        return success(transactionRepository.findBookletByLabelWithTransactions(query.bookletLabel, query.userId)?.retrieveTransactionsSortedByDate(query.month, query.year)
            ?: return domainFailure(
                state = ResultState.BOOKLET_NOT_FOUND,
                "Aucun compte ne correspond au label indiqué",
                "domain.transaction.retrieve.booklet_not_found"
            )
        )
    }
}
