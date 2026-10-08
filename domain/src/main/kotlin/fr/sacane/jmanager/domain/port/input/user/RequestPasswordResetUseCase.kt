package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.PasswordResetToken
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.NotificationPort
import fr.sacane.jmanager.domain.port.output.SecureTokenGenerator
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.port.output.repository.PasswordResetTokenRepository
import fr.sacane.jmanager.domain.port.output.repository.UnitOfWorkTransactionProvider
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.success
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

data class RequestPasswordResetCommand(val email: String) : Command<Unit>

@Port(Side.APPLICATION)
interface RequestPasswordResetUseCase : CommandHandler<RequestPasswordResetCommand, Unit> {
    override val commandClass get() = RequestPasswordResetCommand::class
}

/**
 * Sends a reset link to the account registered at an address, and always succeeds: whether an account
 * exists, is disabled or is ambiguous is never told to the caller. Only the log differs, without the address.
 */
@DomainService
class RequestPasswordResetService(
    private val userRepository: UserRepository,
    private val tokenRepository: PasswordResetTokenRepository,
    private val secureTokenGenerator: SecureTokenGenerator,
    private val notificationPort: NotificationPort,
    private val transaction: UnitOfWorkTransactionProvider,
    private val clock: Clock,
) : RequestPasswordResetUseCase {

    companion object {
        val TOKEN_LIFETIME: Duration = Duration.ofMinutes(30)
        private val log = LoggerFactory.getLogger(RequestPasswordResetService::class.java)
    }

    override fun handle(command: RequestPasswordResetCommand): Result<Unit> {
        val accounts = userRepository.findAllEnabledByEmailIgnoreCase(command.email.trim())
        if (accounts.size > 1) {
            log.warn("Password reset not sent: {} enabled accounts share one address up to case", accounts.size)
            return success(Unit)
        }
        val account = accounts.singleOrNull()
        val email = account?.email
        if (account == null || email == null) {
            log.info("Password reset requested for an address with no enabled account")
            return success(Unit)
        }

        val rawToken = secureTokenGenerator.generate()
        val token = PasswordResetToken(
            tokenHash = PasswordResetToken.digestOf(rawToken),
            userId = account.id,
            expiresAt = LocalDateTime.now(clock).plus(TOKEN_LIFETIME),
        )
        transaction.executeInTransaction(token) {
            tokenRepository.deleteByUserId(it.userId)
            tokenRepository.save(it)
        }
        notificationPort.sendPasswordResetEmail(email, rawToken)
        return success(Unit)
    }
}
