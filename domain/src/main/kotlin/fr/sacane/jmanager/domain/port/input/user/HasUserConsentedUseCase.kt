package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success

/**
 * Query that resolves whether a user has already provided explicit GDPR consent.
 * Returns `false` for admin-created accounts where [User.consent] is null.
 */
data class HasUserConsentedQuery(val userId: UserId) : Query<Boolean>

@Port(Side.APPLICATION)
interface HasUserConsentedUseCase : QueryHandler<HasUserConsentedQuery, Boolean> {
    override val queryClass get() = HasUserConsentedQuery::class
}

@DomainService
class HasUserConsentedService(
    private val userRepository: UserRepository,
) : HasUserConsentedUseCase {

    override fun handle(query: HasUserConsentedQuery): Result<Boolean> {
        val user = userRepository.findUserById(query.userId)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.consent.user_not_found", "L'utilisateur est introuvable"),
            )
        return success(user.consent != null)
    }
}
