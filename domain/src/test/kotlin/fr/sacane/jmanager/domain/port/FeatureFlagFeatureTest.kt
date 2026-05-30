package fr.sacane.jmanager.domain.port

import fr.sacane.jmanager.domain.act
import fr.sacane.jmanager.domain.assertSuccess
import fr.sacane.jmanager.domain.assertTrue
import fr.sacane.jmanager.domain.fake.InMemoryFeatureFlagRepository
import fr.sacane.jmanager.domain.models.FeatureFlag
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.input.featureflag.GetAllFeatureFlagsQuery
import fr.sacane.jmanager.domain.port.input.featureflag.GetAllFeatureFlagsService
import fr.sacane.jmanager.domain.port.input.featureflag.GetAllFeatureFlagsUseCase
import fr.sacane.jmanager.domain.port.input.featureflag.IsFeatureEnabledQuery
import fr.sacane.jmanager.domain.port.input.featureflag.IsFeatureEnabledService
import fr.sacane.jmanager.domain.port.input.featureflag.IsFeatureEnabledUseCase
import fr.sacane.jmanager.domain.port.input.featureflag.ToggleFeatureFlagCommand
import fr.sacane.jmanager.domain.port.input.featureflag.ToggleFeatureFlagService
import fr.sacane.jmanager.domain.port.input.featureflag.ToggleFeatureFlagUseCase
import fr.sacane.jmanager.domain.then
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FeatureFlagFeatureTest {

    private val repository = InMemoryFeatureFlagRepository()
    private val isFeatureEnabledUseCase: IsFeatureEnabledUseCase = IsFeatureEnabledService(repository)
    private val getAllFeatureFlagsUseCase: GetAllFeatureFlagsUseCase = GetAllFeatureFlagsService(repository)
    private val toggleFeatureFlagUseCase: ToggleFeatureFlagUseCase = ToggleFeatureFlagService(repository)

    @AfterEach
    fun afterEach() {
        repository.clear()
    }

    @Nested
    inner class IsFeatureEnabledFeatureTest {

        @Test
        fun `unpersisted flag defaults to disabled`() {
            val result = act { isFeatureEnabledUseCase.handle(IsFeatureEnabledQuery(FeatureKey.USER_REGISTRATION)) }

            then(result) { assertTrue { this == false } }
        }

        @Test
        fun `enabled flag returns true`() {
            repository.upsert(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = true))

            val result = act { isFeatureEnabledUseCase.handle(IsFeatureEnabledQuery(FeatureKey.USER_REGISTRATION)) }

            then(result) { assertTrue { this == true } }
        }

        @Test
        fun `explicitly disabled flag returns false`() {
            repository.upsert(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = false))

            val result = act { isFeatureEnabledUseCase.handle(IsFeatureEnabledQuery(FeatureKey.USER_REGISTRATION)) }

            then(result) { assertTrue { this == false } }
        }
    }

    @Nested
    inner class GetAllFeatureFlagsFeatureTest {

        @Test
        fun `returns all known flag keys with no flags persisted`() {
            val result = act { getAllFeatureFlagsUseCase.handle(GetAllFeatureFlagsQuery()) }

            then(result) {
                assertSuccess()
                val flags = mapNotNullOrFailure()!!
                assertEquals(FeatureKey.entries.size, flags.size)
            }
        }

        @Test
        fun `unpersisted flags are returned as disabled`() {
            val result = act { getAllFeatureFlagsUseCase.handle(GetAllFeatureFlagsQuery()) }

            then(result) {
                assertSuccess()
                assertTrue { mapNotNullOrFailure()!!.all { !it.enabled } }
            }
        }

        @Test
        fun `persisted enabled flag is reflected in the list`() {
            repository.upsert(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = true))

            val result = act { getAllFeatureFlagsUseCase.handle(GetAllFeatureFlagsQuery()) }

            then(result) {
                assertSuccess()
                val userRegistration = mapNotNullOrFailure()!!.first { it.key == FeatureKey.USER_REGISTRATION }
                assertTrue { userRegistration.enabled }
            }
        }
    }

    @Nested
    inner class ToggleFeatureFlagFeatureTest {

        @Test
        fun `toggling a flag to enabled persists the change`() {
            val result = act {
                toggleFeatureFlagUseCase.handle(ToggleFeatureFlagCommand(FeatureKey.USER_REGISTRATION, enabled = true))
            }

            then(result) {
                assertSuccess()
                assertTrue { mapNotNullOrFailure()!!.enabled }
            }
        }

        @Test
        fun `toggling an enabled flag to disabled persists the change`() {
            repository.upsert(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = true))

            val result = act {
                toggleFeatureFlagUseCase.handle(ToggleFeatureFlagCommand(FeatureKey.USER_REGISTRATION, enabled = false))
            }

            then(result) {
                assertSuccess()
                assertTrue { !mapNotNullOrFailure()!!.enabled }
            }
        }

        @Test
        fun `isEnabled reflects state after toggle`() {
            act {
                toggleFeatureFlagUseCase.handle(ToggleFeatureFlagCommand(FeatureKey.USER_REGISTRATION, enabled = true))
            }

            val result = act { isFeatureEnabledUseCase.handle(IsFeatureEnabledQuery(FeatureKey.USER_REGISTRATION)) }

            then(result) { assertTrue { this == true } }
        }

        @Test
        fun `toggling is idempotent — enabling an already enabled flag succeeds`() {
            repository.upsert(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = true))

            val result = act {
                toggleFeatureFlagUseCase.handle(ToggleFeatureFlagCommand(FeatureKey.USER_REGISTRATION, enabled = true))
            }

            then(result) {
                assertSuccess()
                assertTrue { mapNotNullOrFailure()!!.enabled }
            }
        }
    }
}
