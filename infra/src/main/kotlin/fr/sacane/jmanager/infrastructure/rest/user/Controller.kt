package fr.sacane.jmanager.infrastructure.rest.user

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Password
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.port.api.LoginFeature
import fr.sacane.jmanager.infrastructure.extractToken
import fr.sacane.jmanager.infrastructure.rest.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.logging.Logger

@RestController
@RequestMapping("/user")
@Adapter(Side.API)
class SessionController(
    private val loginFeature: LoginFeature
){
    companion object {
        val LOGGER: Logger = Logger.getLogger("ProfileController")
    }

    @PostMapping(path= ["/auth"])
    fun login(@RequestBody userDTO: UserPasswordDTO): ResponseEntity<UserStorageDTO> {
        val response = loginFeature.login(userDTO.username, Password(userDTO.password))
        LOGGER.info("Trying to login user ${userDTO.username}")
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return response.map { u ->
            UserStorageDTO(
                u.user.id.id ?: throw InvalidRequestException("Invalid response, something went wrong"),
                username = u.user.username,
                email = u.user.email,
                token = u.token.tokenValue.toString(),
                refreshToken = u.token.refreshToken.toString()
            )
        }.toResponseEntity()
    }

    @PostMapping(path = ["/logout/{id}"])
    suspend fun logout(@PathVariable id: Long, @RequestHeader("Authorization") token: String): ResponseEntity<Nothing> {
        val ticket = loginFeature.logout(id.id(), token.asTokenUUID())
        if(ticket.status.isFailure()){
            return ResponseEntity.badRequest().build()
        }
        return ResponseEntity.ok().build()
    }
    @PostMapping(path= ["/create"])
    suspend fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
        val response = loginFeature.register(userDTO.toModel())
        if(response.isFailure()) return ResponseEntity.badRequest().build()
        return response.map { u -> u.toDTO() }.toResponseEntity()
    }
}