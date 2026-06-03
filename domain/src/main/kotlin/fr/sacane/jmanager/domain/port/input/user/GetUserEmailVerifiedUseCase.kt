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

data class EmailVerifiedStatus(val emailVerified: Boolean, val email: String?, val mustChangePassword: Boolean = false)

data class GetUserEmailVerifiedQuery(val userId: UserId) : Query<EmailVerifiedStatus>

@Port(Side.APPLICATION)
interface GetUserEmailVerifiedUseCase : QueryHandler<GetUserEmailVerifiedQuery, EmailVerifiedStatus> {
    override val queryClass get() = GetUserEmailVerifiedQuery::class
}

@DomainService
class GetUserEmailVerifiedService(
    private val userRepository: UserRepository,
) : GetUserEmailVerifiedUseCase {

    override fun handle(query: GetUserEmailVerifiedQuery): Result<EmailVerifiedStatus> {
        val user = userRepository.findUserById(query.userId)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.email_verification.user_not_found", "L'utilisateur est introuvable"),
            )
        return success(EmailVerifiedStatus(emailVerified = user.emailVerified, email = user.email, mustChangePassword = user.mustChangePassword))
    }
}
