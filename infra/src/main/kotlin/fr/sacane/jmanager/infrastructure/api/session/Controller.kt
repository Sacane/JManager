package fr.sacane.jmanager.infrastructure.api.session

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleUpdate
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.port.api.UserFeature
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.api.InvalidRequestException
import fr.sacane.jmanager.infrastructure.api.currentUser
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
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
    private val loginFeature: UserFeature,
    private val loginRateLimiter: LoginRateLimiter,
    @Value("\${server.servlet.session.cookie.secure:true}")
    private val secureCookie: Boolean = true,
){
    companion object {
        val LOGGER: Logger = Logger.getLogger(SessionController::class.java.name)
    }

    @PostMapping(path= ["/auth"])
    fun login(
        @Valid @RequestBody userDTO: UserPasswordDTO,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse
    ): ResponseEntity<UserStorageDTO> {
        val clientIp = httpRequest.remoteAddr
        if (!loginRateLimiter.isAllowed(clientIp)) {
            LOGGER.warning("Rate limit exceeded for IP $clientIp")
            return ResponseEntity.status(429).build()
        }
        val domainResult = loginFeature.login(userDTO.username, userDTO.password)
        LOGGER.info("Start authenticate user ${userDTO.username}...")
        if (domainResult.isFailure()) {
            loginRateLimiter.recordFailedAttempt(clientIp)
        } else {
            loginRateLimiter.clearAttempts(clientIp)
        }
        return domainResult.map {
            val cookie = ResponseCookie.from("token", it.token)
                .httpOnly(true)
                .path("/")
                .maxAge(60 * 60 * 24L)
                .secure(secureCookie)
                .sameSite("Strict")
                .build()
            httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
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
                val cookie = ResponseCookie.from("token", "")
                    .httpOnly(true)
                    .path("/")
                    .maxAge(0)
                    .secure(secureCookie)
                    .sameSite("Strict")
                    .build()
                httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
                SecurityContextHolder.getContext().authentication = null
                SecurityContextHolder.clearContext()
            }.toHttpResponse()
    }
    @PostMapping(path= ["/create"])
    fun createUser(@Valid @RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
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
        @Valid @RequestBody settings: UserSettingsUpdateDTO,
    ): ResponseEntity<UserSettingsDTO> {
        val bookletCycles = settings.bookletCycles.associate { cycle ->
            val bookletId = parseBookletId(cycle.bookletId)
            bookletId to BookletMonthlyCycleUpdate(
                monthlyPeriodStartDay = cycle.monthlyPeriodStartDay,
                monthlyPeriodEndDay = cycle.monthlyPeriodEndDay,
            )
        }

        return loginFeature.updateSettings(
            token = currentUser.token,
            projectionWindowDays = settings.projectionWindowDays,
            bookletCycles = bookletCycles,
        )
            .map { it.toDTO() }
            .toHttpResponse()
    }

    private fun parseBookletId(bookletId: String): UUID = try {
        UUID.fromString(bookletId)
    } catch (_: IllegalArgumentException) {
        throw InvalidRequestException(
            ResultState.INVALID.code,
            "L'identifiant de livret '$bookletId' est invalide",
            "domain.user.settings.invalid_booklet_id",
        )
    }
}

private fun UserSettings.toDTO(): UserSettingsDTO = UserSettingsDTO(
    projectionWindowDays = projectionWindowDays,
    bookletCycles = bookletCycles.map { cycle ->
        BookletMonthlyCycleDTO(
            bookletId = cycle.bookletId.toString(),
            label = cycle.bookletLabel,
            monthlyPeriodStartDay = cycle.monthlyPeriodStartDay,
            monthlyPeriodEndDay = cycle.monthlyPeriodEndDay,
        )
    },
)
