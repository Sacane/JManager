package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionQueryRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.time.LocalDate
import java.time.YearMonth

@DomainService
class RegenerateDeletedPrevisionalTransactionsService(
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val regularTransactionGeneratorService: RegularTransactionGenerator,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository,
    private val transactionQueryRepository: TransactionQueryRepository
) : RegenerateDeletedPrevisionalTransactionsUseCase {

    override fun handle(command: RegenerateDeletedPrevisionalTransactionsCommand): Result<List<Transaction>> {
        val (token, bookletId, month, year) = command
        return session.authenticate(token) { userId ->
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            bookletRepository.findBookletByIdWithTransactions(bookletId)
                ?: return@executeInTransaction bookletDomainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Requested booklet is not registered",
                    "domain.booklet.regenerate.not_found"
                )
            val currentYearMonth = YearMonth.now()
            val targetYearMonth = YearMonth.of(year, month)

            if (targetYearMonth.isBefore(currentYearMonth)) {
                return@executeInTransaction success(emptyList<Transaction>())
            }

            val regularTransactions = regularTransactionRepository.getAllRegularUsedByBooklet(userId, bookletId)
                ?: emptyList()
            regularTransactions.forEach { rt ->
                val tracker = trackerRepository.findTracker(rt.id, bookletId)
                if (tracker?.excludedMonths?.contains(targetYearMonth) == true) {
                    trackerRepository.unmarkMonthAsExcluded(rt.id, bookletId, year, month)
                }
            }

            if (targetYearMonth == currentYearMonth) {
                val regenerated = regularTransactionGeneratorService.generateMissingPrevisionalTransactions(
                    bookletId,
                    regularTransactions,
                    month,
                    year
                )
                return@executeInTransaction success(regenerated)
            } else {
                val rangeStart = LocalDate.of(year, month, 1)
                val rangeEnd = YearMonth.of(year, month).atEndOfMonth()
                val existingPhysicalTransactions = transactionQueryRepository.findByBookletIdAndDateBetween(bookletId, rangeStart, rangeEnd)
                val virtual = regularTransactionGeneratorService.calculateVirtualTransactions(
                    bookletId = bookletId,
                    regularTransactions = regularTransactions,
                    startMonth = month,
                    startYear = year,
                    endMonth = month,
                    endYear = year,
                    existingPhysicalTransactions = existingPhysicalTransactions
                )
                return@executeInTransaction success(virtual)
            }
        }
    }
    }
}
