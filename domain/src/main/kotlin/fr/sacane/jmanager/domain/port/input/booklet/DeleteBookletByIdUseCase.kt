package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID

data class DeleteBookletByIdCommand(
    val bookletId: UUID,
    val token: SessionToken
)

@Port(Side.APPLICATION)
interface DeleteBookletByIdUseCase {
    fun handle(command: DeleteBookletByIdCommand): Result<Nothing>
}

@DomainService
class DeleteBookletByIdService(
    private val session: SessionManager,
    private val bookletRepository: BookletRepository,
    private val unitOfWorkTransactionProviderPort: UnitOfWorkTransactionProvider,
    private val trackerRepository: RegularTransactionTrackerRepository
) : DeleteBookletByIdUseCase {
    override fun handle(command: DeleteBookletByIdCommand): Result<Nothing> = session.authenticate(command.token) { userId ->
        val bookletId = command.bookletId
        return@authenticate unitOfWorkTransactionProviderPort.executeInTransaction(Unit) {
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
            return@executeInTransaction success()
        }
    }
}
