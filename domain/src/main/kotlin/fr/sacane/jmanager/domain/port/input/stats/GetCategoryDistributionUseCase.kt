package fr.sacane.jmanager.domain.port.input.stats

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.CategoryDistributionOutput
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.usecase.CategoryDistributionCalculator
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.time.LocalDate
import java.util.UUID
import java.util.logging.Logger

data class GetCategoryDistributionQuery(
    val token: SessionToken,
    val bookletId: UUID? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
) : Query<CategoryDistributionOutput>

@Port(Side.APPLICATION)
interface GetCategoryDistributionUseCase : QueryHandler<GetCategoryDistributionQuery, CategoryDistributionOutput> {
    override val queryClass get() = GetCategoryDistributionQuery::class
}

@DomainService
class GetCategoryDistributionService(
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val categoryDistributionCalculator: CategoryDistributionCalculator
) : GetCategoryDistributionUseCase {

    companion object {
        private val LOGGER = Logger.getLogger(GetCategoryDistributionService::class.java.name)
    }

    override fun handle(query: GetCategoryDistributionQuery): Result<CategoryDistributionOutput> = session.authenticate(query.token) { userId ->
        LOGGER.info("Fetching category distribution for user $userId")

        validateDateRange(query.startDate, query.endDate, "domain.stats.category_distribution")?.let { (detail, key) ->
            return@authenticate statsDomainFailure(ResultState.INVALID, detail, key)
        }

        withScopedBooklets(bookletRepository, userId, query.bookletId) { scopedBooklets ->
            val allTransactions = scopedBooklets.flatMap { booklet ->
                booklet.transactions
            }

            LOGGER.info("All transactions fetched: ${allTransactions.size} transactions found")

            val (categories, totalExpenses) = categoryDistributionCalculator.calculateDistribution(
                transactions = allTransactions,
                startDate = query.startDate,
                endDate = query.endDate
            )

            LOGGER.info("Category distribution calculated: ${categories.size} categories found")

            success(
                CategoryDistributionOutput(
                    categories = categories,
                    totalExpenses = totalExpenses
                )
            )
        }
    }
}
