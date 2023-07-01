package fr.sacane.jmanager.infrastructure.api.adapters

import com.sun.istack.logging.Logger
import fr.sacane.jmanager.domain.hexadoc.LeftAdapter
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.Token
import fr.sacane.jmanager.domain.port.api.Administrator
import fr.sacane.jmanager.infrastructure.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*

@Service
@LeftAdapter
class UserControlAdapter @Autowired constructor(private var userPort: Administrator) {
    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java)
    }
    fun createUser(userDTO: RegisteredUserDTO): ResponseEntity<UserDTO>{
        val response = userPort.register(userDTO.toModel())
        if(response.get() == null) return ResponseEntity.badRequest().build()
        return response.map { u -> u!!.toDTO() }.toResponseEntity()
    }
    fun loginUser(userDTO: UserPasswordDTO): ResponseEntity<UserStorageDTO>{
        val response = userPort.login(userDTO.username, Password(userDTO.password))
        LOGGER.info("Trying to login user ${userDTO.password} with password ${userDTO.password}")
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        val ticket = response.get() ?: return ResponseEntity.badRequest().build()
        val user = ticket.user
        val token = ticket.token
        return ResponseEntity.ok(UserStorageDTO(user.id.get(), user.username, user.email, token.id.toString()))
    }

    fun logout(userId: Long, tokenDTO: String): ResponseEntity<Nothing>{
        val ticket = userPort.logout(userId.id(), Token(UUID.fromString(tokenDTO), null, null))
        if(ticket.status.isFailure()){
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok().build()
    }

}