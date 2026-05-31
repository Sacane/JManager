package fr.sacane.jmanager.domain.port.output

import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side

/** SPI contract for generating cryptographically secure, URL-safe random tokens. */
@Port(Side.INFRASTRUCTURE)
interface SecureTokenGenerator {
    fun generate(): String
}
