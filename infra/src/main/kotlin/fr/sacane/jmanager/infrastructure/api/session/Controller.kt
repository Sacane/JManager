package fr.sacane.jmanager.infrastructure.api.session

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.api.UserFeature
import fr.sacane.jmanager.infrastructure.api.id
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
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
    fun login(
        @RequestBody userDTO: UserPasswordDTO,
        httpResponse: HttpServletResponse
    ): ResponseEntity<UserStorageDTO> {
        val domainResult = loginFeature.login(userDTO.username, userDTO.password)
        LOGGER.info("Start authenticate user ${userDTO.username}...")
        return domainResult.map {
            val cookie = Cookie("token", it.token).apply {
                isHttpOnly = true
                path = "/"
                maxAge = 60 * 60 * 24 // 1 day
                secure = false
            }
            httpResponse.addCookie(cookie)
            UserStorageDTO(
                it.user.id.value,
                username = it.user.username,
                email = it.user.email,
                token = it.token
            )
        }.toHttpResponse()
    }

    @PostMapping(path = ["/logout"])
    fun logout(
        @RequestHeader("Authorization") token: String,
        httpResponse: HttpServletResponse
    ): ResponseEntity<Nothing> {
        return loginFeature.logout(token.asTokenUUID())
            .also {
                val cookie = Cookie("token", null).apply {
                    isHttpOnly = true
                    path = "/"
                    maxAge = 0
                    secure = false
                }
                httpResponse.addCookie(cookie)
            }.toHttpResponse()
    }
//    @PostMapping(path= ["/create"])
//    fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
//        val response = loginFeature.register(userDTO.username, userDTO.password, userDTO.confirmPassword)
//        return response.map { u -> u.toDTO() }.toHttpResponse()
//    }
}
