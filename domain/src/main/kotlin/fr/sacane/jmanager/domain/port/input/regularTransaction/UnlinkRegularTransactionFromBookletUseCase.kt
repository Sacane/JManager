package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID

data class UnlinkRegularTransactionFromBookletCommand(
    val token: SessionToken,
    val transactionId: String,
    val bookletId: UUID
) : Command<RegularTransaction>

@Port(Side.APPLICATION)
interface UnlinkRegularTransactionFromBookletUseCase : CommandHandler<UnlinkRegularTransactionFromBookletCommand, RegularTransaction> {
    override val commandClass get() = UnlinkRegularTransactionFromBookletCommand::class
}

@DomainService
class UnlinkRegularTransactionFromBookletService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val session: SessionManager,
    private val unitOfWork: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository
) : UnlinkRegularTransactionFromBookletUseCase {

    override fun handle(
        command: UnlinkRegularTransactionFromBookletCommand
    ): Result<RegularTransaction> = session.authenticate(command.token) { userId ->
        return@authenticate unitOfWork.executeInTransaction(command.transactionId) {
            val existing = regularTransactionRepository.getRegularTransactionById(userId, RegularTransactionId(it))
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction ${command.transactionId} n'existe pas",
                    "domain.regular_transaction.unlink.not_found"
                )
            if (existing.associatedBooklets.none { b -> b.id == command.bookletId }) {
                return@executeInTransaction domainFailure(
                    ResultState.BAD_REQUEST,
                    "Le livret ${command.bookletId} n'est pas lié à cette transaction",
                    "domain.regular_transaction.unlink.not_linked"
                )
            }
            val updated = regularTransactionRepository.unlinkBooklet(userId, RegularTransactionId(it), command.bookletId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.NOT_FOUND,
                    "Le livret ${command.bookletId} est introuvable",
                    "domain.regular_transaction.unlink.booklet_not_found"
                )
            trackerRepository.deleteTrackerByPair(RegularTransactionId(it), command.bookletId)
            return@executeInTransaction success(updated)
        }
    }
}
