package fr.sacane.jmanager.domain.port.input.booklet

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success

data class FindByLabelAndUserIdQuery(
    val token: SessionToken,
    val label: String
) : Query<Booklet>

@Port(Side.APPLICATION)
interface FindByLabelAndUserIdUseCase : QueryHandler<FindByLabelAndUserIdQuery, Booklet> {
    override val queryClass get() = FindByLabelAndUserIdQuery::class
}

@DomainService
class FindByLabelAndUserIdService(
    private val session: SessionManager,
    private val userRepository: UserRepository
) : FindByLabelAndUserIdUseCase {
    override fun handle(query: FindByLabelAndUserIdQuery): Result<Booklet> = session.authenticate(query.token) {
        val label = query.label
        val user = userRepository.findUserByIdWithBooklets(it)
            ?: return@authenticate bookletDomainFailure(
                ResultState.USER_NOT_FOUND,
                "L'utilisateur recherché n'existe pas",
                "domain.booklet.find_by_label.user_not_found"
            )
        success(
            user.booklets
            .find { acc -> acc.label == label }
            ?: return@authenticate bookletDomainFailure(
                ResultState.BOOKLET_LABEL_NOT_EXIST,
                "Le compte $label n'est pas enregistré en base",
                "domain.booklet.find_by_label.label_not_found"
            )
        )
    }
}
