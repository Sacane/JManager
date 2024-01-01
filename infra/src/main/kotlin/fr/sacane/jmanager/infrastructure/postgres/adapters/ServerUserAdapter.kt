package fr.sacane.jmanager.infrastructure.postgres.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.infrastructure.postgres.entity.Login
import fr.sacane.jmanager.infrastructure.postgres.repositories.LoginRepository
import fr.sacane.jmanager.infrastructure.postgres.repositories.UserPostgresRepository
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.*
import java.util.logging.Logger

@Service
@Adapter(Side.DATASOURCE)
class ServerUserAdapter (
    private val userPostgresRepository: UserPostgresRepository,
    private val loginRepository: LoginRepository
) : UserRepository{
    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java.toString())
    }
//    override fun findById(userId: UserId): UserToken? {
//        val id = userId.id ?: return null
//        val user = userPostgresRepository.findById(id).orElseThrow()
//        val token = loginRepository.findByUser(user) ?: return null
//        return UserToken(user.toModel(), token.toModel())
//    }

    override fun findUserById(userId: UserId): User? {
        val id = userId.id ?: return null
        return userPostgresRepository.findById(id).orElse(null).toModel()
    }

    override fun checkUser(pseudonym: String, pwd: Password): UserToken? {
        val user = userPostgresRepository.findByUsername(pseudonym) ?: return null
        if(!MessageDigest.isEqual(pwd.get(), user.password)){
            LOGGER.info("Password is not correct")
            return null
        }
        val token = Login(user = user) // TODO Implement token implementation
        val tokenBack = loginRepository.save(token)
        return UserToken(user.toModel(), AccessToken(tokenBack.id ?: UUID.randomUUID(), tokenBack.tokenLifeTime, tokenBack.refreshToken))
    }

    override fun findByPseudonym(pseudonym: String): User? {
        val user = userPostgresRepository.findByUsername(pseudonym) ?: return null
        return user.toModel()
    }

    override fun create(user: User): User?{
        return try{
            userPostgresRepository.save(user.asResource()).toModel()
        }catch(e: Exception){
            LOGGER.severe("Failed to save user into database")
            null
        }
    }

    override fun register(user: User): User? {
        val userResource = user.asResource()
        LOGGER.info("Trying to register $userResource into database")
        val userResponse = userPostgresRepository.save(userResource)
        return userResponse.toModel()
    }

    override fun upsert(user: User): User? {
        val userResource = user.asExistingResource()
        val userResponse = userPostgresRepository.save(userResource)
        return userResponse.toModel()
    }

    override fun getUserToken(userId: UserId): AccessToken? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElse(null) ?: return null
        return loginRepository.findByUser(user)?.toModel()
    }
}