package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.TokenGenerator
import org.springframework.stereotype.Component

@Component
class JwtTokenGenerator: TokenGenerator {
    override fun generateToken(userId: UserId, role: Role): AccessToken {
        TODO("Not yet implemented")
    }

    override fun readToken(token: String): AccessToken? {
        TODO("Not yet implemented")
    }
}