package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.EmailVerificationToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.AbstractIntegrationTest
import fr.sacane.jmanager.infrastructure.spi.entity.UserResource
import fr.sacane.jmanager.infrastructure.spi.repositories.EmailVerificationTokenJpaRepository
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDateTime
import java.util.UUID

@SpringBootTest
class EmailVerificationTokenRepositoryJpaAdapterIT(
    @Autowired private val adapter: EmailVerificationTokenRepositoryJpaAdapter,
    @Autowired private val jpaRepository: EmailVerificationTokenJpaRepository,
    @Autowired private val userPostgresRepository: UserPostgresRepository,
) : AbstractIntegrationTest() {

    private val testUserId = UUID.randomUUID()

    @AfterEach
    fun cleanup() {
        jpaRepository.deleteAll()
        userPostgresRepository.deleteAll()
    }

    private fun persistUser(): UserId {
        val resource = UserResource(username = "test-verify-${testUserId}", email = "test@example.com")
        val saved = userPostgresRepository.save(resource)
        return UserId(saved.idUser)
    }

    private fun token(userId: UserId, raw: String = "test-token-${UUID.randomUUID()}") = EmailVerificationToken(
        token = raw,
        userId = userId,
        expiresAt = LocalDateTime.now().plusHours(24),
    )

    @Test
    fun `saved token can be retrieved by token value`() {
        val userId = persistUser()
        val t = token(userId, "abc123")

        adapter.save(t)
        val found = adapter.findByToken("abc123")

        assertThat(found).isNotNull
        assertThat(found!!.userId).isEqualTo(userId)
        assertThat(found.token).isEqualTo("abc123")
    }

    @Test
    fun `findByToken returns null for an unknown token`() {
        val found = adapter.findByToken("does-not-exist")
        assertThat(found).isNull()
    }

    @Test
    fun `deleteByUserId removes all tokens for the user`() {
        val userId = persistUser()
        adapter.save(token(userId, "tok-1"))
        adapter.save(token(userId, "tok-2"))

        adapter.deleteByUserId(userId)

        assertThat(jpaRepository.findByUserId(userId.value!!)).isEmpty()
    }

    @Test
    fun `deleteByUserId does not affect tokens of other users`() {
        val userId1 = persistUser()
        val resource2 = UserResource(username = "other-user-${UUID.randomUUID()}", email = "other@example.com")
        val saved2 = userPostgresRepository.save(resource2)
        val userId2 = UserId(saved2.idUser)

        adapter.save(token(userId1, "tok-u1"))
        adapter.save(token(userId2, "tok-u2"))

        adapter.deleteByUserId(userId1)

        assertThat(jpaRepository.findByUserId(userId1.value!!)).isEmpty()
        assertThat(jpaRepository.findByUserId(userId2.value!!)).hasSize(1)
    }

    @Test
    fun `verified state survives a round-trip on the user`() {
        val userId = persistUser()
        val resource = userPostgresRepository.findById(userId.value!!).get()
        assertThat(resource.emailVerified).isFalse()

        resource.emailVerified = true
        userPostgresRepository.save(resource)

        val reloaded = userPostgresRepository.findById(userId.value!!).get()
        assertThat(reloaded.emailVerified).isTrue()
    }
}
