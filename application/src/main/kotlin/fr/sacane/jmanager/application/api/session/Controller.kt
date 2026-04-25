package fr.sacane.jmanager.application.api.session

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.BookletMonthlyCycleUpdate
import fr.sacane.jmanager.domain.models.UserToken
import fr.sacane.jmanager.domain.models.UserSettings
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.input.user.GetUserSettingsUseCase
import fr.sacane.jmanager.domain.port.input.user.LoginUseCase
import fr.sacane.jmanager.domain.port.input.user.LogoutUseCase
import fr.sacane.jmanager.domain.port.input.user.RefreshSessionUseCase
import fr.sacane.jmanager.domain.port.input.user.RegisterUserUseCase
import fr.sacane.jmanager.domain.port.input.user.UpdateUserSettingsUseCase
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.application.api.InvalidRequestException
import fr.sacane.jmanager.application.api.UnauthorizedRequestException
import fr.sacane.jmanager.application.api.currentUser
import fr.sacane.jmanager.application.api.toDTO
import fr.sacane.jmanager.application.api.toHttpResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import java.util.logging.Logger

@RestController
@RequestMapping("api/user")
@Adapter(Side.APPLICATION)
class SessionController(
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val refreshSessionUseCase: RefreshSessionUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
    private val getUserSettingsUseCase: GetUserSettingsUseCase,
    private val updateUserSettingsUseCase: UpdateUserSettingsUseCase,
    private val loginRateLimiter: LoginRateLimiter,
    @Value("\${server.servlet.session.cookie.secure:true}")
    private val secureCookie: Boolean = true,
){
    companion object {
        val LOGGER: Logger = Logger.getLogger(SessionController::class.java.name)
    }

    @PostMapping(path= ["/auth"], consumes = [MediaType.APPLICATION_JSON_VALUE])
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
        val domainResult = loginUseCase.login(userDTO.username, userDTO.password)
        LOGGER.info("Start authenticate user ${userDTO.username}...")
        if (domainResult.isFailure()) {
            loginRateLimiter.recordFailedAttempt(clientIp)
        } else {
            loginRateLimiter.clearAttempts(clientIp)
        }
        return domainResult.map {
            addAccessCookie(httpResponse, it.token)
            addRefreshCookie(httpResponse, it.refreshToken)
            UserStorageDTO(
                it.user.id.value.toString(),
                username = it.user.username,
                email = it.user.email,
                token = it.token,
                refreshToken = it.refreshToken?.toString(),
            )
        }.toHttpResponse()
    }

    @PostMapping(path = ["/logout"])
    fun logout(
        httpResponse: HttpServletResponse,

    ): ResponseEntity<Nothing> {
        return logoutUseCase.logout(SessionToken(currentUser.token))
            .also {
                clearCookie(httpResponse, "token")
                clearCookie(httpResponse, "refresh_token")
                SecurityContextHolder.getContext().authentication = null
                SecurityContextHolder.clearContext()
            }.toHttpResponse()
    }

    @PostMapping(path = ["/auth/refresh/{userId}"])
    fun refresh(
        @PathVariable userId: String,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<UserStorageDTO> {
        val requestedUserId = parseUserId(userId)
        val providedRefreshToken = parseRefreshToken(httpRequest)

        return refreshSessionUseCase.refresh(providedRefreshToken)
            .map {
                if (it.user.id.value != requestedUserId) {
                    throw UnauthorizedRequestException(
                        ResultState.UNAUTHORIZED.code,
                        "Impossible de rafraichir une session pour un autre utilisateur",
                        "domain.user.refresh.unauthorized_user"
                    )
                }

                addAccessCookie(httpResponse, it.token)
                addRefreshCookie(httpResponse, it.refreshToken)

                it.toStorageDTO()
            }.toHttpResponse()
    }

    @PostMapping(path = ["/auth/refresh"])
    fun refresh(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ResponseEntity<UserStorageDTO> {
        val providedRefreshToken = parseRefreshToken(httpRequest)

        return refreshSessionUseCase.refresh(providedRefreshToken)
            .map {
                addAccessCookie(httpResponse, it.token)
                addRefreshCookie(httpResponse, it.refreshToken)
                it.toStorageDTO()
            }.toHttpResponse()
    }

    @PostMapping(path = ["/create"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createUser(@Valid @RequestBody userDTO: RegisteredUserDTO): ResponseEntity<UserDTO> {
        val response = registerUserUseCase.register(userDTO.username, userDTO.password, userDTO.confirmPassword)
        return response.map { u -> u.toDTO() }.toHttpResponse()
    }

    @GetMapping(path = ["/settings"])
    fun getUserSettings(): ResponseEntity<UserSettingsDTO> {
        return getUserSettingsUseCase.getSettings(SessionToken(currentUser.token))
            .map { it.toDTO() }
            .toHttpResponse()
    }

    @PutMapping(path = ["/settings"], consumes = [MediaType.APPLICATION_JSON_VALUE])
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

        return updateUserSettingsUseCase.updateSettings(
            token = SessionToken(currentUser.token),
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

    private fun parseUserId(userId: String): UUID = try {
        UUID.fromString(userId)
    } catch (_: IllegalArgumentException) {
        throw InvalidRequestException(
            ResultState.INVALID.code,
            "L'identifiant utilisateur '$userId' est invalide",
            "domain.user.refresh.invalid_user_id",
        )
    }

    private fun parseRefreshToken(request: HttpServletRequest): UUID {
        val cookieValue = request.cookies?.firstOrNull { it.name == "refresh_token" }?.value
        val headerValue = request.getHeader("X-Refresh-Token")
        val bearerHeaderValue = request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.removePrefix("Bearer ")
            ?.trim()

        val rawToken = headerValue ?: cookieValue ?: bearerHeaderValue
            ?: throw UnauthorizedRequestException(
                ResultState.UNAUTHORIZED.code,
                "Refresh token manquant",
                "domain.user.refresh.missing_refresh_token"
            )

        return try {
            UUID.fromString(rawToken)
        } catch (_: IllegalArgumentException) {
            throw InvalidRequestException(
                ResultState.INVALID.code,
                "Le refresh token est invalide",
                "domain.user.refresh.invalid_refresh_token"
            )
        }
    }

    private fun addAccessCookie(httpResponse: HttpServletResponse, accessToken: String) {
        val cookie = ResponseCookie.from("token", accessToken)
            .httpOnly(true)
            .path("/")
            .maxAge(60 * 60 * 24L)
            .secure(secureCookie)
            .sameSite("Strict")
            .build()
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun addRefreshCookie(httpResponse: HttpServletResponse, refreshToken: UUID?) {
        if (refreshToken == null) {
            return
        }
        val cookie = ResponseCookie.from("refresh_token", refreshToken.toString())
            .httpOnly(true)
            .path("/")
            .maxAge(60 * 60 * 24L * 7)
            .secure(secureCookie)
            .sameSite("Strict")
            .build()
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun clearCookie(httpResponse: HttpServletResponse, cookieName: String) {
        val cookie = ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .path("/")
            .maxAge(0)
            .secure(secureCookie)
            .sameSite("Strict")
            .build()
        httpResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    private fun UserToken.toStorageDTO(): UserStorageDTO = UserStorageDTO(
        id = user.id.value.toString(),
        username = user.username,
        email = user.email,
        token = token,
        refreshToken = refreshToken?.toString(),
    )
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
