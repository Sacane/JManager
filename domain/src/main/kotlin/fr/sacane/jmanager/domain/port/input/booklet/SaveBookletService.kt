package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.port.spi.repository.BookletRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success

@DomainService
class SaveBookletService(
    private val session: SessionManager,
    private val userRepository: UserRepository,
    private val bookletRepository: BookletRepository
) : SaveBookletUseCase {
    override fun handle(command: SaveBookletCommand): Result<Booklet> = session.authenticate(command.token) {
        val booklet = command.booklet
        val user = userRepository.findUserByIdWithBooklets(it)
            ?: return@authenticate bookletDomainFailure(
                ResultState.USER_NOT_FOUND,
                "L'utilisateur n'existe pas en base",
                "domain.booklet.save.user_not_found"
            )
        if(user.hasBooklet(booklet.label)) {
            return@authenticate bookletDomainFailure(
                ResultState.BOOKLET_LABEL_EXIST,
                "Le profil contient déjà un compte avec le label ${booklet.label}",
                "domain.booklet.save.label_already_exists"
            )
        }
        if (user.booklets.size >= 6) {
            return@authenticate bookletDomainFailure(
                ResultState.BOOKLET_MAXIMUM_SIZE_REACHED,
                "Le profil ne peut pas contenir plus de 6 comptes",
                "domain.booklet.save.maximum_size_reached"
            )
        }
        val bookletSaved = bookletRepository.save(it, booklet)
            ?: return@authenticate bookletDomainFailure(
                ResultState.INFRASTRUCTURE_ERROR,
                "Erreur lors de la sauvegarde du compte",
                "domain.booklet.save.infrastructure_error"
            )
        success(bookletSaved)
    }
}
