package fr.sacane.jmanager.infrastructure.spi.adapters.utils

import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.TokenGenerator
import fr.sacane.jmanager.domain.toUUID
import io.jsonwebtoken.Jwts
import org.springframework.beans.factory.annotation.Value
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.logging.Logger
import javax.crypto.spec.SecretKeySpec

class JwtTokenGenerator(
    @Value("\${auth.secret}")
    private val secret: String,
    private val clock: Clock = Clock.systemUTC(),
): TokenGenerator {

    companion object {
        private val LOGGER = Logger.getLogger(JwtTokenGenerator::class.java.name)
    }

    private val signingKey: SecretKeySpec
        get() {
            val keyBytes: ByteArray = Base64.getDecoder().decode(secret)
            return SecretKeySpec(keyBytes, 0, keyBytes.size, "HmacSHA256")
        }

    override fun generateToken(userId: UserId, username: String, roles: Set<Role>): AccessToken {
        // JWT dates have second precision: truncate so the issue time read back equals the one returned.
        val issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS)
        val expirationDate = Date.from(issuedAt.plus(1, ChronoUnit.HOURS))
        var claim = Jwts.builder()
            .subject(userId.value.toString())
            .issuedAt(Date.from(issuedAt))
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
                    roles = roles,
                    issuedAt = LocalDateTime.ofInstant(issuedAt, ZoneOffset.UTC),
                )
            }

    }

    override fun readToken(token: String): AccessToken? {
        return try {
            val claims = Jwts.parser()
                .clock { Date.from(clock.instant()) }
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .payload

            val userId = UserId(claims.subject.toUUID())
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
                },
                issuedAt = claims.issuedAt?.let { LocalDateTime.ofInstant(it.toInstant(), ZoneOffset.UTC) },
            )
        } catch (e: Exception) {
            LOGGER.warning("Error reading token: ${e.javaClass.simpleName}")
            null
        }
    }
}