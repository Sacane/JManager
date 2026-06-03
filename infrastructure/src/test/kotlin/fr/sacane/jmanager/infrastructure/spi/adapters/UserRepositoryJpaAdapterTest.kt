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

    @Test
    fun `findByEmailWithEncodedPassword returns user with hashed password for a known email`() {
        userRepositoryJpaAdapter.register("email-user", "hashed-pwd", emptySet(), email = "email-user@example.com")

        val result = userRepositoryJpaAdapter.findByEmailWithEncodedPassword("email-user@example.com")

        assertThat(result).isNotNull
        assertThat(result!!.user.email).isEqualTo("email-user@example.com")
        assertThat(result.password).isEqualTo("hashed-pwd")
    }

    @Test
    fun `findByEmailWithEncodedPassword returns null for an unknown email`() {
        val result = userRepositoryJpaAdapter.findByEmailWithEncodedPassword("ghost@example.com")

        assertThat(result).isNull()
    }

    @Test
    fun `recordConsent should persist all three columns and be readable`() {
        val registered = userRepositoryJpaAdapter.register("consent-record-user", "pwd", emptySet())
        assertThat(registered).isNotNull
        val consentTime = LocalDateTime.of(2026, 1, 1, 10, 0)
        val consent = fr.sacane.jmanager.domain.models.ConsentRecord(consentTime, "1.0", consentTime)

        val result = userRepositoryJpaAdapter.recordConsent(registered!!.id, consent)

        assertThat(result.isSuccess()).isTrue()
        val refreshed = userRepositoryJpaAdapter.findUserById(registered.id)
        assertThat(refreshed?.consent).isNotNull
        assertThat(refreshed!!.consent!!.tosAcceptedAt).isEqualTo(consentTime)
        assertThat(refreshed.consent!!.tosVersion).isEqualTo("1.0")
        assertThat(refreshed.consent!!.privacyAcceptedAt).isEqualTo(consentTime)
    }

    @Test
    fun `recordConsent on unknown user should return USER_NOT_FOUND`() {
        val consent = fr.sacane.jmanager.domain.models.ConsentRecord(
            LocalDateTime.now(), "1.0", LocalDateTime.now()
        )

        val result = userRepositoryJpaAdapter.recordConsent(UserId(UUID.randomUUID()), consent)

        assertThat(result.isFailure()).isTrue()
    }

    @Test
    fun `register with mustChangePassword true persists the flag`() {
        val registered = userRepositoryJpaAdapter.register(
            username = "must-change-user",
            password = "temp-pwd",
            roles = emptySet(),
            email = "must-change@example.com",
            mustChangePassword = true,
        )

        assertThat(registered).isNotNull
        assertThat(registered!!.mustChangePassword).isTrue()
    }

    @Test
    fun `register without mustChangePassword defaults to false`() {
        val registered = userRepositoryJpaAdapter.register("no-force-user", "pwd", emptySet())

        assertThat(registered).isNotNull
        assertThat(registered!!.mustChangePassword).isFalse()
    }

    @Test
    fun `findByIdWithEncodedPassword returns user with password for a known id`() {
        val registered = userRepositoryJpaAdapter.register(
            username = "by-id-user",
            password = "hashed-pwd",
            roles = emptySet(),
        )
        assertThat(registered).isNotNull

        val result = userRepositoryJpaAdapter.findByIdWithEncodedPassword(registered!!.id)

        assertThat(result).isNotNull
        assertThat(result!!.user.username).isEqualTo("by-id-user")
        assertThat(result.password).isEqualTo("hashed-pwd")
    }

    @Test
    fun `findByIdWithEncodedPassword returns null for an unknown id`() {
        val result = userRepositoryJpaAdapter.findByIdWithEncodedPassword(UserId(UUID.randomUUID()))

        assertThat(result).isNull()
    }

    @Test
    fun `updatePassword persists the new hashed password`() {
        val registered = userRepositoryJpaAdapter.register("pwd-update-user", "old-pwd", emptySet())
        assertThat(registered).isNotNull

        val result = userRepositoryJpaAdapter.updatePassword(registered!!.id, "new-pwd", clearMustChange = false)

        assertThat(result.isSuccess()).isTrue()
        val stored = userRepositoryJpaAdapter.findByIdWithEncodedPassword(registered.id)
        assertThat(stored!!.password).isEqualTo("new-pwd")
    }

    @Test
    fun `updatePassword with clearMustChange true resets the mustChangePassword flag`() {
        val registered = userRepositoryJpaAdapter.register(
            username = "force-pwd-user",
            password = "temp-pwd",
            roles = emptySet(),
            mustChangePassword = true,
        )
        assertThat(registered).isNotNull
        assertThat(registered!!.mustChangePassword).isTrue()

        userRepositoryJpaAdapter.updatePassword(registered.id, "new-pwd", clearMustChange = true)

        val refreshed = userRepositoryJpaAdapter.findUserById(registered.id)
        assertThat(refreshed!!.mustChangePassword).isFalse()
    }

    @Test
    fun `updatePassword with clearMustChange false leaves mustChangePassword unchanged`() {
        val registered = userRepositoryJpaAdapter.register(
            username = "keep-flag-user",
            password = "temp-pwd",
            roles = emptySet(),
            mustChangePassword = true,
        )
        assertThat(registered).isNotNull

        userRepositoryJpaAdapter.updatePassword(registered!!.id, "new-pwd", clearMustChange = false)

        val refreshed = userRepositoryJpaAdapter.findUserById(registered.id)
        assertThat(refreshed!!.mustChangePassword).isTrue()
    }

    @Test
    fun `updatePassword on unknown user returns USER_NOT_FOUND`() {
        val result = userRepositoryJpaAdapter.updatePassword(UserId(UUID.randomUUID()), "new-pwd", clearMustChange = false)

        assertThat(result.isFailure()).isTrue()
    }
}
