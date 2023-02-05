package fr.sacane.jmanager.infra.server.adapters

import fr.sacane.jmanager.common.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.serverside.LoginTransactor
import fr.sacane.jmanager.infra.server.entity.Login
import fr.sacane.jmanager.infra.server.repositories.LoginRepository
import fr.sacane.jmanager.infra.server.repositories.UserRepository
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

@Service
@DatasourceAdapter
class LoginTransactionAdapter(val userRepository: UserRepository, val loginRepository: LoginRepository) : LoginTransactor {

    companion object{
        private const val DEFAULT_TOKEN_LIFETIME_IN_HOURS = 1L //1hour
//        private const val DEFAULT_REFRESH_TOKEN_LIFETIME = 60L* 60L * 24L * 5L * 1000L // 5 days
    }

    override fun login(userPseudonym: String, password: Password): Ticket? {
        val userResponse = userRepository.findByPseudonym(userPseudonym)
        val user = userResponse ?: return null
        return if(MessageDigest.isEqual(user.password, password.get())){
            val login = loginRepository.save(Login(user, LocalDateTime.now().plusHours(1)))
            Ticket(user.toModel(), login.toModel())
        }
        else null
    }

    override fun logout(userId: UserId, token: Token): Ticket? {
        val userResponse = userRepository.findById(userId.get())
        if(userResponse.isEmpty) return null
        val user = userResponse.get()
        val login = loginRepository.findByUser(user) ?: return null
        loginRepository.delete(login)
        return null
    }

    override fun refresh(userId: UserId, token: Token): Ticket? {
        val userResponse = userRepository.findById(userId.get())
        if (userResponse.isEmpty) return null
        val user = userResponse.get()
        val login = loginRepository.findByUser(user) ?: return null
        login.id = UUID.randomUUID()
        login.refreshToken = UUID.randomUUID()
        login.lastRefresh = LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS)
        val response = loginRepository.save(login)
        return Ticket(user.toModel(), login.toModel())
    }
}