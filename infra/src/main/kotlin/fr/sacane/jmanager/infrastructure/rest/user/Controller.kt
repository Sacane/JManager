package fr.sacane.jmanager.infrastructure.rest.user

import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.Token
import fr.sacane.jmanager.domain.port.api.Administrator
import fr.sacane.jmanager.infrastructure.extractToken
import fr.sacane.jmanager.infrastructure.rest.adapters.*
import fr.sacane.jmanager.infrastructure.rest.adapters.id
import fr.sacane.jmanager.infrastructure.rest.adapters.toModel
import fr.sacane.jmanager.infrastructure.rest.adapters.toResponseEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.logging.Logger

@RestController
@RequestMapping("/user")
class ProfileController(
    private val administrator: Administrator
){
    companion object {
        val LOGGER: Logger = Logger.getLogger("ProfileController")
    }

    @PostMapping(path= ["/auth"])
    fun login(@RequestBody userDTO: UserPasswordDTO): ResponseEntity<UserStorageDTO> {
        val response = administrator.login(userDTO.username, Password(userDTO.password))
        LOGGER.info("Trying to login user ${userDTO.password} with password ${userDTO.password}")
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return response.map { u -> UserStorageDTO(u!!.user.id.id!!, u.user.username, u.user.email, u.token.id.toString()) }.toResponseEntity()
    }

    @PostMapping(path = ["/logout/{id}"])
    suspend fun logout(@PathVariable id: Long, @RequestHeader("Authorization") token: String): ResponseEntity<Nothing> {
        val ticket = administrator.logout(id.id(), Token(UUID.fromString(extractToken(token))))
        if(ticket.status.isFailure()){
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok().build()
    }
    @PostMapping(path= ["/create"])
    suspend fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
        val response = administrator.register(userDTO.toModel())
        if(response.isFailure()) return ResponseEntity.badRequest().build()
        return response.map { u -> u!!.toDTO() }.toResponseEntity()
    }
}