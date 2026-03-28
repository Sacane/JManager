package fr.sacane.jmanager.infrastructure.api.session

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.port.api.UserFeature
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.api.InvalidRequestException
import fr.sacane.jmanager.infrastructure.api.currentUser
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
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
            }
            httpResponse.addCookie(cookie)
            UserStorageDTO(
                it.user.id.value.toString(),
                username = it.user.username,
                email = it.user.email,
                token = it.token
            )
        }.toHttpResponse()
    }

    @PostMapping(path = ["/logout"])
    fun logout(
        httpResponse: HttpServletResponse,

    ): ResponseEntity<Nothing> {
        return loginFeature.logout(currentUser.token)
            .also {
                val cookie = Cookie("token", null).apply {
                    isHttpOnly = true
                    path = "/"
                    maxAge = 0
                }
                httpResponse.addCookie(cookie)
                SecurityContextHolder.getContext().authentication = null
                SecurityContextHolder.clearContext()
            }.toHttpResponse()
    }
    @PostMapping(path= ["/create"])
    fun createUser(@RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
        val response = loginFeature.register(userDTO.username, userDTO.password, userDTO.confirmPassword)
        return response.map { u -> u.toDTO() }.toHttpResponse()
    }

    @GetMapping(path = ["/settings"])
    fun getUserSettings(): ResponseEntity<UserSettingsDTO> {
        return loginFeature.getSettings(currentUser.token)
            .map { it.toDTO() }
            .toHttpResponse()
    }

    @PutMapping(path = ["/settings"])
    fun updateUserSettings(
        @RequestBody settings: UserSettingsUpdateDTO,
    ): ResponseEntity<UserSettingsDTO> {
        val accountCycles = settings.accountCycles.associate { cycle ->
            val accountId = parseAccountId(cycle.accountId)
            accountId to cycle.monthlyPeriodStartDay
        }

        return loginFeature.updateSettings(
            token = currentUser.token,
            projectionWindowDays = settings.projectionWindowDays,
            accountCycles = accountCycles,
        )
            .map { it.toDTO() }
            .toHttpResponse()
    }

    private fun parseAccountId(accountId: String): UUID = try {
        UUID.fromString(accountId)
    } catch (_: IllegalArgumentException) {
        throw InvalidRequestException(
            ResultState.INVALID.code,
            "L'identifiant de compte '$accountId' est invalide",
            "domain.user.settings.invalid_account_id",
        )
    }
}

private fun UserSettings.toDTO(): UserSettingsDTO = UserSettingsDTO(
    projectionWindowDays = projectionWindowDays,
    accountCycles = accountCycles.map { cycle ->
        AccountMonthlyCycleDTO(
            accountId = cycle.accountId.toString(),
            label = cycle.accountLabel,
            monthlyPeriodStartDay = cycle.monthlyPeriodStartDay,
        )
    },
)
