package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.inDisplayOrder
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import java.util.UUID

/**
 * Puts the user's booklets in the order of [bookletIds]. Booklets left out, such as one created after the
 * screen was opened, follow in their previous order.
 */
data class ReorderBookletsCommand(val userId: UserId, val bookletIds: List<UUID>) : Command<Unit>

@Port(Side.APPLICATION)
interface ReorderBookletsUseCase : CommandHandler<ReorderBookletsCommand, Unit> {
    override val commandClass get() = ReorderBookletsCommand::class
}

@DomainService
class ReorderBookletsService(
    private val bookletRepository: BookletRepository,
) : ReorderBookletsUseCase {

    override fun handle(command: ReorderBookletsCommand): Result<Unit> {
        if (command.bookletIds.isEmpty()) {
            return bookletDomainFailure(ResultState.INVALID, "Aucun compte à ordonner", "domain.booklet.order.empty")
        }
        if (command.bookletIds.toSet().size != command.bookletIds.size) {
            return bookletDomainFailure(ResultState.INVALID, "Un compte apparaît deux fois", "domain.booklet.order.duplicate")
        }
        val owned = bookletRepository.findBookletsForUser(command.userId).inDisplayOrder().mapNotNull { it.id }
        // An unknown booklet answers like someone else's: whether it exists is none of the caller's business.
        if (!owned.containsAll(command.bookletIds)) {
            return bookletDomainFailure(ResultState.FORBIDDEN, "Vous n'avez pas accès à l'un de ces comptes", "domain.booklet.order.forbidden")
        }

        val order = command.bookletIds + (owned - command.bookletIds.toSet())
        if (!bookletRepository.updateDisplayPositions(order)) {
            return bookletDomainFailure(ResultState.INFRASTRUCTURE_ERROR, "Impossible d'enregistrer l'ordre des comptes", "domain.booklet.order.update_failed")
        }
        return success(Unit)
    }
}
