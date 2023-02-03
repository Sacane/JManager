package fr.sacane.jmanager.infra.server.adapters

import com.sun.istack.logging.Logger
import fr.sacane.jmanager.common.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import fr.sacane.jmanager.infra.server.entity.Login
import fr.sacane.jmanager.infra.server.repositories.LoginRepository
import fr.sacane.jmanager.infra.server.repositories.UserRepository
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime

@Service
@DatasourceAdapter
class ServerUserAdapter(private val userRepository: UserRepository, private val loginRepository: LoginRepository): UserTransaction{

    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java)
    }
    override fun findById(userId: UserId): User {
        val user = userRepository.findById(userId.get())
        return user.get().toModel()
    }

    override fun checkUser(pseudonym: String, pwd: Password): Ticket {
        LOGGER.info("Trying to login user $pseudonym")
        val user = userRepository.findByPseudonym(pseudonym)
        if(!MessageDigest.isEqual(pwd.get(), user?.password)){
            return invalidateTicket()
        }
        val token = Login(user!!, LocalDateTime.now())
        val tokenBack = loginRepository.save(token)
        return Ticket(user.toModel(), TicketState.OK, Token(tokenBack.id!!, tokenBack.lastRefresh!!, tokenBack.refreshToken!!))
    }

    override fun findByPseudonym(pseudonym: String): User? {
        return userRepository.findByPseudonym(pseudonym)?.toModel()
    }

    override fun create(user: User): User? {
        LOGGER.info("user : ${user.id.get()} | ${user.password.get()}| ${user.email} | ${user.pseudonym} | ${user.username}")
        val entity = userRepository.save(user.asResource())
        return entity.toModel()
    }

    override fun save(user: User): User {
        userRepository.save(user.asResource())
        return user
    }
}