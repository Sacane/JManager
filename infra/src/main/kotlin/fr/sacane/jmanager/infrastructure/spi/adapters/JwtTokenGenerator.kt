package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import java.time.ZoneId
import java.util.*
import javax.crypto.spec.SecretKeySpec

class JwtTokenGenerator(
    @Value("\${auth.secret}")
    private val secret: String
): TokenGenerator {

    private val signingKey: SecretKeySpec
        get() {
            val keyBytes: ByteArray = Base64.getDecoder().decode(secret)
            return SecretKeySpec(keyBytes, 0, keyBytes.size, "HmacSHA256")
        }

    override fun generateToken(userId: UserId, role: Role): AccessToken {
        val expirationDate = Date(System.currentTimeMillis() + 60 * 60 * 1000) // 1 hour
        return Jwts.builder()
            .subject(userId.value.toString())
            .expiration(expirationDate)
            .claim("role", role.name)
            .signWith(signingKey)
            .compact()
            .let { tokenValue ->
                AccessToken(
                    userId = userId,
                    tokenValue = tokenValue,
                    tokenExpirationDate = expirationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    role = role
                )
            }

    }

    override fun readToken(token: String): AccessToken? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val userId = UserId(claims.subject.toLong())
            val role = Role.valueOf(claims["role"].toString())
            val expirationDate = claims.expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

            AccessToken(
                userId = userId,
                tokenValue = token,
                tokenExpirationDate = expirationDate,
                role = role
            )
        } catch (e: Exception) {
            null
        }
    }
}