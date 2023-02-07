package fr.sacane.jmanager.app.adapters

import fr.sacane.jmanager.app.*
import fr.sacane.jmanager.domain.hexadoc.LeftAdapter
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.port.apiside.LoginManager
import fr.sacane.jmanager.domain.port.apiside.UserRegister
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
@LeftAdapter
class UserControlAdapter @Autowired constructor(private var userPort: UserRegister, private var loginPort: LoginManager) {
    fun createUser(userDTO: RegisteredUserDTO): ResponseEntity<UserDTO>{
        val response = userPort.signIn(userDTO.toModel())
        if(response.get() == null) return ResponseEntity.badRequest().build()
        return response.mapTo { u -> u!!.toDTO() }.toResponseEntity()
    }
    fun loginUser(userDTO: UserPasswordDTO): ResponseEntity<UserTokenDTO>{
        val response = loginPort.login(userDTO.username, Password(userDTO.password))
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        val ticket = response.get() ?: return ResponseEntity.badRequest().build()
        val user = ticket.user ?: return ResponseEntity.badRequest().build()
        val token = ticket.token ?: return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(UserTokenDTO(user.toDTO(), token.toDTO()))
    }

    fun logout(userId: Long, tokenDTO: TokenDTO): ResponseEntity<UserDTO>{
        val ticket = loginPort.logout(userId.id(), tokenDTO.toToken())
        if(ticket.status.isFailure()){
            return ResponseEntity.badRequest().build()
        }
        return ticket.mapTo { p -> p!!.user!!.toDTO() }.toResponseEntity()
    }

}