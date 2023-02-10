package fr.sacane.jmanager.server.adapters

import com.sun.istack.logging.Logger
import fr.sacane.jmanager.domain.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import fr.sacane.jmanager.server.spring.SpringLayerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@DatasourceAdapter
class ServerUserAdapter : UserTransaction{

    @Autowired
    private lateinit var service: SpringLayerService

    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java)
    }
    override fun findById(userId: UserId): Ticket? {
        return service.findById(userId)
    }

    override fun checkUser(pseudonym: String, pwd: Password): Ticket? {
        LOGGER.info("Trying to login user $pseudonym")
        return service.checkUser(pseudonym, pwd)
    }

    override fun findByPseudonym(pseudonym: String): User? {
        return service.findByPseudonym(pseudonym)
    }

    override fun create(user: User): User?{
        return service.create(user)
    }

    override fun save(user: User): User? {
        return service.save(user)
    }

    override fun getUserToken(userId: UserId): Token? {
        return service.getUserToken(userId)
    }
}