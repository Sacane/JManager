package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.port.spi.Hasher
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.util.logging.Logger

data class CreateAdminIfNotExistsCommand(val username: String, val password: String)

@Port(Side.APPLICATION)
interface CreateAdminIfNotExistsUseCase {
    fun handle(command: CreateAdminIfNotExistsCommand): Result<User>
}

@DomainService
class CreateAdminIfNotExistsService(
    private val userRepository: UserRepository,
    private val hasher: Hasher
) : CreateAdminIfNotExistsUseCase {

    companion object {
        private val LOGGER = Logger.getLogger(CreateAdminIfNotExistsService::class.java.name)
    }

    override fun handle(command: CreateAdminIfNotExistsCommand): Result<User> {
        val existingAdmin = userRepository.findByPseudonymWithEncodedPassword(command.username)
        val hashedPassword = hasher.hash(command.password)
        if (existingAdmin != null) {
            LOGGER.info("Admin user already exists with username ${command.username}")
            return if (!hasher.verify(command.password, existingAdmin.password))
                failure(
                    ResultState.PASSWORD_NOT_MATCH,
                    DomainError(ResultState.PASSWORD_NOT_MATCH.code, "domain.user.admin.password_mismatch", "admin password does not match the existing one")
                )
            else success(existingAdmin.user)
        }
        val adminUser = userRepository.register(command.username, hashedPassword, setOf(Role.USER, Role.ADMIN))
            ?: return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.admin.creation_failed", "Une erreur est survenue lors de la création de l'administrateur")
            )
        LOGGER.info("Admin user created with username ${command.username}")
        return success(adminUser)
    }
}
