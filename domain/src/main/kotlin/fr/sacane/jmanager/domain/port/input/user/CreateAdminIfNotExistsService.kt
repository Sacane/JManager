package fr.sacane.jmanager.domain.port.input.user

import fr.sacane.jmanager.domain.hexadoc.DomainService
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

@DomainService
class CreateAdminIfNotExistsService(
    private val userRepository: UserRepository,
    private val hasher: Hasher
) : CreateAdminIfNotExistsUseCase {

    companion object {
        private val LOGGER = Logger.getLogger(CreateAdminIfNotExistsService::class.java.name)
    }

    override fun createAdminIfNotExists(username: String, password: String): Result<User> {
        val existingAdmin = userRepository.findByPseudonymWithEncodedPassword(username)
        val hashedPassword = hasher.hash(password)
        if (existingAdmin != null) {
            LOGGER.info("Admin user already exists with username $username")
            return if (!hasher.verify(password, existingAdmin.password))
                failure(
                    ResultState.PASSWORD_NOT_MATCH,
                    DomainError(ResultState.PASSWORD_NOT_MATCH.code, "domain.user.admin.password_mismatch", "admin password does not match the existing one")
                )
            else success(existingAdmin.user)
        }
        val adminUser = userRepository.register(username, hashedPassword, setOf(Role.USER, Role.ADMIN))
            ?: return failure(
                ResultState.INVALID,
                DomainError(ResultState.INVALID.code, "domain.user.admin.creation_failed", "Une erreur est survenue lors de la création de l'administrateur")
            )
        LOGGER.info("Admin user created with username $username")
        return success(adminUser)
    }
}
