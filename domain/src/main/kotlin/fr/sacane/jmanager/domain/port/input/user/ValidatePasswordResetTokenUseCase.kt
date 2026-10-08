package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.input.Query
import fr.sacane.jmanager.domain.port.input.QueryHandler
import fr.sacane.jmanager.domain.usecase.PasswordResetTokenVerifier
import fr.sacane.jmanager.domain.utils.Result

data class ValidatePasswordResetTokenQuery(val token: String) : Query<Unit>

@Port(Side.APPLICATION)
interface ValidatePasswordResetTokenUseCase : QueryHandler<ValidatePasswordResetTokenQuery, Unit> {
    override val queryClass get() = ValidatePasswordResetTokenQuery::class
}

/** Tells whether a reset link can still be used, without consuming it. */
@DomainService
class ValidatePasswordResetTokenService(
    private val verifier: PasswordResetTokenVerifier,
) : ValidatePasswordResetTokenUseCase {
    override fun handle(query: ValidatePasswordResetTokenQuery): Result<Unit> = verifier.verify(query.token).map { }
}
