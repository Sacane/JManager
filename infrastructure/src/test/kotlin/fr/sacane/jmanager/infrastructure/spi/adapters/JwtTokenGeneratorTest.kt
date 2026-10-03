package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.JwtTokenGenerator
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import io.jsonwebtoken.Jwts
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

class JwtTokenGeneratorTest {
    private val secret: String = "THIS-IS-A-TEST-JWT-SECRET-THAT-SHOULD-NOT-BE-USED-IN-PRODUCTION"
    private val base64Secret = Base64.getEncoder().encodeToString(secret.toByteArray())

    private val tokenGenerator = JwtTokenGenerator(base64Secret)

    @Test
    fun `token must be correctly generated`() {
        val userId = UserId(UUID.randomUUID())
        val role = Role.USER
        val token = tokenGenerator.generateToken(userId, "test", setOf(role))

        Assertions.assertThat(token).isNotNull
        Assertions.assertThat(token.userId).isEqualTo(userId)
        Assertions.assertThat(token.roles).contains(role)
        Assertions.assertThat(token.tokenValue).isNotEmpty
    }

    @Test
    fun `should read a generated token`() {
        val userId = UserId(UUID.randomUUID())
        val role = Role.USER
        val token = tokenGenerator.generateToken(userId, "test", setOf(role))

        val readToken = tokenGenerator.readToken(token.tokenValue)

        Assertions.assertThat(readToken).isNotNull
        Assertions.assertThat(readToken?.userId).isEqualTo(userId)
        Assertions.assertThat(readToken?.roles).contains(role)
        Assertions.assertThat(readToken?.userName).isEqualTo("test")
        Assertions.assertThat(readToken?.tokenValue).isEqualTo(token.tokenValue)
    }

    // Compared with credentialsChangedAt, which the domain records from the UTC clock.
    @Test
    fun `shouldCarryItsIssueTimeInUtc_whenReadBack`() {
        val clock = Clock.fixed(Instant.parse("2026-09-22T08:30:15.600Z"), ZoneId.of("Europe/Paris"))
        val generator = JwtTokenGenerator(base64Secret, clock)

        val token = generator.generateToken(UserId(UUID.randomUUID()), "test", setOf(Role.USER))

        val expected = LocalDateTime.of(2026, 9, 22, 8, 30, 15)
        Assertions.assertThat(token.issuedAt).isEqualTo(expected)
        Assertions.assertThat(generator.readToken(token.tokenValue)?.issuedAt).isEqualTo(expected)
    }

    // Tokens signed before this release carry no issue time.
    @Test
    fun `shouldReadNoIssueTime_whenTokenHasNone`() {
        val keyBytes = Base64.getDecoder().decode(base64Secret)
        val legacy = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .expiration(Date(System.currentTimeMillis() + 60_000))
            .claim("username", "test")
            .signWith(SecretKeySpec(keyBytes, 0, keyBytes.size, "HmacSHA256"))
            .compact()

        val read = tokenGenerator.readToken(legacy)

        Assertions.assertThat(read).isNotNull
        Assertions.assertThat(read?.issuedAt).isNull()
    }

}