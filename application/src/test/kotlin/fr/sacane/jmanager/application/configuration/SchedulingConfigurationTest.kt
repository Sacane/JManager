package fr.sacane.jmanager.application.configuration

import fr.sacane.jmanager.application.AbstractIntegrationTest
import fr.sacane.jmanager.infrastructure.spi.configuration.RetentionProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor

class SchedulingConfigurationTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var retentionProperties: RetentionProperties

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    // ── Default binding ──────────────────────────────────────────────────────

    @Test
    fun `unconsentedAccountDays binds to 30 from application properties`() {
        // application.properties sets jmanager.retention.unconsented-account-days=30
        // application-test.properties does NOT override this key
        assertThat(retentionProperties.unconsentedAccountDays).isEqualTo(30L)
    }

    @Test
    fun `cron expression is disabled in test environment`() {
        // application-test.properties overrides jmanager.retention.cron=-
        // Proves that @TestPropertySource override propagates to @ConfigurationProperties binding
        assertThat(retentionProperties.cron).isEqualTo("-")
    }

    // ── Scheduling infrastructure ────────────────────────────────────────────

    @Test
    fun `SchedulingConfiguration activates Spring scheduling infrastructure`() {
        // ScheduledAnnotationBeanPostProcessor is the key bean registered by @EnableScheduling.
        // Its presence in the context confirms SchedulingConfiguration was processed.
        assertThat(applicationContext.getBeanNamesForType(ScheduledAnnotationBeanPostProcessor::class.java))
            .isNotEmpty
    }

    // ── Pure unit test — no Spring context cost ───────────────────────────────

    @Test
    fun `RetentionProperties constructor defaults are correct`() {
        // Verifies the data class defaults independently of the Spring binding mechanism.
        // Guards against accidental default value drift.
        val defaults = RetentionProperties()
        assertThat(defaults.unconsentedAccountDays).isEqualTo(30L)
        assertThat(defaults.cron).isEqualTo("0 0 2 * * *")
    }
}
