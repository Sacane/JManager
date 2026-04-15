package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

class RecurrenceRuleEntityTest {

    @Test
    fun `fromDomain and toDomain should map Monthly correctly`() {
        val domain = RecurrenceRule.Monthly(15)
        val entity = RecurrenceRuleEntity.fromDomain(domain)

        assertThat(entity.type).isEqualTo(RecurrenceType.MONTHLY)
        assertThat(entity.dayOfMonth).isEqualTo(15)
        assertThat(entity.month).isNull()
        assertThat(entity.daysOfWeek).isEmpty()

        val back = entity.toDomain()
        assertThat(back).isInstanceOf(RecurrenceRule.Monthly::class.java)
        assertThat((back as RecurrenceRule.Monthly).dayOfMonth).isEqualTo(15)
    }

    @Test
    fun `fromDomain and toDomain should map Yearly correctly`() {
        val domain = RecurrenceRule.Yearly(12, 31)
        val entity = RecurrenceRuleEntity.fromDomain(domain)

        assertThat(entity.type).isEqualTo(RecurrenceType.YEARLY)
        assertThat(entity.month).isEqualTo(12)
        assertThat(entity.dayOfMonth).isEqualTo(31)
        assertThat(entity.daysOfWeek).isEmpty()

        val back = entity.toDomain()
        assertThat(back).isInstanceOf(RecurrenceRule.Yearly::class.java)
        val yearly = back as RecurrenceRule.Yearly
        assertThat(yearly.month).isEqualTo(12)
        assertThat(yearly.dayOfMonth).isEqualTo(31)
    }

    @Test
    fun `fromDomain and toDomain should map Weekly correctly`() {
        val domain = RecurrenceRule.Weekly(setOf(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
        val entity = RecurrenceRuleEntity.fromDomain(domain)

        assertThat(entity.type).isEqualTo(RecurrenceType.WEEKLY)
        assertThat(entity.daysOfWeek).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
        assertThat(entity.dayOfMonth).isNull()
        assertThat(entity.month).isNull()

        val back = entity.toDomain()
        assertThat(back).isInstanceOf(RecurrenceRule.Weekly::class.java)
        val weekly = back as RecurrenceRule.Weekly
        assertThat(weekly.daysOfWeek).containsExactlyInAnyOrder(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
    }

    @Test
    fun `fromDomain and toDomain should map Daily correctly`() {
        val domain = RecurrenceRule.Daily
        val entity = RecurrenceRuleEntity.fromDomain(domain)

        assertThat(entity.type).isEqualTo(RecurrenceType.DAILY)
        assertThat(entity.dayOfMonth).isNull()
        assertThat(entity.month).isNull()
        assertThat(entity.daysOfWeek).isEmpty()

        val back = entity.toDomain()
        assertThat(back).isSameAs(RecurrenceRule.Daily)
    }
}

