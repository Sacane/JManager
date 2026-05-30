package fr.sacane.jmanager.infrastructure.spi.adapters

import fr.sacane.jmanager.domain.models.FeatureFlag
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.infrastructure.AbstractIntegrationTest
import fr.sacane.jmanager.infrastructure.spi.configuration.FeatureFlagSeeder
import fr.sacane.jmanager.infrastructure.spi.entity.FeatureFlagEntity
import fr.sacane.jmanager.infrastructure.spi.repositories.FeatureFlagJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class FeatureFlagRepositoryJpaAdapterIT(
    @Autowired private val adapter: FeatureFlagRepositoryJpaAdapter,
    @Autowired private val jpaRepository: FeatureFlagJpaRepository,
    @Autowired private val seeder: FeatureFlagSeeder,
) : AbstractIntegrationTest() {

    @BeforeEach
    fun clear() {
        jpaRepository.deleteAll()
    }

    // ── Adapter: findByKey ─────────────────────────────────────────────────

    @Test
    fun `findByKey returns null for a flag that has never been persisted`() {
        val result = adapter.findByKey(FeatureKey.USER_REGISTRATION)

        assertThat(result).isNull()
    }

    @Test
    fun `findByKey returns the flag with its persisted enabled state`() {
        jpaRepository.save(FeatureFlagEntity(key = FeatureKey.USER_REGISTRATION.name, enabled = true))

        val result = adapter.findByKey(FeatureKey.USER_REGISTRATION)

        assertThat(result).isEqualTo(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = true))
    }

    @Test
    fun `findByKey returns disabled flag when persisted as disabled`() {
        jpaRepository.save(FeatureFlagEntity(key = FeatureKey.USER_REGISTRATION.name, enabled = false))

        val result = adapter.findByKey(FeatureKey.USER_REGISTRATION)

        assertThat(result).isEqualTo(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = false))
    }

    // ── Adapter: upsert ────────────────────────────────────────────────────

    @Test
    fun `upsert inserts a new flag and returns it`() {
        val flag = FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = true)

        val result = adapter.upsert(flag)

        assertThat(result).isEqualTo(flag)
        assertThat(jpaRepository.findById(FeatureKey.USER_REGISTRATION.name).get().enabled).isTrue()
    }

    @Test
    fun `upsert updates an existing flag without creating a duplicate`() {
        jpaRepository.save(FeatureFlagEntity(key = FeatureKey.USER_REGISTRATION.name, enabled = false))

        adapter.upsert(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = true))

        val all = jpaRepository.findAll().filter { it.key == FeatureKey.USER_REGISTRATION.name }
        assertThat(all).hasSize(1)
        assertThat(all.first().enabled).isTrue()
    }

    @Test
    fun `upsert is idempotent when called twice with the same state`() {
        val flag = FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = true)

        adapter.upsert(flag)
        val result = adapter.upsert(flag)

        assertThat(result).isEqualTo(flag)
        assertThat(jpaRepository.count()).isEqualTo(1L)
    }

    // ── Adapter: findAll ───────────────────────────────────────────────────

    @Test
    fun `findAll returns empty list when no flags are persisted`() {
        val result = adapter.findAll()

        assertThat(result).isEmpty()
    }

    @Test
    fun `findAll returns all persisted flags`() {
        jpaRepository.save(FeatureFlagEntity(key = FeatureKey.USER_REGISTRATION.name, enabled = true))

        val result = adapter.findAll()

        assertThat(result).hasSize(1)
        assertThat(result.map { it.key }).containsExactly(FeatureKey.USER_REGISTRATION)
    }

    // ── Seeder ─────────────────────────────────────────────────────────────

    @Nested
    inner class SeederTests {

        @Test
        fun `seeder creates one disabled row per known FeatureKey`() {
            seeder.seed()

            val rows = jpaRepository.findAll()
            assertThat(rows).hasSize(FeatureKey.entries.size)
            assertThat(rows.all { !it.enabled }).isTrue()
        }

        @Test
        fun `seeder does not overwrite an admin-enabled flag`() {
            jpaRepository.save(FeatureFlagEntity(key = FeatureKey.USER_REGISTRATION.name, enabled = true))

            seeder.seed()

            val flag = jpaRepository.findById(FeatureKey.USER_REGISTRATION.name).get()
            assertThat(flag.enabled).isTrue()
        }

        @Test
        fun `seeder is idempotent — calling it twice does not duplicate rows`() {
            seeder.seed()
            seeder.seed()

            assertThat(jpaRepository.count()).isEqualTo(FeatureKey.entries.size.toLong())
        }

        @Test
        fun `seeder removes orphaned flags that are no longer in the enum`() {
            jpaRepository.save(FeatureFlagEntity(key = "OBSOLETE_FLAG", enabled = true))

            seeder.seed()

            assertThat(jpaRepository.existsById("OBSOLETE_FLAG")).isFalse()
            assertThat(jpaRepository.count()).isEqualTo(FeatureKey.entries.size.toLong())
        }
    }
}
