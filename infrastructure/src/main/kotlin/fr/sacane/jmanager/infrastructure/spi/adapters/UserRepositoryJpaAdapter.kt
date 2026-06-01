package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.SubscriptionPlan
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.ConsentRecord
import fr.sacane.jmanager.domain.port.output.UserRepository
import fr.sacane.jmanager.domain.utils.DomainError
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success
import java.time.LocalDateTime
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.asExistingResource
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.asResource
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModel
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModelWithPasswords
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.toModelWithSimpleBooklets
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
@Adapter(Side.INFRASTRUCTURE)
class UserRepositoryJpaAdapter (
    private val userPostgresRepository: UserPostgresRepository,
) : UserRepository {
    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java.toString())
    }

    @Transactional
    override fun findUserById(userId: UserId): User? {
        val id = userId.value ?: return null
        val user = userPostgresRepository.findById(id).orElse(null) ?: return null
        return user.toModel()
    }
    @Cacheable(cacheNames = ["allBooklets"], key = "#userId")
    @Transactional
    override fun findUserByIdWithBooklets(userId: UserId): User? {
        val id = userId.value ?: return null
        return userPostgresRepository.findByIdWithBooklets(id)?.toModelWithSimpleBooklets()
    }

    @Transactional
    override fun findByPseudonym(pseudonym: String): User? {
        val user = userPostgresRepository.findByUsername(pseudonym) ?: return null
        return user.toModel()
    }

    override fun findByPseudonymWithEncodedPassword(pseudonym: String): UserWithPassword? {
        val user = userPostgresRepository.findByUsername(pseudonym) ?: return null
        return user.toModelWithPasswords()
    }

    override fun findByEmailWithEncodedPassword(email: String): UserWithPassword? =
        userPostgresRepository.findByEmail(email)?.toModelWithPasswords()

    @Transactional
    override fun create(user: UserWithPassword): User?{
        return try{
            val userAsResource = user.user.asResource(user.password)
            val saved = userPostgresRepository.save(userAsResource)
            saved.toModel()
        }catch(e: Exception){
            LOGGER.severe("Failed to save user into database: ${e.message}")
            null
        }
    }
    @Transactional
    override fun register(username: String, password: String, roles: Set<Role>, subscriptionPlan: SubscriptionPlan, email: String?, tosAcceptedAt: LocalDateTime?, tosVersion: String?, privacyAcceptedAt: LocalDateTime?, emailVerified: Boolean): User? {
        return try {
            val userResource = UserResource(
                username = username,
                password = password,
                email = email,
                roles = roles.toMutableSet(),
                subscriptionPlan = subscriptionPlan,
                tosAcceptedAt = tosAcceptedAt,
                tosVersion = tosVersion,
                privacyAcceptedAt = privacyAcceptedAt,
                emailVerified = emailVerified,
            )
            val userResponse = userPostgresRepository.save(userResource)
            userResponse.toModel()
        } catch (e: Exception) {
            LOGGER.severe("Failed to register user: ${e.message}")
            null
        }
    }

    @Transactional
    override fun markEmailVerified(userId: UserId): Result<Unit> {
        val id = userId.value ?: return failure(
            ResultState.USER_NOT_FOUND,
            DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.email_verification.user_not_found", "Identifiant utilisateur invalide")
        )
        val resource = userPostgresRepository.findById(id).orElse(null)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.email_verification.user_not_found", "L'utilisateur est introuvable")
            )
        resource.emailVerified = true
        userPostgresRepository.save(resource)
        return success(Unit)
    }
    @Transactional
    override fun upsert(user: User): User? {
        val userResource = user.asExistingResource()
        return try {
            val userResponse = userPostgresRepository.save(userResource)
            userResponse.toModel()
        } catch (e: Exception) {
            LOGGER.severe("Failed to upsert user: ${e.message}")
            null
        }
    }

    @CacheEvict(cacheNames = ["userSettings", "allBooklets"], allEntries = true)
    @Transactional
    override fun updateProjectionWindowDays(userId: UserId, projectionWindowDays: Int): Boolean {
        val id = userId.value ?: return false
        return userPostgresRepository.updateProjectionWindowDays(id, projectionWindowDays) > 0
    }

    override fun findAll(): List<User> {
        return userPostgresRepository.findAll().map { it.toModel() }
    }

    @Transactional
    override fun recordConsent(userId: UserId, consent: ConsentRecord): Result<Unit> {
        val id = userId.value ?: return failure(
            ResultState.USER_NOT_FOUND,
            DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.consent.user_not_found", "L'utilisateur est introuvable")
        )
        val resource = userPostgresRepository.findById(id).orElse(null)
            ?: return failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.consent.user_not_found", "L'utilisateur est introuvable")
            )
        resource.tosAcceptedAt = consent.tosAcceptedAt
        resource.tosVersion = consent.tosVersion
        resource.privacyAcceptedAt = consent.privacyAcceptedAt
        userPostgresRepository.save(resource)
        return success(Unit)
    }

    @Transactional
    override fun deleteById(userId: UserId): Result<Unit> {
        val id = userId.value ?: return failure(
            ResultState.USER_NOT_FOUND,
            DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.delete.user_not_found", "Identifiant utilisateur invalide")
        )
        return if (userPostgresRepository.existsById(id)) {
            userPostgresRepository.deleteById(id)
            success(Unit)
        } else {
            failure(
                ResultState.USER_NOT_FOUND,
                DomainError(ResultState.USER_NOT_FOUND.code, "domain.user.delete.user_not_found", "Le compte à supprimer est introuvable")
            )
        }
    }
}
