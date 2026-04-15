package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.spi.adapters.utils.JwtTokenGenerator
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.UUID

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

}