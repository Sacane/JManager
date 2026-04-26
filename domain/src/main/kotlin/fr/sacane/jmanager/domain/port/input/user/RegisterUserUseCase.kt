package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.spi.Hasher
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success

data class RegisterUserCommand(val username: String, val password: String, val confirmPassword: String)

@Port(Side.APPLICATION)
interface RegisterUserUseCase {
    fun handle(command: RegisterUserCommand): Result<User>
}

@DomainService
class RegisterUserService(
    private val userRepository: UserRepository,
    private val hasher: Hasher
) : RegisterUserUseCase {

    override fun handle(command: RegisterUserCommand): Result<User> {
        if (command.password != command.confirmPassword) {
            return failure(
                ResultState.PASSWORD_NOT_MATCH,
                DomainError(ResultState.PASSWORD_NOT_MATCH.code, "domain.user.register.password_mismatch", "Les mots de passes ne correspondent pas")
            )
        }
        val hashedPassword = hasher.hash(command.password)
        val userResult = userRepository.register(command.username, hashedPassword)
            ?: return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.register.invalid", "Une erreur est survenue")
            )
        return success(userResult)
    }
}
