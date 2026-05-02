package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.PrevisionalTransactionsOutput
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.usecase.PrevisionalTransactionFilter
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

data class GetPrevisionalTransactionsQuery(
    val userId: UserId,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val bookletId: UUID? = null
) : Query<PrevisionalTransactionsOutput>

@Port(Side.APPLICATION)
interface GetPrevisionalTransactionsUseCase : QueryHandler<GetPrevisionalTransactionsQuery, PrevisionalTransactionsOutput> {
    override val queryClass get() = GetPrevisionalTransactionsQuery::class
}

@DomainService
class GetPrevisionalTransactionsService(
    private val bookletRepository: BookletRepository,
    private val previsionalTransactionFilter: PrevisionalTransactionFilter
) : GetPrevisionalTransactionsUseCase {

    companion object {
        private val LOGGER = Logger.getLogger(GetPrevisionalTransactionsService::class.java.name)
    }

    override fun handle(query: GetPrevisionalTransactionsQuery): Result<PrevisionalTransactionsOutput> {
        val userId = query.userId
        LOGGER.info("Fetching previsional transactions from ${query.startDate} to ${query.endDate} for user $userId")

        if (query.startDate.isAfter(query.endDate)) {
            return statsDomainFailure(
                ResultState.INVALID,
                "La date de début doit être antérieure à la date de fin",
                "domain.stats.previsional.invalid_date_range"
            )
        }

        return withScopedBooklets(bookletRepository, userId, query.bookletId) { scopedBooklets ->
            val result = previsionalTransactionFilter.filterPrevisionalTransactions(
                booklets = scopedBooklets,
                startDate = query.startDate,
                endDate = query.endDate
            )

            LOGGER.info(
                """
                Previsional transactions fetched:
                - Total transactions: ${result.transactions.size}
                - Total amount: ${result.totalAmount}
                - Total income: ${result.totalIncome}
                - Total expenses: ${result.totalExpenses}
                """.trimIndent()
            )

            success(
                PrevisionalTransactionsOutput(
                    transactions = result.transactions,
                    groupedByBooklet = result.groupedByBooklet,
                    totalAmount = result.totalAmount,
                    totalIncome = result.totalIncome,
                    totalExpenses = result.totalExpenses,
                    regularTransactions = result.regularTransactions,
                    nonRegularTransactions = result.nonRegularTransactions,
                    totalRegularAmount = result.totalRegularAmount,
                    totalNonRegularAmount = result.totalNonRegularAmount,
                    startDate = query.startDate,
                    endDate = query.endDate
                )
            )
        }
    }
}
