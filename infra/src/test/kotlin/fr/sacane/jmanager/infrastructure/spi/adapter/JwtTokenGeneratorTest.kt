package fr.sacane.jmanager.infrastructure.spi.adapter

import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.spi.adapters.JwtTokenGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class JwtTokenGeneratorTest {
    private val secret: String = "THIS-IS-A-TEST-JWT-SECRET-THAT-SHOULD-NOT-BE-USED-IN-PRODUCTION"
    private val base64Secret = Base64.getEncoder().encodeToString(secret.toByteArray())

    private val tokenGenerator = JwtTokenGenerator(base64Secret)

    @Test
    fun `token must be correctly generated`() {
        val userId = UserId(1)
        val role = Role.USER
        val token = tokenGenerator.generateToken(userId, role)

        assertThat(token).isNotNull
        assertThat(token.userId).isEqualTo(userId)
        assertThat(token.role).isEqualTo(role)
        assertThat(token.tokenValue).isNotEmpty
    }

    @Test
    fun `should read a generated token`() {
        val userId = UserId(1)
        val role = Role.USER
        val token = tokenGenerator.generateToken(userId, role)

        val readToken = tokenGenerator.readToken(token.tokenValue)

        assertThat(readToken).isNotNull
        assertThat(readToken?.userId).isEqualTo(userId)
        assertThat(readToken?.role).isEqualTo(role)
        assertThat(readToken?.tokenValue).isEqualTo(token.tokenValue)
    }

}