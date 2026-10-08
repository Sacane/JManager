package fr.sacane.jmanager.application.api.password

import fr.sacane.jmanager.application.api.toHttpResponse
import fr.sacane.jmanager.application.bus.CommandBus
import fr.sacane.jmanager.application.bus.QueryBus
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.input.user.ConfirmPasswordResetCommand
import fr.sacane.jmanager.domain.port.input.user.RequestPasswordResetCommand
import fr.sacane.jmanager.domain.port.input.user.ValidatePasswordResetTokenQuery
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger

/** Lets an anonymous visitor reset a forgotten password. */
@RestController
@RequestMapping("api/password-reset")
@Adapter(Side.APPLICATION)
class PasswordResetController(
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
    private val rateLimiter: PasswordResetRateLimiter,
) {
    companion object {
        private val LOGGER = Logger.getLogger(PasswordResetController::class.java.name)
    }

    /** Always 202 with an empty body: the answer never tells whether the address is registered. */
    @PostMapping(path = ["/request"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun request(@Valid @RequestBody dto: PasswordResetRequestDTO, httpRequest: HttpServletRequest): ResponseEntity<Void> {
        if (!rateLimiter.tryRequest(httpRequest.remoteAddr, dto.email)) return tooManyRequests(httpRequest)
        commandBus.dispatch(RequestPasswordResetCommand(dto.email))
        return ResponseEntity.accepted().build()
    }

    @PostMapping(path = ["/validate"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun validate(@Valid @RequestBody dto: PasswordResetTokenDTO, httpRequest: HttpServletRequest): ResponseEntity<Void> {
        if (!rateLimiter.tryTokenCheck(httpRequest.remoteAddr)) return tooManyRequests(httpRequest)
        queryBus.dispatch(ValidatePasswordResetTokenQuery(dto.token)).toHttpResponse()
        return ResponseEntity.noContent().build()
    }

    @PostMapping(path = ["/confirm"], consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun confirm(@Valid @RequestBody dto: ConfirmPasswordResetDTO, httpRequest: HttpServletRequest): ResponseEntity<Void> {
        if (!rateLimiter.tryTokenCheck(httpRequest.remoteAddr)) return tooManyRequests(httpRequest)
        commandBus.dispatch(ConfirmPasswordResetCommand(dto.token, dto.newPassword, dto.confirmPassword)).toHttpResponse()
        return ResponseEntity.noContent().build()
    }

    private fun tooManyRequests(httpRequest: HttpServletRequest): ResponseEntity<Void> {
        LOGGER.warning("Password reset rate limit exceeded for ${httpRequest.remoteAddr} on ${httpRequest.requestURI}")
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
    }
}
