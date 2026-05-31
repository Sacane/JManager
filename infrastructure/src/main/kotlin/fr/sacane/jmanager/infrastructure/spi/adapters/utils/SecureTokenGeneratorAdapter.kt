package fr.sacane.jmanager.infrastructure.spi.adapters.utils

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.port.output.SecureTokenGenerator
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64

@Service
@Adapter(Side.INFRASTRUCTURE)
class SecureTokenGeneratorAdapter : SecureTokenGenerator {

    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override fun generate(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return encoder.encodeToString(bytes)
    }
}
