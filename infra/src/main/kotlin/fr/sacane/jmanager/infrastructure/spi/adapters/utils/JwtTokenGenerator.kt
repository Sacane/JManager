package fr.sacane.jmanager.infrastructure.spi.adapters.utils

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

    override fun generateToken(userId: UserId, username: String, roles: Set<Role>): AccessToken {
        val expirationDate = Date(System.currentTimeMillis() + 60 * 60 * 1000) // 1 hour
        var claim = Jwts.builder()
            .subject(userId.value.toString())
            .expiration(expirationDate)
            .claim("username", username)
        for(role in roles) {
            claim = claim.claim(role.name, true)
        }
        return claim
            .signWith(signingKey)
            .compact()
            .let { tokenValue ->
                AccessToken(
                    userId = userId,
                    userName = username,
                    tokenValue = tokenValue,
                    tokenExpirationDate = expirationDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    roles = roles
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
            val roleUser = claims["USER"] as? Boolean ?: false
            val roleAdmin = claims["ADMIN"] as? Boolean ?: false
            val expirationDate = claims.expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()

            AccessToken(
                userId = userId,
                userName = claims["username"].toString(),
                tokenValue = token,
                tokenExpirationDate = expirationDate,
                roles = buildSet {
                    if (roleUser) add(Role.USER)
                    if (roleAdmin) add(Role.ADMIN)
                }
            )
        } catch (e: Exception) {
            println("Error reading token $token: ${e.message}")
            null
        }
    }
}