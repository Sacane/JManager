package fr.sacane.jmanager.infra.server.adapters

import fr.sacane.jmanager.domain.model.Ticket
import fr.sacane.jmanager.domain.model.Token
import fr.sacane.jmanager.domain.model.UserId
import fr.sacane.jmanager.domain.model.expiredTicket
import fr.sacane.jmanager.infra.server.entity.Login
import fr.sacane.jmanager.infra.server.repositories.LoginRepository
import fr.sacane.jmanager.infra.server.repositories.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class SessionManager {

    @Autowired
    private lateinit var loginRepository: LoginRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    fun login(userId: Long): Token?{
        val userResponse = userRepository.findById(userId)
        if(userResponse.isEmpty){
            return null
        }
        val user = userResponse.get()
        val login = Login(user, LocalDateTime.now())
        val token = loginRepository.save(login)
        return Token(token.id!!, token.lastRefresh!!, token.refreshToken!!)
    }

    fun logout(tokenId: UUID): Token?{
        val login = loginRepository.findById(tokenId)
        if(login.isPresent) loginRepository.deleteById(tokenId)
        else return null
        val entity = login.get()
        return Token(entity.id!!, entity.lastRefresh!!, entity.refreshToken!!)
    }
    fun refreshUserToken(userId: UserId, refreshTokenId: UUID) :Ticket{
        val user = userRepository.findById(userId.get())
        val token = loginRepository.findByUser(user.get())
        loginRepository.deleteById(token?.id!!)
        val userModel = user.get().toModel()
        return if(refreshTokenId == token.refreshToken){
            val login = loginRepository.save(Login(user.get(), LocalDateTime.now()))
            login.toValidateTicket(userModel)
        } else {
            expiredTicket()
        }
    }
    fun purgeExpiredToken(time: LocalDateTime){
        val tokens = loginRepository.findAll()
        for (token in tokens){
            if(token.lastRefresh?.isBefore(time) == true) {
                loginRepository.delete(token)
            }
        }
    }
}