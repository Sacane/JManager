package fr.sacane.jmanager.domain.fixture

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

object RegularTransactionFixture {

    fun aMonthlyTransaction(
        id: RegularTransactionId = RegularTransactionId(UUID.randomUUID().toString()),
        label: String = "Monthly transaction",
        amount: Amount = 1000.toAmount(),
        isIncome: Boolean = true,
        tag: Tag? = null,
        startDate: LocalDate = LocalDate.of(2024, 1, 1),
        dayOfMonth: Int = 1,
        frequencyProperty: FrequencyProperty = FrequencyProperty.Forever(),
        associatedBooklets: List<Booklet> = emptyList()
    ) = RegularTransaction(
        label = label,
        amount = amount,
        isIncome = isIncome,
        tag = tag,
        id = id,
        startDate = startDate,
        frequencyProperty = frequencyProperty,
        recurrenceRule = RecurrenceRule.Monthly(dayOfMonth),
        associatedBooklets = associatedBooklets
    )

    fun aWeeklyTransaction(
        id: RegularTransactionId = RegularTransactionId(UUID.randomUUID().toString()),
        label: String = "Weekly transaction",
        amount: Amount = 100.toAmount(),
        isIncome: Boolean = false,
        tag: Tag? = null,
        startDate: LocalDate = LocalDate.of(2024, 1, 1),
        daysOfWeek: Set<DayOfWeek> = setOf(DayOfWeek.MONDAY),
        frequencyProperty: FrequencyProperty = FrequencyProperty.Forever(),
        associatedBooklets: List<Booklet> = emptyList()
    ) = RegularTransaction(
        label = label,
        amount = amount,
        isIncome = isIncome,
        tag = tag,
        id = id,
        startDate = startDate,
        frequencyProperty = frequencyProperty,
        recurrenceRule = RecurrenceRule.Weekly(daysOfWeek),
        associatedBooklets = associatedBooklets
    )

    fun aYearlyTransaction(
        id: RegularTransactionId = RegularTransactionId(UUID.randomUUID().toString()),
        label: String = "Yearly transaction",
        amount: Amount = 500.toAmount(),
        isIncome: Boolean = false,
        tag: Tag? = null,
        startDate: LocalDate = LocalDate.of(2024, 1, 1),
        month: Int = 1,
        dayOfMonth: Int = 1,
        frequencyProperty: FrequencyProperty = FrequencyProperty.Forever(),
        associatedBooklets: List<Booklet> = emptyList()
    ) = RegularTransaction(
        label = label,
        amount = amount,
        isIncome = isIncome,
        tag = tag,
        id = id,
        startDate = startDate,
        frequencyProperty = frequencyProperty,
        recurrenceRule = RecurrenceRule.Yearly(month, dayOfMonth),
        associatedBooklets = associatedBooklets
    )
}
