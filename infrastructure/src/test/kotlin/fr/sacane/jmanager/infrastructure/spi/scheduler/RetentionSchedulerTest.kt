package fr.sacane.jmanager.infrastructure.spi.scheduler

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.LoggerFactory
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

    // Accounts left behind are a GDPR breach in waiting: the run must say so where errors are read.
    @Test
    fun `shouldReportUndeletedAccountsAtErrorLevel_whenSomeDeletionsFail`() {
        val purge: PurgeExpiredDataUseCase = mock {
            on { purge(any(), any()) } doReturn PurgeSummary(deletedCount = 2, failedCount = 1)
        }
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        val logger = LoggerFactory.getLogger(RetentionScheduler::class.java) as Logger
        logger.addAppender(appender)

        try {
            RetentionScheduler(purge, RetentionProperties(), mock(), Clock.systemUTC()).run()
        } finally {
            logger.detachAppender(appender)
        }

        assertThat(appender.list.filter { it.level == Level.ERROR }.map { it.formattedMessage })
            .containsExactly("Retention purge could not delete 1 account(s); they will be retried on the next run")
    }
}
