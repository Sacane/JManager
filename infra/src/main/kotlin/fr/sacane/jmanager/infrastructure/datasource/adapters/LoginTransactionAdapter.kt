package fr.sacane.jmanager.infrastructure.datasource.adapters

import fr.sacane.jmanager.domain.Hash
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.LoginManager
import fr.sacane.jmanager.infrastructure.datasource.entity.Login
import fr.sacane.jmanager.infrastructure.datasource.repositories.LoginRepository
import fr.sacane.jmanager.infrastructure.datasource.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import java.util.logging.Logger

@Service
@Adapter(DomainSide.DATASOURCE)
class LoginTransactionAdapter : LoginManager {

    @Autowired
    private lateinit var userRepository: UserRepository
    @Autowired
    private lateinit var loginRepository: LoginRepository
    companion object{
        private const val DEFAULT_TOKEN_LIFETIME_IN_HOURS = 1L //1hour
//        private const val DEFAULT_REFRESH_TOKEN_LIFETIME = 60L* 60L * 24L * 5L * 1000L // 5 days
        private val LOGGER = Logger.getLogger(Companion::class.java.toString())
    }

    override fun login(userPseudonym: String, password: Password): UserToken? {
        LOGGER.info("Trying to login user $userPseudonym")
        val userResponse = userRepository.findByUsername(userPseudonym) ?: return null
        LOGGER.info("Find user ${userResponse.username}")
        return if(Hash.contentEquals(userResponse.password!!, password.value!!)){
            val login = loginRepository.save(
                Login(
                    userResponse,
                    LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS)
                ))
            LOGGER.info("User ${userResponse.username} logged in")
            UserToken(userResponse.toModel(), login.toModel())
        }
        else null
    }

    override fun logout(userId: UserId, token: Token): Token? {
        val userResponse = userRepository.findById(userId.get())
        if(userResponse.isEmpty) return null
        val user = userResponse.get()
        val login = loginRepository.findByUser(user) ?: return null
        loginRepository.delete(login)
        return login.toModel()
    }

    override fun refresh(userId: UserId, token: Token): UserToken? {
        val userResponse = userRepository.findById(userId.get())
        if (userResponse.isEmpty) return null
        val user = userResponse.get()
        val login = loginRepository.findByUser(user) ?: return null
        login.id = UUID.randomUUID()
        login.refreshToken = UUID.randomUUID()
        login.lastRefresh = LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS)
        loginRepository.save(login)
        return UserToken(user.toModel(), login.toModel())
    }

    override fun tokenBy(userId: UserId): Token? {
        val user = userRepository.findById(userId.get())
        if(user.isEmpty) return null
        val token = loginRepository.findByUser(user.get()) ?: return null
        return token.toModel()
    }

    override fun generateToken(user: User): Token? {
        val userResponse = userRepository.findById(user.id.get())
        if(userResponse.isEmpty) return null
        return loginRepository
            .saveAndFlush(
                Login(
                userResponse.get(),
                LocalDateTime.now().plusHours(DEFAULT_TOKEN_LIFETIME_IN_HOURS))
            ).toModel()
    }
}