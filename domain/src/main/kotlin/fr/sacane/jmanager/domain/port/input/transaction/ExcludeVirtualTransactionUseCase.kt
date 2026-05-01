package fr.sacane.jmanager.domain.port.input.transaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.utils.*
import java.time.Month
import java.util.UUID

data class ExcludeVirtualTransactionCommand(
    val userId: UserId,
    val bookletId: UUID,
    val regularTransactionId: RegularTransactionId,
    val month: Month,
    val year: Int
) : Command<Unit>

@Port(Side.APPLICATION)
interface ExcludeVirtualTransactionUseCase : CommandHandler<ExcludeVirtualTransactionCommand, Unit> {
    override val commandClass get() = ExcludeVirtualTransactionCommand::class
}

@DomainService
class ExcludeVirtualTransactionService(
    private val bookletRepository: BookletRepository,
    private val trackerRepository: RegularTransactionTrackerRepository
) : ExcludeVirtualTransactionUseCase {

    override fun handle(command: ExcludeVirtualTransactionCommand): Result<Unit> {
        val booklet = bookletRepository.findBookletByIdWithTransactions(command.bookletId)
            ?: return domainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "Booklet ${command.bookletId} n'existe pas",
                "domain.virtual_transaction.exclude.booklet_not_found"
            )

        trackerRepository.markMonthAsExcluded(
            regularTransactionId = command.regularTransactionId,
            bookletId = command.bookletId,
            year = command.year,
            month = command.month
        )

        return success(Unit)
    }
}
