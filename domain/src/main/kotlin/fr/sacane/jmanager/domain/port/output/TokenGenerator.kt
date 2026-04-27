package fr.sacane.jmanager.domain.port.output

import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.Role
import fr.sacane.jmanager.domain.models.UserId

/**
 * SPI contract for generating and reading access tokens.
 *
 * Implementations encapsulate the token format, signing and verification strategy.
 * The domain uses this SPI to create tokens for authenticated users and to decode tokens
 * when needed (for example in session validation).
 */
interface TokenGenerator {
    /**
     * Generate an access token for the provided user identifier and roles.
     *
     * @param userId Domain user identifier.
     * @param username The username to embed or associate with the token.
     * @param roles Set of roles to encode in the token (defaults to Role.USER).
     * @return An AccessToken value object representing a signed, time-bound token.
     */
    fun generateToken(userId: UserId, username: String, roles: Set<Role> = setOf(Role.USER)): AccessToken

    /**
     * Read and decode a raw token string into an AccessToken domain object.
     * Implementations should return null for invalid or expired tokens.
     *
     * @param token Raw token string to decode.
     * @return Decoded AccessToken if valid, or null if the token cannot be read.
     */
    fun readToken(token: String): AccessToken?
}