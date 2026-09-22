package fr.sacane.jmanager.application.api.password

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.PasswordPolicy
import fr.sacane.jmanager.domain.models.PasswordRule
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

/** The password rules, as stable keys; the client words them. */
data class PasswordPolicyDTO(
    val minLength: Int,
    val maxLength: Int,
    val rules: List<String>,
)

/**
 * Publishes the password policy so the screens can show the rules before anyone types — the sign-up screen
 * included, hence public. The client keeps no copy of the numbers: a copy is how a 6-character minimum came
 * to be announced that nothing enforced.
 *
 * Reads the domain policy directly: it is a constant rule set, not a use case with an input.
 */
@RestController
@Adapter(Side.APPLICATION)
class PasswordPolicyController {

    @GetMapping("/api/password-policy")
    fun get(): ResponseEntity<PasswordPolicyDTO> =
        ResponseEntity.ok()
            // Changes only with a release.
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
            .body(
                PasswordPolicyDTO(
                    minLength = PasswordPolicy.MIN_LENGTH,
                    maxLength = PasswordPolicy.MAX_LENGTH,
                    rules = PasswordRule.entries.map { it.key },
                ),
            )
}
