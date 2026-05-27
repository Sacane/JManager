package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.api.AuthenticatedUserTest
import java.time.LocalDateTime
import fr.sacane.jmanager.infrastructure.spi.repositories.UserPostgresRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.util.UUID

@TestPropertySource(locations = ["classpath:application-test.properties"])
@SpringBootTest
class UserRepositoryJpaAdapterTest(
    @Autowired private val userRepositoryJpaAdapter: UserRepositoryJpaAdapter,
    @Autowired private val userPostgresRepository: UserPostgresRepository
) : AuthenticatedUserTest() {

    @AfterEach
    fun clear(){
        userPostgresRepository.deleteAll()
    }

    @Test
    fun `create should persist user and allow findByPseudonym`(){
        val u = UserWithPassword(user = fr.sacane.jmanager.domain.models.User(username = "u1", email = null, roles = emptySet()), password = "pass")
        val created = userRepositoryJpaAdapter.create(u)
        assertThat(created).isNotNull
        val found = userRepositoryJpaAdapter.findByPseudonym("u1")
        assertThat(found).isNotNull
    }

    @Test
    fun `register should persist user and be returned`(){
        val registered = userRepositoryJpaAdapter.register("u2", "pwd", emptySet())
        assertThat(registered).isNotNull
        val byPseudo = userRepositoryJpaAdapter.findByPseudonymWithEncodedPassword("u2")
        assertThat(byPseudo).isNotNull
        assertThat(byPseudo!!.user.username).isEqualTo("u2")
    }

    @Test
    fun `upsert should not throw and should persist changes`(){
        val r = userRepositoryJpaAdapter.register("u3", "p3", emptySet())
        assertThat(r).isNotNull
        val existing = r!!
        val toUpUser = fr.sacane.jmanager.domain.models.User(id = existing.id, username = "u3-mod", email = existing.email, booklets = existing.booklets, tags = existing.tags, roles = existing.roles)
        // upsert expects domain User
        val up = userRepositoryJpaAdapter.upsert(toUpUser)
        assertThat(up).isNotNull
        val ok = userRepositoryJpaAdapter.findByPseudonym("u3-mod")
        assertThat(ok).isNotNull
    }

    @Test
    fun `findAll should return list`(){
        userRepositoryJpaAdapter.register("u4", "p4", emptySet())
        val all = userRepositoryJpaAdapter.findAll()
        assertThat(all).isNotEmpty
    }

    @Test
    fun `updateProjectionWindowDays should persist setting`() {
        val registered = userRepositoryJpaAdapter.register("u5", "p5", emptySet())
        assertThat(registered).isNotNull

        val userId = registered!!.id.value ?: UUID.randomUUID()
        val updated = userRepositoryJpaAdapter.updateProjectionWindowDays(UserId(userId), 30)
        assertThat(updated).isTrue()

        val refreshed = userRepositoryJpaAdapter.findByPseudonym("u5")
        assertThat(refreshed).isNotNull
        assertThat(refreshed!!.projectionWindowDays).isEqualTo(30)
    }

    @Test
    fun `deleteById should remove user and return success`() {
        val registered = userRepositoryJpaAdapter.register("u6", "p6", emptySet())
        assertThat(registered).isNotNull

        val result = userRepositoryJpaAdapter.deleteById(registered!!.id)

        assertThat(result.isSuccess()).isTrue()
        assertThat(userRepositoryJpaAdapter.findByPseudonym("u6")).isNull()
    }

    @Test
    fun `deleteById on unknown user should return failure`() {
        val result = userRepositoryJpaAdapter.deleteById(UserId(UUID.randomUUID()))

        assertThat(result.isFailure()).isTrue()
    }

    @Test
    fun `register with consent should persist and return consent record`() {
        val consentTime = LocalDateTime.of(2026, 1, 1, 10, 0)

        val registered = userRepositoryJpaAdapter.register(
            username = "consent-user",
            password = "pwd",
            roles = emptySet(),
            tosAcceptedAt = consentTime,
            tosVersion = "1.0",
            privacyAcceptedAt = consentTime,
        )

        assertThat(registered).isNotNull
        assertThat(registered!!.consent).isNotNull
        assertThat(registered.consent!!.tosAcceptedAt).isEqualTo(consentTime)
        assertThat(registered.consent!!.tosVersion).isEqualTo("1.0")
        assertThat(registered.consent!!.privacyAcceptedAt).isEqualTo(consentTime)
    }

    @Test
    fun `register without consent should persist null consent`() {
        val registered = userRepositoryJpaAdapter.register("no-consent-user", "pwd", emptySet())

        assertThat(registered).isNotNull
        assertThat(registered!!.consent).isNull()
    }
}
