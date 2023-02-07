package fr.sacane.jmanager.server.adapters

import fr.sacane.jmanager.domain.Hash
import fr.sacane.jmanager.domain.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.serverside.LoginTransactor
import fr.sacane.jmanager.server.entity.Login
import fr.sacane.jmanager.server.repositories.LoginRepository
import fr.sacane.jmanager.server.repositories.UserRepository
import fr.sacane.jmanager.server.spring.SpringLayerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@DatasourceAdapter
class LoginTransactionAdapter : LoginTransactor {
    @Autowired
    private lateinit var service: SpringLayerService
    companion object{
        private const val DEFAULT_TOKEN_LIFETIME_IN_HOURS = 1L //1hour
//        private const val DEFAULT_REFRESH_TOKEN_LIFETIME = 60L* 60L * 24L * 5L * 1000L // 5 days
    }

    override fun login(userPseudonym: String, password: Password): Ticket? {
        return service.login(userPseudonym, password)
    }

    override fun logout(userId: UserId, token: Token): Ticket? {
        return service.logout(userId, token)
    }

    override fun refresh(userId: UserId, token: Token): Ticket? {
        return service.refresh(userId, token)
    }
}