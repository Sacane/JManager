package fr.sacane.jmanager.server.adapters

import fr.sacane.jmanager.domain.Hash
import fr.sacane.jmanager.domain.hexadoc.DatasourceAdapter
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.LoginManager
import fr.sacane.jmanager.server.entity.Login
import fr.sacane.jmanager.server.repositories.LoginRepository
import fr.sacane.jmanager.server.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
@DatasourceAdapter
class LoginTransactionAdapter : LoginManager {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var loginRepository: LoginRepository
    companion object{
        private const val DEFAULT_TOKEN_LIFETIME_IN_HOURS = 1L //1hour
//        private const val DEFAULT_REFRESH_TOKEN_LIFETIME = 60L* 60L * 24L * 5L * 1000L // 5 days
    }

    override fun login(userPseudonym: String, password: Password): Ticket? {
        val userResponse = userRepository.findByPseudonym(userPseudonym)
        val user = userResponse ?: return null
        return if(Hash.contentEquals(user.password!!, password.value)){
            val login = loginRepository.save(Login(user, LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS)))
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

    override fun tokenBy(userId: UserId, ): Token? {
        val user = userRepository.findById(userId.get())
        if(user.isEmpty) return null
        val token = loginRepository.findByUser(user.get()) ?: return null
        return token.toModel()
    }

    override fun generateToken(user: User): Token? {
        return loginRepository
            .save(Login(
                user.asResource(),
                LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS))
            ).toModel()
    }
}