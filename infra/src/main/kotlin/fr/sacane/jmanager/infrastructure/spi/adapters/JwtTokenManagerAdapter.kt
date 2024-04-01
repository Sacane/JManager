package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.port.spi.TokenManager
import org.springframework.stereotype.Service

@Service
@Adapter(Side.DATASOURCE)
class JwtTokenManagerAdapter : TokenManager {
    override fun generateToken(): AccessToken {
        TODO("Not yet implemented")
    }

    override fun checkAuthenticate(to: AccessToken, from: AccessToken): Boolean {
        TODO("Not yet implemented")
    }
}