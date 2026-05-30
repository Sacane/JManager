package fr.sacane.jmanager.application.bus

import fr.sacane.jmanager.domain.models.FeatureFlag
import fr.sacane.jmanager.domain.models.FeatureKey
import fr.sacane.jmanager.domain.port.input.Command
import fr.sacane.jmanager.domain.port.input.FeatureFlagged
import fr.sacane.jmanager.domain.port.output.FeatureFlagRepository
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.success
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class FeatureFlagCommandBusTest {

    private val delegate: LoggingCommandBus = Mockito.mock(LoggingCommandBus::class.java)
    private val featureFlagRepository: FeatureFlagRepository = Mockito.mock(FeatureFlagRepository::class.java)
    private val bus = FeatureFlagCommandBus(delegate, featureFlagRepository)

    private data class FlaggedCommand(
        override val featureKey: FeatureKey = FeatureKey.USER_REGISTRATION,
    ) : Command<String>, FeatureFlagged

    private data class PlainCommand(val value: String = "x") : Command<String>

    @AfterEach
    fun afterEach() {
        Mockito.reset(delegate, featureFlagRepository)
    }

    @Nested
    inner class WhenCommandIsNotFeatureFlagged {

        @Test
        fun `delegates immediately without checking the repository`() {
            val command = PlainCommand()
            val expected = success("ok")
            Mockito.`when`(delegate.dispatch(command)).thenReturn(expected)

            val result = bus.dispatch(command)

            assertThat(result).isSameAs(expected)
            Mockito.verify(delegate).dispatch(command)
            Mockito.verifyNoInteractions(featureFlagRepository)
        }
    }

    @Nested
    inner class WhenCommandIsFeatureFlagged {

        @Test
        fun `returns FEATURE_DISABLED when flag row is absent`() {
            Mockito.`when`(featureFlagRepository.findByKey(FeatureKey.USER_REGISTRATION)).thenReturn(null)
            val command = FlaggedCommand()

            val result = bus.dispatch(command)

            assertThat(result.status).isEqualTo(ResultState.FEATURE_DISABLED)
            assertThat(result.errorInfo?.key).isEqualTo("feature_flag.feature_disabled")
            Mockito.verifyNoInteractions(delegate)
        }

        @Test
        fun `returns FEATURE_DISABLED when flag is explicitly disabled`() {
            Mockito.`when`(featureFlagRepository.findByKey(FeatureKey.USER_REGISTRATION))
                .thenReturn(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = false))
            val command = FlaggedCommand()

            val result = bus.dispatch(command)

            assertThat(result.status).isEqualTo(ResultState.FEATURE_DISABLED)
            Mockito.verifyNoInteractions(delegate)
        }

        @Test
        fun `delegates when flag is enabled`() {
            Mockito.`when`(featureFlagRepository.findByKey(FeatureKey.USER_REGISTRATION))
                .thenReturn(FeatureFlag(FeatureKey.USER_REGISTRATION, enabled = true))
            val command = FlaggedCommand()
            val expected = success("delegated")
            Mockito.`when`(delegate.dispatch(command)).thenReturn(expected)

            val result = bus.dispatch(command)

            assertThat(result).isSameAs(expected)
            Mockito.verify(delegate).dispatch(command)
        }
    }
}
