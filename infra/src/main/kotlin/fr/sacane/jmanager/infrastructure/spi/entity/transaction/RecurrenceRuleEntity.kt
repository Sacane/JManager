package fr.sacane.jmanager.infrastructure.spi.entity.transaction

import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import jakarta.persistence.*
import java.time.DayOfWeek

@Entity
@Table(name = "recurrence_rule")
data class RecurrenceRuleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: RecurrenceType,

    @Column(name = "day_of_month")
    val dayOfMonth: Int? = null,

    @Column(name = "month")
    val month: Int? = null,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "recurrence_days_of_week", joinColumns = [JoinColumn(name = "recurrence_rule_id")])
    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    val daysOfWeek: Set<DayOfWeek> = emptySet()
) {
    fun toDomain(): RecurrenceRule {
        return when (type) {
            RecurrenceType.MONTHLY -> RecurrenceRule.Monthly(dayOfMonth!!)
            RecurrenceType.YEARLY -> RecurrenceRule.Yearly(month!!, dayOfMonth!!)
            RecurrenceType.WEEKLY -> RecurrenceRule.Weekly(daysOfWeek)
            RecurrenceType.DAILY -> RecurrenceRule.Daily
        }
    }

    companion object {
        fun fromDomain(rule: RecurrenceRule): RecurrenceRuleEntity {
            return when (rule) {
                is RecurrenceRule.Monthly -> RecurrenceRuleEntity(
                    type = RecurrenceType.MONTHLY,
                    dayOfMonth = rule.dayOfMonth
                )
                is RecurrenceRule.Yearly -> RecurrenceRuleEntity(
                    type = RecurrenceType.YEARLY,
                    month = rule.month,
                    dayOfMonth = rule.dayOfMonth
                )
                is RecurrenceRule.Weekly -> RecurrenceRuleEntity(
                    type = RecurrenceType.WEEKLY,
                    daysOfWeek = rule.daysOfWeek
                )
                is RecurrenceRule.Daily -> RecurrenceRuleEntity(
                    type = RecurrenceType.DAILY
                )
            }
        }
    }
}

enum class RecurrenceType {
    MONTHLY,
    YEARLY,
    WEEKLY,
    DAILY
}

