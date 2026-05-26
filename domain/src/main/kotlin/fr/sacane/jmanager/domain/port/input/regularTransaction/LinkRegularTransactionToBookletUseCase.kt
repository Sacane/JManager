package fr.sacane.jmanager.domain.port.input.regularTransaction

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.input.MdcContextProvider
import fr.sacane.jmanager.domain.utils.*
import java.util.UUID

data class LinkRegularTransactionToBookletCommand(
    val userId: UserId,
    val transactionId: String,
    val bookletId: UUID
) : Command<RegularTransaction>, MdcContextProvider {

    override fun mdcContext(): Map<String, String> {
        return mapOf(
            "userId" to userId.toString(),
            "transactionId" to transactionId,
            "bookletId" to bookletId.toString()
        )
    }
}

@Port(Side.APPLICATION)
interface LinkRegularTransactionToBookletUseCase : CommandHandler<LinkRegularTransactionToBookletCommand, RegularTransaction> {
    override val commandClass get() = LinkRegularTransactionToBookletCommand::class
}

@DomainService
class LinkRegularTransactionToBookletService(
    private val regularTransactionRepository: RegularTransactionRepository,
    private val unitOfWork: UnitOfWorkTransactionProvider
) : LinkRegularTransactionToBookletUseCase {

    override fun handle(
        command: LinkRegularTransactionToBookletCommand
    ): Result<RegularTransaction> {
        val userId = command.userId
        return unitOfWork.executeInTransaction(command.transactionId) {
            val existing = regularTransactionRepository.getRegularTransactionById(userId, RegularTransactionId(it))
                ?: return@executeInTransaction domainFailure(
                    ResultState.TRANSACTION_NOT_FOUND,
                    "La transaction ${command.transactionId} n'existe pas",
                    "domain.regular_transaction.link.not_found"
                )
            if (existing.associatedBooklets.any { b -> b.id == command.bookletId }) {
                return@executeInTransaction domainFailure(
                    ResultState.BAD_REQUEST,
                    "Le livret ${command.bookletId} est déjà lié à cette transaction",
                    "domain.regular_transaction.link.already_linked"
                )
            }
            val updated = regularTransactionRepository.linkBooklet(userId, RegularTransactionId(it), command.bookletId)
                ?: return@executeInTransaction domainFailure(
                    ResultState.NOT_FOUND,
                    "Le livret ${command.bookletId} est introuvable",
                    "domain.regular_transaction.link.booklet_not_found"
                )
            return@executeInTransaction success(updated)
        }
    }
}
