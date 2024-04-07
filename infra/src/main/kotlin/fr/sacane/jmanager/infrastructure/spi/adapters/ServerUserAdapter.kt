package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.infrastructure.spi.entity.Login
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.AccountRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.LoginRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.util.*
import java.util.logging.Logger

@Service
@Adapter(Side.DATASOURCE)
class ServerUserAdapter (
    private val userPostgresRepository: UserPostgresRepository,
    private val accountRepository: AccountRepository,
    private val loginRepository: LoginRepository
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

//    override fun findUserByIdWithSheets(userId: UserId): User? {
//        val id = userId.id ?: return null
//        println("find user by id with sheets...")
//        return userPostgresRepository.findByIdWithSheets(id)?.toModel()
//    }
    @Transactional
    override fun checkUser(pseudonym: String, pwd: Password): UserToken? {
        val user = userPostgresRepository.findByUsername(pseudonym) ?: return null
        if(!MessageDigest.isEqual(pwd.get(), user.password)){
            LOGGER.info("Password is not correct")
            return null
        }
        val token = Login(user = user) // TODO Implement token implementation
        val tokenBack = loginRepository.save(token)
        return UserToken(user.toMinimalUserRepresentation(), AccessToken(tokenBack.id ?: UUID.randomUUID(), tokenBack.tokenLifeTime, tokenBack.refreshToken))
    }
    @Transactional
    override fun findByPseudonym(pseudonym: String): User? {
        val user = userPostgresRepository.findByUsername(pseudonym) ?: return null
        return user.toModelWithPasswords()
    }
    @Transactional
    override fun create(user: User): User?{
        return try{
            userPostgresRepository.save(user.asResource()).toModel()
        }catch(e: Exception){
            LOGGER.severe("Failed to save user into database")
            null
        }
    }
    @Transactional
    override fun register(username: String, email: String, password: Password): User? {
        val userResponse = userPostgresRepository.save(
            UserResource(username = username, password = password.get(), email = email)
        )
        return userResponse.toModel()
    }
    @Transactional
    override fun upsert(user: User): User? {
        val userResource = user.asExistingResource()
        val userResponse = userPostgresRepository.save(userResource)
        return userResponse.toModel()
    }
    @Transactional
    override fun getUserToken(userId: UserId): AccessToken? {
        val id = userId.id ?: return null
        val user = userPostgresRepository.findById(id).orElse(null) ?: return null
        return loginRepository.findByUser(user)?.toModel()
    }
}