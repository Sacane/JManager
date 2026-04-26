package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success

@DomainService
class FindAllRegisteredBookletsService(
    private val session: SessionManager,
    private val userRepository: UserRepository
) : FindAllRegisteredBookletsUseCase {
    override fun handle(query: FindAllRegisteredBookletsQuery): Result<List<Booklet>> = session.authenticate(query.token) {
        val user = userRepository.findUserByIdWithBooklets(it)
            ?: return@authenticate bookletDomainFailure(
                ResultState.BOOKLET_NOT_FOUND,
                "L'utilisateur n'existe pas en base",
                "domain.booklet.find_all.user_not_found"
            )
        return@authenticate success(user.booklets)
    }
}
