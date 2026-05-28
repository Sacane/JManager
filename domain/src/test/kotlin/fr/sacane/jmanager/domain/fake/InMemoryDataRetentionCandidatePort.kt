package fr.sacane.jmanager.domain.fake

import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.output.DataRetentionCandidatePort
import java.time.LocalDateTime

/**
 * In-memory fake for [DataRetentionCandidatePort].
 * Filters the shared [InMemoryDatabase] to return users with no consent
 * whose account was created strictly before [cutoff].
 */
class InMemoryDataRetentionCandidatePort(
    private val inMemoryDatabase: InMemoryDatabase,
) : DataRetentionCandidatePort {

    override fun findUnconsentedUsersCreatedBefore(cutoff: LocalDateTime): List<UserId> =
        inMemoryDatabase.users.values
            .filter { it.user.consent == null && it.user.creationDate.isBefore(cutoff) }
            .map { it.user.id }
}
