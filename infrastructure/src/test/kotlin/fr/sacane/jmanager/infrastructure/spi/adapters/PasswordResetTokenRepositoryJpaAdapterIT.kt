package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.PasswordResetToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.AbstractIntegrationTest
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.PasswordResetTokenJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime
import java.util.UUID

private val NOW: LocalDateTime = LocalDateTime.of(2026, 10, 8, 9, 0)

@SpringBootTest
class PasswordResetTokenRepositoryJpaAdapterIT(
    @Autowired private val adapter: PasswordResetTokenRepositoryJpaAdapter,
    @Autowired private val jpaRepository: PasswordResetTokenJpaRepository,
    @Autowired private val userPostgresRepository: UserPostgresRepository,
) : AbstractIntegrationTest() {

    @AfterEach
    fun cleanup() {
        jpaRepository.deleteAll()
        userPostgresRepository.deleteAll()
    }

    @Test
    fun `shouldFindASavedTokenByItsDigest`() {
        val userId = persistUser()
        val token = token(userId, raw = "raw-token", expiresAt = NOW.plusMinutes(30))

        adapter.save(token)

        assertThat(adapter.findByTokenHash(PasswordResetToken.digestOf("raw-token"))).isEqualTo(token)
        assertThat(adapter.findByTokenHash(PasswordResetToken.digestOf("other"))).isNull()
    }

    @Test
    fun `shouldKeepOnlyTheNewToken_whenTheUsersTokensAreDeletedFirst`() {
        val userId = persistUser()
        adapter.save(token(userId, raw = "first"))

        adapter.deleteByUserId(userId)
        adapter.save(token(userId, raw = "second"))

        assertThat(jpaRepository.findAll().map { it.tokenHash }).containsExactly(PasswordResetToken.digestOf("second"))
    }

    // One token per user is a schema rule, not only a habit of the code.
    @Test
    fun `shouldRefuseASecondTokenForTheSameUser`() {
        val userId = persistUser()
        adapter.save(token(userId, raw = "first"))

        assertThatThrownBy { adapter.save(token(userId, raw = "second")) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    fun `shouldDeleteTheTokens_whenTheirUserIsDeleted`() {
        val userId = persistUser()
        adapter.save(token(userId, raw = "raw-token"))

        userPostgresRepository.deleteById(userId.value!!)

        assertThat(jpaRepository.count()).isZero()
    }

    @Test
    fun `shouldPurgeOnlyExpiredTokens`() {
        val expired = token(persistUser(), raw = "expired", expiresAt = NOW.minusSeconds(1))
        val live = token(persistUser(), raw = "live", expiresAt = NOW.plusMinutes(10))
        adapter.save(expired)
        adapter.save(live)

        val deleted = adapter.deleteExpired(NOW)

        assertThat(deleted).isEqualTo(1)
        assertThat(jpaRepository.findAll().map { it.tokenHash }).containsExactly(live.tokenHash)
    }

    private fun persistUser(): UserId {
        val id = UUID.randomUUID()
        val saved = userPostgresRepository.save(UserResource(username = "reset-$id", email = "reset-$id@example.com"))
        return UserId(saved.idUser)
    }

    private fun token(userId: UserId, raw: String, expiresAt: LocalDateTime = NOW.plusMinutes(30)) =
        PasswordResetToken(PasswordResetToken.digestOf(raw), userId, expiresAt)
}
