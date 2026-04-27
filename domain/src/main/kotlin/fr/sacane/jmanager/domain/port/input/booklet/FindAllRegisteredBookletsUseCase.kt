package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.output.SessionManager
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success

data class FindAllRegisteredBookletsQuery(
    val token: SessionToken
) : Query<List<Booklet>>

@Port(Side.APPLICATION)
interface FindAllRegisteredBookletsUseCase : QueryHandler<FindAllRegisteredBookletsQuery, List<Booklet>> {
    override val queryClass get() = FindAllRegisteredBookletsQuery::class
}

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
