package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionQueryRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.RegularTransactionGenerator
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.port.input.MdcKeys
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

/**
 * Restores the deleted occurrences the user explicitly selected.
 *
 * @property regularTransactionIds The regular transactions the user chose to restore. Only those are
 *           un-excluded and regenerated; identifiers that are not currently excluded for the target
 *           month are ignored. Must not be empty — restoring nothing is not a meaningful request.
 */
data class RegenerateDeletedPrevisionalTransactionsCommand(
    val userId: UserId,
    val bookletId: UUID,
    val month: Month,
    val year: Int,
    val regularTransactionIds: List<RegularTransactionId>
) : Command<List<Transaction>>, MdcContextProvider {
    override fun mdcContext() = mapOf(MdcKeys.BOOKLET_ID to bookletId.toString())
}

@Port(Side.APPLICATION)
interface RegenerateDeletedPrevisionalTransactionsUseCase : CommandHandler<RegenerateDeletedPrevisionalTransactionsCommand, List<Transaction>> {
    override val commandClass get() = RegenerateDeletedPrevisionalTransactionsCommand::class
}

@DomainService
class RegenerateDeletedPrevisionalTransactionsService(
    private val bookletRepository: BookletRepository,
    private val regularTransactionRepository: RegularTransactionRepository,
    private val regularTransactionGeneratorService: RegularTransactionGenerator,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository,
    private val transactionQueryRepository: TransactionQueryRepository
) : RegenerateDeletedPrevisionalTransactionsUseCase {

    override fun handle(command: RegenerateDeletedPrevisionalTransactionsCommand): Result<List<Transaction>> {
        val (userId, bookletId, month, year, selectedIds) = command
        return unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            // A booklet owned by someone else is reported as missing rather than forbidden: telling the
            // caller it exists would turn this endpoint into a booklet-enumeration oracle.
            if (!userOwnsBooklet(bookletRepository, userId, bookletId)) {
                return@executeInTransaction bookletDomainFailure(
                    ResultState.BOOKLET_NOT_FOUND,
                    "Requested booklet is not registered",
                    "domain.booklet.regenerate.not_found"
                )
            }
            if (selectedIds.isEmpty()) {
                return@executeInTransaction bookletDomainFailure(
                    ResultState.TRANSACTION_ENTRY_ERROR,
                    "No transaction selected for regeneration",
                    "domain.booklet.regenerate.empty_selection"
                )
            }
            val currentYearMonth = YearMonth.now()
            val targetYearMonth = YearMonth.of(year, month)

            if (targetYearMonth.isBefore(currentYearMonth)) {
                return@executeInTransaction success(emptyList<Transaction>())
            }

            // Only the selected regular transactions that were actually deleted for that month are
            // restored: a selection referencing a non-excluded one must never materialise occurrences
            // the user did not delete.
            val selection = selectedIds.toSet()
            val regularTransactions = regularTransactionRepository.getAllRegularUsedByBooklet(userId, bookletId)
                .orEmpty()
                .filter { it.id in selection && trackerRepository.excludesMonth(it.id, bookletId, targetYearMonth) }

            if (regularTransactions.isEmpty()) {
                return@executeInTransaction success(emptyList<Transaction>())
            }

            regularTransactions.forEach { rt ->
                trackerRepository.unmarkMonthAsExcluded(rt.id, bookletId, year, month)
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
