package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.util.logging.Logger

@Service
@Adapter(Side.DATASOURCE)
class ServerUserAdapter (
    private val userPostgresRepository: UserPostgresRepository
) : UserRepository{
    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java.toString())
    }

    @Transactional
    override fun findUserById(userId: UserId): User? {
        val id = userId.id ?: return null
        return userPostgresRepository.findById(id).orElse(null).toModel()
    }
    @Transactional
    override fun findUserByIdWithAccounts(userId: UserId): User? {
        val id = userId.id ?: return null
        return userPostgresRepository.findByIdWithAccount(id)?.toModelWithSimpleAccounts()
    }

    @Transactional
    override fun findByPseudonym(pseudonym: String): User? {
        val user = userPostgresRepository.findByUsername(pseudonym) ?: return null
        return user.toModelWithPasswords()
    }
    @Transactional
    override fun create(user: User): User?{
        return try{
            val userAsResource = user.asResource()
            userPostgresRepository.save(userAsResource).toModel()
        }catch(e: Exception){
            LOGGER.severe("Failed to save user into database")
            null
        }
    }
    @Transactional
    override fun register(username: String, email: String, password: Password): User? {
        val userResource = UserResource(username = username, password = password.get(), email = email)
        val userResponse = userPostgresRepository.save(
            userResource
        )
        return userResponse.toModel()
    }
    @Transactional
    override fun upsert(user: User): User? {
        val userResource = user.asExistingResource()
        val userResponse = userPostgresRepository.save(userResource)
        return userResponse.toModel()
    }
}
