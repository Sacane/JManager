package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.port.output.UserRepository
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
    override fun register(username: String, password: String, roles: Set<Role>): User? {
        return try {
            val userResource = UserResource(username = username, password = password, roles = roles.toMutableSet())
            val userResponse = userPostgresRepository.save(userResource)
            userResponse.toModel()
        } catch (e: Exception) {
            LOGGER.severe("Failed to register user: ${e.message}")
            null
        }
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
}
