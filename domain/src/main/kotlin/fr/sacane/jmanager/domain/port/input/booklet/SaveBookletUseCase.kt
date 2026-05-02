package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.port.output.repository.BookletRepository
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success

data class SaveBookletCommand(
    val userId: UserId,
    val booklet: Booklet
) : Command<Booklet>

@Port(Side.APPLICATION)
interface SaveBookletUseCase : CommandHandler<SaveBookletCommand, Booklet> {
    override val commandClass get() = SaveBookletCommand::class
}

@DomainService
class SaveBookletService(
    private val userRepository: UserRepository,
    private val bookletRepository: BookletRepository
) : SaveBookletUseCase {
    override fun handle(command: SaveBookletCommand): Result<Booklet> {
        val userId = command.userId
        val booklet = command.booklet
        val user = userRepository.findUserByIdWithBooklets(userId)
            ?: return bookletDomainFailure(
                ResultState.USER_NOT_FOUND,
                "L'utilisateur n'existe pas en base",
                "domain.booklet.save.user_not_found"
            )
        if(user.hasBooklet(booklet.label)) {
            return bookletDomainFailure(
                ResultState.BOOKLET_LABEL_EXIST,
                "Le profil contient déjà un compte avec le label ${booklet.label}",
                "domain.booklet.save.label_already_exists"
            )
        }
        if (user.booklets.size >= 6) {
            return bookletDomainFailure(
                ResultState.BOOKLET_MAXIMUM_SIZE_REACHED,
                "Le profil ne peut pas contenir plus de 6 comptes",
                "domain.booklet.save.maximum_size_reached"
            )
        }
        val bookletSaved = bookletRepository.save(userId, booklet)
            ?: return bookletDomainFailure(
                ResultState.INFRASTRUCTURE_ERROR,
                "Erreur lors de la sauvegarde du compte",
                "domain.booklet.save.infrastructure_error"
            )
        return success(bookletSaved)
    }
}
