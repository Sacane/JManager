package fr.sacane.jmanager.infra.server.adapters

import fr.sacane.jmanager.domain.model.Password
import fr.sacane.jmanager.domain.model.User
import fr.sacane.jmanager.domain.model.UserId
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import fr.sacane.jmanager.infra.server.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.security.MessageDigest

@Service
class ServerUserAdapter(private val userRepository: UserRepository): UserTransaction{

    companion object{
        private val LOGGER = LoggerFactory.getLogger("infra.server.adapters.ServerUserAdapter")
    }
    override fun findById(userId: UserId): User {
        TODO("Not yet implemented")
    }

    override fun checkUser(pseudonym: String, pwd: Password): Boolean {
        val user = userRepository.findByPseudonym(pseudonym)
        LOGGER.info("${pwd.value} -> ${user?.password} -> ${pwd.get()}")
        val res = MessageDigest.isEqual(pwd.get(), user?.password)
        LOGGER.info("$res")
        return res
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