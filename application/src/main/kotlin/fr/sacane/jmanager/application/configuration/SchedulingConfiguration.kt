package fr.sacane.jmanager.application.configuration

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * Activates Spring's `@Scheduled` task infrastructure for the application.
 *
 * `RetentionProperties` is registered by the infrastructure module's `JpaAutoConfiguration`
 * via `@EnableConfigurationProperties` — no duplication needed here.
 *
 * To disable all scheduled tasks in a given environment (e.g. tests), set each task's
 * cron expression to `"-"` in the corresponding properties file.
 */
@Configuration
@EnableScheduling
class SchedulingConfiguration
