package fr.sacane.jmanager.domain.models.transaction.regular

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.BaseTransaction
import java.time.DayOfWeek
import java.time.LocalDate

@JvmInline
value class RegularTransactionId(val value: String)


data class RegularTransaction(
    override var label: String,
    override var amount: Amount,
    override var isIncome: Boolean,
    override var tag: Tag? = null,
    val id: RegularTransactionId,
    val startDate: LocalDate,
    val frequencyProperty: FrequencyProperty,
    val recurrenceRule: RecurrenceRule,
    val associatedBooklets: List<Booklet> = listOf()
): BaseTransaction

/**
 * Sealed interface defining the different recurrence patterns for regular transactions.
 * This approach follows the Open/Closed Principle and makes it easy to add new recurrence types.
 */
sealed interface RecurrenceRule {

    /**
     * Monthly recurrence on a specific day of the month.
     * @property dayOfMonth The day of the month (1-31) when the transaction occurs.
     */
    data class Monthly(val dayOfMonth: Int) : RecurrenceRule {
        init {
            require(dayOfMonth in 1..31) { "Day of month must be between 1 and 31" }
        }
    }

    /**
     * Yearly recurrence on a specific month and day.
     * @property month The month (1-12) when the transaction occurs.
     * @property dayOfMonth The day of the month (1-31) when the transaction occurs.
     */
    data class Yearly(val month: Int, val dayOfMonth: Int) : RecurrenceRule {
        init {
            require(month in 1..12) { "Month must be between 1 and 12" }
            require(dayOfMonth in 1..31) { "Day of month must be between 1 and 31" }
        }
    }

    /**
     * Weekly recurrence on specific days of the week.
     * @property daysOfWeek The set of days when the transaction occurs.
     */
    data class Weekly(val daysOfWeek: Set<DayOfWeek>) : RecurrenceRule {
        init {
            require(daysOfWeek.isNotEmpty()) { "At least one day of week must be specified" }
        }
    }

    /**
     * Daily recurrence.
     */
    data object Daily : RecurrenceRule
}
