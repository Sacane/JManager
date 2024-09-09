package fr.sacane.jmanager.domain.port.spi

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies
import fr.sacane.jmanager.domain.port.spi.DefaultHasher.STRENGTH
import java.security.SecureRandom

interface Hasher {
    fun hash(password: String): String
    fun verify(password: String, hash: String): Boolean
}

object DefaultHasher: Hasher {

    private val version = BCrypt.Version.VERSION_2B
    private val strategy = LongPasswordStrategies.hashSha512(version)
    private val hasher = BCrypt.with(version, SecureRandom(), strategy)
    private val verifier = BCrypt.verifyer(version, strategy)

    private const val STRENGTH = 12

    override fun hash(password: String): String =
        hasher.hashToString(STRENGTH, password.toCharArray())

    override fun verify(password: String, hash: String): Boolean =
        verifier.verify(password.toCharArray(), hash.toCharArray()).verified
}