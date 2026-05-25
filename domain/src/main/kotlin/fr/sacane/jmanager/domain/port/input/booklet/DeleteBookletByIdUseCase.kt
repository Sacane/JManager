package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID
import org.slf4j.LoggerFactory

data class DeleteBookletByIdCommand(
    val bookletId: UUID,
    val userId: UserId
) : Command<Nothing>

@Port(Side.APPLICATION)
interface DeleteBookletByIdUseCase : CommandHandler<DeleteBookletByIdCommand, Nothing> {
    override val commandClass get() = DeleteBookletByIdCommand::class
}

@DomainService
class DeleteBookletByIdService(
    private val bookletRepository: BookletRepository,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository
) : DeleteBookletByIdUseCase {

    companion object {
        private val log = LoggerFactory.getLogger(DeleteBookletByIdService::class.java)
    }

    override fun handle(command: DeleteBookletByIdCommand): Result<Nothing> {
        val userId = command.userId
        val bookletId = command.bookletId
        return unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
            if(bookletRepository.findBookletByIdWithTransactions(bookletId) == null){
                return@executeInTransaction bookletDomainFailure(
                    ResultState.NOT_FOUND,
                    "Le livret $bookletId n'existe pas",
                    "domain.booklet.delete.not_found"
                )
            }
            if (!userOwnsBooklet(bookletRepository, userId, bookletId)) {
                return@executeInTransaction bookletDomainFailure(
                    ResultState.FORBIDDEN,
                    "Vous n'avez pas accès à ce livret",
                    "domain.booklet.delete.forbidden"
                )
            }
            bookletRepository.deleteBookletById(bookletId)
            trackerRepository.deleteTrackerByBookletId(bookletId)
            log.info("Booklet deleted: id={}", bookletId)
            return@executeInTransaction success()
        }
    }
}
