package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.input.FeatureFlagged
import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.domain.port.output.NotificationPort
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.usecase.EmailVerificationIssuer
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

data class RegisterUserCommand(
    val username: String,
    val password: String,
    val confirmPassword: String,
    val email: String,
    val tosAcceptedAt: LocalDateTime? = null,
    val tosVersion: String? = null,
    val privacyAcceptedAt: LocalDateTime? = null,
) : Command<User>, FeatureFlagged {
    override val featureKey get() = FeatureKey.USER_REGISTRATION
}

@Port(Side.APPLICATION)
interface RegisterUserUseCase : CommandHandler<RegisterUserCommand, User> {
    override val commandClass get() = RegisterUserCommand::class
}

@DomainService
class RegisterUserService(
    private val userRepository: UserRepository,
    private val hasher: Hasher,
    private val notificationPort: NotificationPort,
    private val emailVerificationIssuer: EmailVerificationIssuer,
) : RegisterUserUseCase {

    companion object {
        private val log = LoggerFactory.getLogger(RegisterUserService::class.java)
    }

    override fun handle(command: RegisterUserCommand): Result<User> {
        if (command.password != command.confirmPassword) {
            return failure(
                ResultState.PASSWORD_NOT_MATCH,
                DomainError(ResultState.PASSWORD_NOT_MATCH.code, "domain.user.register.password_mismatch", "Les mots de passes ne correspondent pas")
            )
        }
        if (command.email.isBlank()) {
            return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.register.email_required", "L'email est obligatoire")
            )
        }
        if (command.tosAcceptedAt == null || command.privacyAcceptedAt == null) {
            return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.register.consent_required", "Le consentement aux CGU et à la politique de confidentialité est obligatoire")
            )
        }
        val hashedPassword = hasher.hash(command.password)
        val userResult = userRepository.register(
            command.username, hashedPassword,
            email = command.email,
            subscriptionPlan = SubscriptionPlan.BETA_TESTER,
            tosAcceptedAt = command.tosAcceptedAt,
            tosVersion = command.tosVersion,
            privacyAcceptedAt = command.privacyAcceptedAt,
        ) ?: return failure(
            ResultState.INVALID,
            DomainError(ResultState.INVALID.code, "domain.user.register.invalid", "Une erreur est survenue")
        )
        notificationPort.sendWelcomeEmail(userResult.username, command.email, userResult.subscriptionPlan)
        emailVerificationIssuer.issue(userResult.id, command.email)
        log.info("User registered successfully: plan={}", userResult.subscriptionPlan)
        return success(userResult)
    }
}
