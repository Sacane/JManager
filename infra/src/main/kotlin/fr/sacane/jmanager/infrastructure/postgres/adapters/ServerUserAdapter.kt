package fr.sacane.jmanager.infrastructure.postgres.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import fr.sacane.jmanager.infrastructure.postgres.entity.Login
import fr.sacane.jmanager.infrastructure.postgres.repositories.LoginRepository
import fr.sacane.jmanager.infrastructure.postgres.repositories.UserRepository
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.*
import java.util.logging.Logger

@Service
@Adapter(DomainSide.DATASOURCE)
class ServerUserAdapter (
    private val userRepository: UserRepository,
    private val loginRepository: LoginRepository
) : UserTransaction{
    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java.toString())
    }
    override fun findById(userId: UserId): UserToken? {
        val id = userId.id ?: return null
        val user = userRepository.findById(id).orElseThrow()
        val token = loginRepository.findByUser(user) ?: return null
        return UserToken(user.toModel(), token.toModel())
    }

    override fun findUserById(userId: UserId): User? {
        val id = userId.id ?: return null
        return userRepository.findById(id).orElse(null).toModel()
    }

    override fun checkUser(pseudonym: String, pwd: Password): UserToken? {
        val user = userRepository.findByUsername(pseudonym) ?: return null
        if(!MessageDigest.isEqual(pwd.get(), user.password)){
            LOGGER.info("Password is not correct")
            return null
        }
        val token = Login(user = user) // TODO Implement token implementation
        val tokenBack = loginRepository.save(token)
        return UserToken(user.toModel(), Token(tokenBack.id ?: UUID.randomUUID(), tokenBack.lastRefresh, tokenBack.refreshToken))
    }

    override fun findByPseudonym(pseudonym: String): User? {
        val user = userRepository.findByUsername(pseudonym) ?: return null
        return user.toModel()
    }

    override fun create(user: User): User?{
        return try{
            userRepository.save(user.asResource()).toModel()
        }catch(e: Exception){
            LOGGER.severe("Failed to save user into database")
            null
        }
    }

    override fun register(user: User): User? {
        val userResource = user.asResource()
        LOGGER.info("Trying to register $userResource into database")
        val userResponse = userRepository.save(userResource)
        return userResponse.toModel()
    }

    override fun upsert(user: User): User? {
        val userResource = user.asExistingResource()
        val userResponse = userRepository.saveAndFlush(userResource)
        return userResponse.toModel()
    }

    override fun getUserToken(userId: UserId): Token? {
        val id = userId.id ?: return null
        val user = userRepository.findById(id).orElseThrow()
        return loginRepository.findByUser(user)?.toModel()
    }
}