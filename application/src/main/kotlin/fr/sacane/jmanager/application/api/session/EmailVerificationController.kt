package fr.sacane.jmanager.application.api.session

import fr.sacane.jmanager.application.api.currentUser
import fr.sacane.jmanager.application.api.toHttpResponse
import fr.sacane.jmanager.application.bus.CommandBus
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.input.user.ResendVerificationEmailCommand
import fr.sacane.jmanager.domain.port.input.user.VerifyEmailCommand
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/verify-email")
@Adapter(Side.APPLICATION)
class EmailVerificationController(
    private val commandBus: CommandBus,
) {

    /**
     * Consumes a verification token sent by email.
     * Public — no authentication required (user may not be logged in when clicking the link).
     * Returns 200 on success, 410 Gone on expiry, 404 on unknown token.
     */
    @GetMapping
    fun verify(@RequestParam token: String): ResponseEntity<Unit> =
        commandBus.dispatch(VerifyEmailCommand(token)).toHttpResponse()

    /**
     * Re-issues a verification token and sends a new verification email.
     * Authenticated — the user must be logged in to request a resend.
     * Returns 200 on success, 409 Conflict if already verified.
     */
    @PostMapping("/resend")
    fun resend(): ResponseEntity<Unit> =
        commandBus.dispatch(ResendVerificationEmailCommand(UserId(currentUser.id))).toHttpResponse()
}
