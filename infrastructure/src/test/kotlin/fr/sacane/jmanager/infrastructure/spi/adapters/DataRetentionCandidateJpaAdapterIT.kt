package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import fr.sacane.jmanager.infrastructure.spi.adapters.retention.DataRetentionCandidateJpaAdapter
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime

@SpringBootTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
class DataRetentionCandidateJpaAdapterIT(
    @Autowired private val adapter: DataRetentionCandidateJpaAdapter,
    @Autowired private val userPostgresRepository: UserPostgresRepository,
) : AuthenticatedUserTest() {

    private val now = LocalDateTime.now()

    @AfterEach
    fun clear() {
        userPostgresRepository.deleteAll()
    }

    private fun saveUnconsentedUser(username: String, creationDate: LocalDateTime): UserResource {
        return userPostgresRepository.save(
            UserResource(
                username = username,
                password = "hash",
                creationDate = creationDate,
                tosAcceptedAt = null,
                privacyAcceptedAt = null,
            )
        )
    }

    private fun saveConsentedUser(username: String, creationDate: LocalDateTime): UserResource {
        val consentTime = creationDate
        return userPostgresRepository.save(
            UserResource(
                username = username,
                password = "hash",
                creationDate = creationDate,
                tosAcceptedAt = consentTime,
                tosVersion = "1.0",
                privacyAcceptedAt = consentTime,
            )
        )
    }

    @Test
    fun `returns only expired unconsented users`() {
        val cutoff = now.minusDays(30)

        val expired = saveUnconsentedUser("expired-user", now.minusDays(35))
        saveUnconsentedUser("recent-unconsented", now.minusDays(20))
        saveConsentedUser("consented-old", now.minusDays(40))

        val result = adapter.findUnconsentedUsersCreatedBefore(cutoff)

        assertThat(result).hasSize(1)
        assertThat(result.first().value).isEqualTo(expired.idUser)
    }

    @Test
    fun `returns empty list when no expired unconsented users exist`() {
        val cutoff = now.minusDays(30)

        saveConsentedUser("consented-1", now.minusDays(45))
        saveConsentedUser("consented-2", now.minusDays(90))

        val result = adapter.findUnconsentedUsersCreatedBefore(cutoff)

        assertThat(result).isEmpty()
    }

    @Test
    fun `user created exactly at the cutoff is NOT returned (strictly-before semantics)`() {
        val cutoff = now.minusDays(30)

        saveUnconsentedUser("boundary-user", cutoff)

        val result = adapter.findUnconsentedUsersCreatedBefore(cutoff)

        assertThat(result).isEmpty()
    }

    @Test
    fun `returns all expired unconsented users when multiple exist`() {
        val cutoff = now.minusDays(30)

        val u1 = saveUnconsentedUser("expired-1", now.minusDays(31))
        val u2 = saveUnconsentedUser("expired-2", now.minusDays(60))
        val u3 = saveUnconsentedUser("expired-3", now.minusDays(90))
        saveUnconsentedUser("recent", now.minusDays(10))

        val result = adapter.findUnconsentedUsersCreatedBefore(cutoff)

        val returnedIds = result.map { it.value }.toSet()
        assertThat(returnedIds).containsExactlyInAnyOrder(u1.idUser, u2.idUser, u3.idUser)
    }
}
