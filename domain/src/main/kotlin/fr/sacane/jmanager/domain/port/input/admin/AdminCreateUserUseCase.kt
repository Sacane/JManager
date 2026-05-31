package fr.sacane.jmanager.domain.port.input.admin

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.CommandHandler
import fr.sacane.jmanager.domain.port.output.Hasher
import fr.sacane.jmanager.domain.port.output.NotificationPort
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.usecase.EmailVerificationIssuer
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success

data class AdminCreateUserCommand(
    val username: String,
    val password: String,
    val email: String,
) : Command<User>

@Port(Side.APPLICATION)
interface AdminCreateUserUseCase : CommandHandler<AdminCreateUserCommand, User> {
    override val commandClass get() = AdminCreateUserCommand::class
}

@DomainService
class AdminCreateUserService(
    private val userRepository: UserRepository,
    private val hasher: Hasher,
    private val notificationPort: NotificationPort,
    private val emailVerificationIssuer: EmailVerificationIssuer,
) : AdminCreateUserUseCase {

    override fun handle(command: AdminCreateUserCommand): Result<User> {
        if (command.email.isBlank()) {
            return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.admin.create.email_required", "L'email est obligatoire")
            )
        }
        val hashedPassword = hasher.hash(command.password)
        val user = userRepository.register(
            username = command.username,
            password = hashedPassword,
            subscriptionPlan = SubscriptionPlan.BETA_TESTER,
            email = command.email,
        ) ?: return failure(
            ResultState.INVALID,
            DomainError(ResultState.INVALID.code, "domain.user.admin.create.failed", "Une erreur est survenue lors de la création de l'utilisateur")
        )
        val verificationToken = emailVerificationIssuer.issue(user.id)
        notificationPort.sendWelcomeWithVerificationEmail(user.username, command.email, user.subscriptionPlan, verificationToken.token)
        return success(user)
    }
}
