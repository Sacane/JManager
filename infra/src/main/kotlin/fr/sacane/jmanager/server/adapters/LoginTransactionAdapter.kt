package fr.sacane.jmanager.server.adapters

import fr.sacane.jmanager.domain.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.serverside.LoginInventory
import fr.sacane.jmanager.server.spring.SpringLayerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@DatasourceAdapter
class LoginTransactionAdapter : LoginInventory {
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