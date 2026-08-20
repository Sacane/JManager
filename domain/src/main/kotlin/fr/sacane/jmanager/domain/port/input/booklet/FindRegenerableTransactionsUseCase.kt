package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegenerableTransaction
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.port.input.MdcKeys
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.time.Month
import java.time.YearMonth
import java.util.UUID

data class FindRegenerableTransactionsQuery(
    val userId: UserId,
    val bookletId: UUID,
    val month: Month,
    val year: Int
) : Query<List<RegenerableTransaction>>, MdcContextProvider {
    override fun mdcContext() = mapOf(MdcKeys.BOOKLET_ID to bookletId.toString())
}

@Port(Side.APPLICATION)
interface FindRegenerableTransactionsUseCase : QueryHandler<FindRegenerableTransactionsQuery, List<RegenerableTransaction>> {
    override val queryClass get() = FindRegenerableTransactionsQuery::class
}

/**
 * Lists the occurrences a user could restore for a given booklet and month, without restoring anything.
 *
 * Only regular transactions whose target month is currently marked as excluded are considered: those
 * are exactly the ones the user deleted. Their occurrences are recomputed on-the-fly so the caller can
 * display the label, amount and date of what would come back.
 */
@DomainService
class FindRegenerableTransactionsService(
    private val bookletRepository: BookletRepository,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val trackerRepository: RegularTransactionTrackerRepository,
    private val regularTransactionGenerator: RegularTransactionGenerator
) : FindRegenerableTransactionsUseCase {

    override fun handle(query: FindRegenerableTransactionsQuery): Result<List<RegenerableTransaction>> {
        val (userId, bookletId, month, year) = query
        // A booklet owned by someone else is reported as missing rather than forbidden: telling the
        // caller it exists would turn this endpoint into a booklet-enumeration oracle.
        if (!userOwnsBooklet(bookletRepository, userId, bookletId)) {
            return bookletDomainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "Requested booklet is not registered",
                "domain.booklet.regenerable.not_found"
            )
        }

        val targetYearMonth = YearMonth.of(year, month)
        if (targetYearMonth.isBefore(YearMonth.now())) {
            return success(emptyList())
        }

        val excludedRegularTransactions = regularTransactionRepository
            .getAllRegularUsedByBooklet(userId, bookletId)
            .orEmpty()
            .filter { trackerRepository.excludesMonth(it.id, bookletId, targetYearMonth) }

        if (excludedRegularTransactions.isEmpty()) {
            return success(emptyList())
        }

        val occurrences = regularTransactionGenerator.calculateOccurrencesIgnoringExclusions(
            regularTransactions = excludedRegularTransactions,
            targetMonth = month,
            targetYear = year
        )
        return success(occurrences.mapNotNull { it.toRegenerableTransaction() })
    }

    private fun Transaction.toRegenerableTransaction(): RegenerableTransaction? =
        regularTransactionId?.let {
            RegenerableTransaction(
                regularTransactionId = it,
                label = label,
                amount = amount,
                isIncome = isIncome,
                tag = tag,
                date = date
            )
        }
}
