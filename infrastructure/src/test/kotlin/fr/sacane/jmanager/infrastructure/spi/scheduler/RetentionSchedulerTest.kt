package fr.sacane.jmanager.infrastructure.spi.scheduler

import fr.sacane.jmanager.domain.models.retention.PurgeSummary
import fr.sacane.jmanager.domain.port.input.retention.PurgeExpiredDataUseCase
import fr.sacane.jmanager.domain.port.output.repository.PasswordResetTokenRepository
import fr.sacane.jmanager.infrastructure.spi.configuration.RetentionProperties
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class RetentionSchedulerTest {

    // Reset tokens expire on the UTC clock the domain issued them with.
    @Test
    fun `shouldPurgeExpiredPasswordResetTokensOnTheUtcClock_whenTheNightlyRunFires`() {
        val clock = Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC)
        val purge: PurgeExpiredDataUseCase = mock { on { purge(any(), any()) } doReturn PurgeSummary(0) }
        val tokens: PasswordResetTokenRepository = mock()

        RetentionScheduler(purge, RetentionProperties(), tokens, clock).run()

        verify(tokens).deleteExpired(LocalDateTime.of(2026, 10, 8, 0, 0))
    }
}
