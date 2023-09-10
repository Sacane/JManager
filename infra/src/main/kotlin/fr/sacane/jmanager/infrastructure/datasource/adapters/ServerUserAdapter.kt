package fr.sacane.jmanager.infrastructure.datasource.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import fr.sacane.jmanager.infrastructure.datasource.entity.Login
import fr.sacane.jmanager.infrastructure.datasource.repositories.LoginRepository
import fr.sacane.jmanager.infrastructure.datasource.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.logging.Logger

@Service
@Adapter(DomainSide.DATASOURCE)
class ServerUserAdapter : UserTransaction{
    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var loginRepository: LoginRepository
    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java.toString())
    }
    override fun findById(userId: UserId): UserToken? {
        val user = userRepository.findById(userId.get())
        if(user.isEmpty) return null
        val token = loginRepository.findByUser(user.get()) ?: return null
        return UserToken(user.get().toModel(), token.toModel())
    }

    override fun findUserById(userId: UserId): User? {
        return userRepository.findById(userId.get()).get().toModel()
    }

    override fun checkUser(pseudonym: String, pwd: Password): UserToken? {
        val user = userRepository.findByUsername(pseudonym)
        if(!MessageDigest.isEqual(pwd.get(), user?.password)){
            LOGGER.info("Password is not correct")
            return null
        }
        val token = Login(user!!, LocalDateTime.now())
        val tokenBack = loginRepository.save(token)
        return UserToken(user.toModel(), Token(tokenBack.id!!, tokenBack.lastRefresh!!, tokenBack.refreshToken!!))
    }

    override fun findByPseudonym(pseudonym: String): User? {
        val user = userRepository.findByUsername(pseudonym)
        return user?.toModel()
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
        val userResponse = userRepository.findById(userId.get())
        if (userResponse.isEmpty) return null
        return loginRepository.findByUser(userResponse.get())?.toModel()
    }
}