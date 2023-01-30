package fr.sacane.jmanager.infra.server.adapters

import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.serverside.LoginTransactor
import fr.sacane.jmanager.infra.server.entity.Login
import fr.sacane.jmanager.infra.server.repositories.LoginRepository
import fr.sacane.jmanager.infra.server.repositories.UserRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.*

@Service
class LoginTransactionReceiver(val userRepository: UserRepository, val loginRepository: LoginRepository) : LoginTransactor {

    companion object{
        private const val DEFAULT_TOKEN_LIFETIME = 60L * 60L * 1000L //1hour
        private const val DEFAULT_REFRESH_TOKEN_LIFETIME = 60L* 60L * 24L * 7L * 1000L // 7 days
    }

    override fun login(userId: UserId, password: Password, token: Token): Ticket {
        val userResponse = userRepository.findById(userId.get())
        if(userResponse.isEmpty) return invalidateTicket()
        val user = userResponse.get()
        if(MessageDigest.isEqual(user.password, password.get())){
            val login = loginRepository.save(Login(user, LocalDateTime.now()))
            return login.toValidateTicket(user.toModel())
        }
        return invalidateTicket()
    }

    override fun logout(userId: UserId, token: Token): Ticket {
        val userResponse = userRepository.findById(userId.get())
        if(userResponse.isEmpty) return invalidateTicket()
        val user = userResponse.get()
        val login = loginRepository.findByUser(user)
        if(token.id != login?.id) return invalidateTicket()
        loginRepository.delete(login)
        return emptyValidateTicket()
    }

    override fun refresh(userId: UserId, token: Token): Ticket {
        val userResponse = userRepository.findById(userId.get())
        if(userResponse.isEmpty) return invalidateTicket()
        val user = userResponse.get()
        val login = loginRepository.findByUser(user) ?: return invalidateTicket()
        if(login.refreshToken != token.refreshToken){
            return invalidateTicket()
        }
        login.id = UUID.randomUUID()
        login.refreshToken = UUID.randomUUID()
        login.lastRefresh = LocalDateTime.now()
        val response = loginRepository.save(login)
        return response.toValidateTicket(user.toModel())
    }

    @Scheduled(cron = "0 0 0/3 * * *")
    fun refreshExpiredTokens(){
        val limit = LocalDateTime.now().minusHours(DEFAULT_TOKEN_LIFETIME)
        for (login in loginRepository.findAll()){
            if(login.lastRefresh?.isBefore(limit) == true){
                login.refreshToken = UUID.randomUUID()
                login.lastRefresh = LocalDateTime.now()
                login.id = UUID.randomUUID()
                loginRepository.save(login)
            }
        }
    }
}