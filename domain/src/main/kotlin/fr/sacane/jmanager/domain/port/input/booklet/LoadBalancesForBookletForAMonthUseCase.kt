package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.BookletBalances
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletBalanceQueryRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionQueryRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

data class LoadBalancesForBookletForAMonthQuery(
    val token: SessionToken,
    val bookletId: UUID,
    val month: Month,
    val year: Int,
    val startingMonth: Month? = null,
    val startingYear: Int? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
) : Query<BookletBalances>

@Port(Side.APPLICATION)
interface LoadBalancesForBookletForAMonthUseCase : QueryHandler<LoadBalancesForBookletForAMonthQuery, BookletBalances> {
    override val queryClass get() = LoadBalancesForBookletForAMonthQuery::class
}

@DomainService
class LoadBalancesForBookletForAMonthService(
    private val session: SessionManager,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProvider,
    private val transactionQueryRepository: TransactionQueryRepository,
    private val bookletBalanceQueryRepository: BookletBalanceQueryRepository,
    private val regularTransactionGeneratorService: RegularTransactionGenerator
) : LoadBalancesForBookletForAMonthUseCase {

    override fun handle(query: LoadBalancesForBookletForAMonthQuery): Result<BookletBalances> {
        val (token, bookletId, month, year, startingMonth, startingYear, startDate, endDate) = query
        return session.authenticate(token) { userId ->
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
                val persisted = bookletBalanceQueryRepository.findPersistedBalances(bookletId)
                    ?: return@executeInTransaction bookletDomainFailure(
                        ResultState.BOOKLET_NOT_FOUND,
                        "Requested booklet is not registered",
                        "domain.booklet.load_balances.not_found"
                    )

                val currentDate = LocalDate.now()
                val currentMonth = startingMonth ?: currentDate.month
                val currentYear = startingYear ?: currentDate.year

                if ((startDate == null) != (endDate == null)) {
                    return@executeInTransaction bookletDomainFailure(
                        ResultState.BAD_REQUEST,
                        "startDate and endDate must be both defined or both omitted",
                        "domain.booklet.load_balances.invalid_date_range"
                    )
                }

                if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                    return@executeInTransaction bookletDomainFailure(
                        ResultState.BAD_REQUEST,
                        "startDate cannot be after endDate",
                        "domain.booklet.load_balances.invalid_date_range"
                    )
                }

                val regularTransactions = regularTransactionRepository.getAllRegularUsedByBooklet(userId, bookletId)
                    ?: emptyList()

                val baseBooklet = Booklet(
                    amount = Amount(persisted.amount),
                    label = persisted.label,
                    id = bookletId
                )

                val (from, to) = if (startDate != null && endDate != null) {
                    startDate to endDate
                } else {
                    val previewStart = YearMonth.of(currentYear, currentMonth)
                    val previewEnd = YearMonth.of(year, month)
                    monthDateBounds(previewStart, previewEnd)
                }
                val allPhysicalTransactionsInRange = transactionQueryRepository
                    .findByBookletIdAndDateBetween(bookletId, from, to)

                val physicalPreviewTransactions = allPhysicalTransactionsInRange.filter { it.isPreview }
                physicalPreviewTransactions.forEach { baseBooklet.addTransaction(it) }

                val previsionalSold = calculatePrevisionalSold(
                    regularTransactionGeneratorService,
                    baseBooklet,
                    regularTransactions,
                    from,
                    to,
                    allPhysicalTransactionsForDedup = allPhysicalTransactionsInRange
                )

                return@executeInTransaction success(
                    BookletBalances(
                        label = persisted.label,
                        realSold = Amount(persisted.amount),
                        previewSold = previsionalSold
                    )
                )
            }
        }
    }
}
