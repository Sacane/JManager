package fr.sacane.jmanager.domain.models

/**
 * Domain representation of a session authentication token.
 *
 * Encapsulates the raw token string to prevent leaking transport-level
 * string primitives directly into domain port contracts.
 */
@JvmInline
value class SessionToken(val value: String)
