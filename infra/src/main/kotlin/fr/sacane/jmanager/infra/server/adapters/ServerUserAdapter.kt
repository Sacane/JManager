package fr.sacane.jmanager.infra.server.adapters

import com.sun.istack.logging.Logger
import fr.sacane.jmanager.domain.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import fr.sacane.jmanager.infra.server.entity.Login
import fr.sacane.jmanager.infra.server.repositories.LoginRepository
import fr.sacane.jmanager.infra.server.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime

@Service
@DatasourceAdapter
class ServerUserAdapter : UserTransaction{

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var loginRepository: LoginRepository

    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java)
    }
    override fun findById(userId: UserId): Ticket? {
        val user = userRepository.findById(userId.get())
        if(user.isEmpty) return null
        val token = loginRepository.findByUser(user.get()) ?: return null
        return Ticket(user.get().toModel(), token.toModel())
    }

    override fun checkUser(pseudonym: String, pwd: Password): Ticket? {
        LOGGER.info("Trying to login user $pseudonym")
        val user = userRepository.findByPseudonym(pseudonym)
        if(!MessageDigest.isEqual(pwd.get(), user?.password)){
            return null
        }
        val token = Login(user!!, LocalDateTime.now())
        val tokenBack = loginRepository.save(token)
        return Ticket(user.toModel(), Token(tokenBack.id!!, tokenBack.lastRefresh!!, tokenBack.refreshToken!!))
    }

    override fun findByPseudonym(pseudonym: String): Ticket? {
        val user = userRepository.findByPseudonym(pseudonym) ?: return null
        val token = loginRepository.findByUser(user) ?: return null
        return Ticket(user.toModel(), token.toModel())
    }

    override fun create(user: User): User?{
        LOGGER.info("Persist user : ${user.id.get()} | ${user.password.get()}| ${user.email} | ${user.pseudonym} | ${user.username}")
        return try{
            userRepository.save(user.asResource()).toModel()
        }catch(e: Exception){
            null
        }
    }

    override fun save(user: User): User? {
        return try{
            val userResponse = userRepository.save(user.asResource())
            userResponse.toModel()
        }catch (e: Exception){
            null
        }
    }

    override fun getUserToken(userId: UserId): Token? {
        val userResponse = userRepository.findById(userId.get())
        if (userResponse.isEmpty) return null
        return loginRepository.findByUser(userResponse.get())?.toModel()
    }
}