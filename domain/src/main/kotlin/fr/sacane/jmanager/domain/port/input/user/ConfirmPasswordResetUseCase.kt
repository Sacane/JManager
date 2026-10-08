package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.models.PasswordPolicy
import fr.sacane.jmanager.domain.port.output.NotificationPort
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.port.output.repository.PasswordResetTokenRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.usecase.PasswordRenewal
import fr.sacane.jmanager.domain.usecase.PasswordResetTokenVerifier
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure

data class ConfirmPasswordResetCommand(
    val token: String,
    val newPassword: String,
    val confirmPassword: String,
) : Command<Unit>

@Port(Side.APPLICATION)
interface ConfirmPasswordResetUseCase : CommandHandler<ConfirmPasswordResetCommand, Unit> {
    override val commandClass get() = ConfirmPasswordResetCommand::class
}

/**
 * Sets a new password from a reset link. The link is checked before the passwords, so a stale one says
 * so first; a password the user must retype leaves the link usable.
 *
 * On success, in one transaction: the password, the cleared forced-change flag, the address marked
 * verified (the link proved it) and the token consumed; every session is revoked. The user is then told.
 */
@DomainService
class ConfirmPasswordResetService(
    private val verifier: PasswordResetTokenVerifier,
    private val tokenRepository: PasswordResetTokenRepository,
    private val userRepository: UserRepository,
    private val passwordRenewal: PasswordRenewal,
    private val notificationPort: NotificationPort,
    private val transaction: UnitOfWorkTransactionProvider,
) : ConfirmPasswordResetUseCase {

    override fun handle(command: ConfirmPasswordResetCommand): Result<Unit> {
        val verified = verifier.verify(command.token)
        val token = verified.mapNotNullOrFailure() ?: return verified.map { }

        if (command.newPassword != command.confirmPassword) {
            return failure(
                ResultState.PASSWORD_NOT_MATCH,
                DomainError(ResultState.PASSWORD_NOT_MATCH.code, "domain.user.password.mismatch", "Les mots de passe ne correspondent pas"),
            )
        }

        val user = userRepository.findUserById(token.userId)?.takeIf { it.isEnabled }
            ?: return PasswordResetTokenVerifier.invalidToken()

        PasswordPolicy.violationOf<Unit>(command.newPassword, user.email)?.let { return it }

        val renewed = transaction.executeInTransaction(user) {
            passwordRenewal.renew(it.id, command.newPassword, clearMustChange = true)
                .onSuccess { _ ->
                    if (!it.emailVerified) userRepository.markEmailVerified(it.id)
                    tokenRepository.deleteByUserId(it.id)
                }
        }
        return renewed.map { changedAt ->
            user.email?.let { email -> notificationPort.sendPasswordChangedEmail(email, changedAt) }
        }
    }
}
