package fr.sacane.jmanager.domain.port.output

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies
import fr.sacane.jmanager.domain.hexadoc.DomainService
import java.security.SecureRandom

/**
 * SPI contract for hashing and verifying passwords.
 *
 * Implementations provide a secure one-way hash algorithm and a verification routine.
 * This interface is intended for use by domain services that need to persist or validate
 * user credentials without depending on a concrete hashing library.
 */
interface Hasher {
    /**
     * Hashes a plain text password and returns the resulting hash string.
     * Implementations should include salts and follow current best practices for password hashing.
     *
     * @param password Plain text password to hash.
     * @return Encoded password hash to be stored in persistence.
     */
    fun hash(password: String): String

    /**
     * Verifies a plain text password against the stored hash value.
     *
     * @param password Plain text password to verify.
     * @param hash Stored password hash to compare against.
     * @return true when the password matches the hash, false otherwise.
     */
    fun verify(password: String, hash: String): Boolean
}

@DomainService
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