package fr.sacane.jmanager.infrastructure.api.session

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.api.UserFeature
import fr.sacane.jmanager.infrastructure.api.id
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger

@RestController
@RequestMapping("api/user")
@Adapter(Side.APPLICATION)
class SessionController(
    private val loginFeature: UserFeature
){
    companion object {
        val LOGGER: Logger = Logger.getLogger(SessionController::class.java.name)
    }

    @PostMapping(path= ["/auth"])
    fun login(@RequestBody userDTO: UserPasswordDTO): ResponseEntity<UserStorageDTO> {
        val response = loginFeature.login(userDTO.username, userDTO.password)
        LOGGER.info("Start authenticate user ${userDTO.username}...")
        return response.map {
            UserStorageDTO(
                it.user.id.value,
                username = it.user.username,
                email = it.user.email,
                token = it.token
            )
        }.toHttpResponse()
    }

    @PostMapping(path = ["/logout/{id}"])
    fun logout(@PathVariable id: Long, @RequestHeader("Authorization") token: String): ResponseEntity<Nothing> {
        return loginFeature.logout(id.id(), token.asTokenUUID())
            .toHttpResponse()
    }
//    @PostMapping(path= ["/create"])
//    fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
//        val response = loginFeature.register(userDTO.username, userDTO.password, userDTO.confirmPassword)
//        return response.map { u -> u.toDTO() }.toHttpResponse()
//    }
}
