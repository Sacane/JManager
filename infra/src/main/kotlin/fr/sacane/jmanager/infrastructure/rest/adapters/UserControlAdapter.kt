package fr.sacane.jmanager.infrastructure.rest.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.Token
import fr.sacane.jmanager.domain.port.api.Administrator
import fr.sacane.jmanager.infrastructure.rest.RegisteredUserDTO
import fr.sacane.jmanager.infrastructure.rest.UserDTO
import fr.sacane.jmanager.infrastructure.rest.UserPasswordDTO
import fr.sacane.jmanager.infrastructure.rest.UserStorageDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.*
import java.util.logging.Logger

@Service
@Adapter(DomainSide.API)
class UserControlAdapter @Autowired constructor(private var userPort: Administrator) {
    companion object{
        private val LOGGER = Logger.getLogger(Companion::class.java.toString())
    }
    fun createUser(userDTO: RegisteredUserDTO): ResponseEntity<UserDTO>{
        val response = userPort.register(userDTO.toModel())
        if(response.isFailure()) return ResponseEntity.badRequest().build()
        return response.map { u -> u!!.toDTO() }.toResponseEntity()
    }
    fun loginUser(userDTO: UserPasswordDTO): ResponseEntity<UserStorageDTO>{
        val response = userPort.login(userDTO.username, Password(userDTO.password))
        LOGGER.info("Trying to login user ${userDTO.password} with password ${userDTO.password}")
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return response.map { u -> UserStorageDTO(u!!.user.id.id!!, u.user.username, u.user.email, u.token.id.toString()) }.toResponseEntity()
    }

    fun logout(userId: Long, tokenDTO: String): ResponseEntity<Nothing>{
        val ticket = userPort.logout(userId.id(), Token(UUID.fromString(tokenDTO), null, null))
        if(ticket.status.isFailure()){
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok().build()
    }

}