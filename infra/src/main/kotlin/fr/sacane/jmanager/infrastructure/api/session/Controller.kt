package fr.sacane.jmanager.infrastructure.api.session

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.api.SessionFeature
import fr.sacane.jmanager.infrastructure.api.id
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.sendAsHttpResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger

@RestController
@RequestMapping("api/user")
@Adapter(Side.APPLICATION)
class SessionController(
    private val loginFeature: SessionFeature
){
    companion object {
        val LOGGER: Logger = Logger.getLogger("ProfileController")
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
                token = it.token.tokenValue.toString(),
                refreshToken = it.token.refreshToken.toString(),
                tokenExpirationDate = it.token.tokenExpirationDate,
                refreshExpirationDate = it.token.refreshTokenLifetime
            )
        }.sendAsHttpResponse()
    }

    @PostMapping(path = ["/logout/{id}"])
    fun logout(@PathVariable id: Long, @RequestHeader("Authorization") token: String): ResponseEntity<Nothing> {
        return loginFeature.logout(id.id(), token.asTokenUUID())
            .sendAsHttpResponse()
    }
    @PostMapping(path= ["/create"])
    fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
        val response = loginFeature.register(userDTO.username, userDTO.password, userDTO.confirmPassword)
        return response.map { u -> u.toDTO() }.sendAsHttpResponse()
    }

    @PostMapping("/auth/refresh/{id}")
    fun tryRefresh(@PathVariable id: Long, @RequestHeader("Authorization") refreshToken: String)
    : ResponseEntity<UserStorageDTO> = loginFeature.tryRefresh(id.id(), refreshToken.asTokenUUID())
            .map {
                UserStorageDTO(
                    it.first.id.value,
                    it.first.username,
                    it.first.email,
                    it.second.tokenValue.toString(),
                    it.second.refreshToken.toString(),
                    it.second.tokenExpirationDate,
                    it.second.refreshTokenLifetime
                )
            }.sendAsHttpResponse()
}
