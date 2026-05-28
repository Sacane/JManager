package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.InMemoryDatabase
import fr.sacane.jmanager.domain.fake.InMemoryDataRetentionCandidatePort
import fr.sacane.jmanager.domain.fake.InMemoryUserRepository
import fr.sacane.jmanager.domain.models.ConsentRecord
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.retention.PurgeSummary
import fr.sacane.jmanager.domain.models.retention.RetentionDays
import fr.sacane.jmanager.domain.models.retention.RetentionPolicy
import fr.sacane.jmanager.domain.port.input.retention.PurgeExpiredDataService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PurgeExpiredDataUseCaseTest {

    private val db = InMemoryDatabase()
    private val candidatePort = InMemoryDataRetentionCandidatePort(db)
    private val userRepository = InMemoryUserRepository(db)
    private val service = PurgeExpiredDataService(candidatePort, userRepository)

    private val policy = RetentionPolicy(RetentionDays(30))
    private val now = LocalDateTime.of(2026, 1, 31, 0, 0)

    @BeforeEach
    fun setup() {
        db.clearUsers()
    }

    private fun unconsentedUserCreatedAt(creationDate: LocalDateTime): UserWithPassword {
        val user = User(
            id = UserId(UUID.randomUUID()),
            username = "user-${UUID.randomUUID()}",
            email = null,
            creationDate = creationDate,
            consent = null,
        )
        return UserWithPassword(user, "hash")
    }

    private fun consentedUserCreatedAt(creationDate: LocalDateTime): UserWithPassword {
        val user = User(
            id = UserId(UUID.randomUUID()),
            username = "user-${UUID.randomUUID()}",
            email = null,
            creationDate = creationDate,
            consent = ConsentRecord(
                tosAcceptedAt = creationDate,
                tosVersion = "1.0",
                privacyAcceptedAt = creationDate,
            ),
        )
        return UserWithPassword(user, "hash")
    }

    @Test
    fun `unconsented account older than retention threshold is deleted`() {
        val expired = unconsentedUserCreatedAt(now.minusDays(31))
        db.initUsers(listOf(expired))

        val summary = service.purge(policy, now)

        assertEquals(PurgeSummary(deletedCount = 1), summary)
        assertNull(db.users[expired.user.id])
    }

    @Test
    fun `unconsented account created 29 days ago is not purged`() {
        val recent = unconsentedUserCreatedAt(now.minusDays(29))
        db.initUsers(listOf(recent))

        val summary = service.purge(policy, now)

        assertEquals(PurgeSummary(deletedCount = 0), summary)
        assertEquals(recent.user.id, db.users[recent.user.id]?.user?.id)
    }

    @Test
    fun `consented account older than threshold is never purged`() {
        val old = consentedUserCreatedAt(now.minusDays(60))
        db.initUsers(listOf(old))

        val summary = service.purge(policy, now)

        assertEquals(PurgeSummary(deletedCount = 0), summary)
        assertEquals(old.user.id, db.users[old.user.id]?.user?.id)
    }

    @Test
    fun `purge is a no-op when all accounts have consented`() {
        db.initUsers(listOf(
            consentedUserCreatedAt(now.minusDays(45)),
            consentedUserCreatedAt(now.minusDays(90)),
        ))

        val summary = service.purge(policy, now)

        assertEquals(PurgeSummary(deletedCount = 0), summary)
    }

    @Test
    fun `all expired unconsented accounts are deleted when multiple exist`() {
        val expired1 = unconsentedUserCreatedAt(now.minusDays(35))
        val expired2 = unconsentedUserCreatedAt(now.minusDays(60))
        val expired3 = unconsentedUserCreatedAt(now.minusDays(90))
        db.initUsers(listOf(expired1, expired2, expired3))

        val summary = service.purge(policy, now)

        assertEquals(PurgeSummary(deletedCount = 3), summary)
        assertNull(db.users[expired1.user.id])
        assertNull(db.users[expired2.user.id])
        assertNull(db.users[expired3.user.id])
    }
}
