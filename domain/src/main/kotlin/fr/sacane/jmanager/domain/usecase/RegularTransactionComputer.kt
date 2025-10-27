package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.spi.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.repository.TransactionRepository
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.UUID

interface RegularTransactionGenerator {
    /**
     * Generates a list of missing previsional transactions for a given booklet and target timeframe
     * based on the provided regular transactions.
     *
     * @param bookletId The booklet ID for which the previsional transactions are generated.
     * @param regularTransactions The list of regular transactions to use for generating missing previsional transactions.
     * @param targetMonth The month for which the previsional transactions are being generated.
     * @param targetYear The year for which the previsional transactions are being generated.
     * @return A list of generated previsional transactions.
     */
    fun generateMissingPrevisionalTransactions(
        bookletId: UUID,
        regularTransactions: List<RegularTransaction>,
        targetMonth: Month,
        targetYear: Int
    ): List<Transaction>
}

@UseCase
class RegularTransactionGeneratorService(
    private val transactionRepository: TransactionRepository,
    private val trackerRepository: RegularTransactionTrackerRepository
): RegularTransactionGenerator {

    override fun generateMissingPrevisionalTransactions(
        bookletId: UUID,
        regularTransactions: List<RegularTransaction>,
        targetMonth: Month,
        targetYear: Int
    ): List<Transaction> {
        val createdTransactions = mutableListOf<Transaction>()

        regularTransactions.forEach { regularTransaction ->
            val regularTxId = regularTransaction.id

            val tracker = trackerRepository.findTracker(regularTxId, bookletId)

            val startDate = if (tracker != null) {
                tracker.lastGeneratedDate.plusMonths(1)
            } else {
                regularTransaction.startDate
            }

            val lastDayOfTargetMonth = YearMonth.of(targetYear, targetMonth).lengthOfMonth()
            val targetDate = LocalDate.of(targetYear, targetMonth, lastDayOfTargetMonth)

            val transactionsToCreate = when(val frequency = regularTransaction.frequencyProperty) {
                is FrequencyProperty.Forever -> generateTransactionsBetween(
                    regularTransaction,
                    startDate,
                    targetDate,
                    bookletId,
                )
                is FrequencyProperty.UntilDate -> generateTransactionsBetween(
                    regularTransaction,
                    startDate,
                    targetDate,
                    bookletId,
                    untilDate = frequency.date
                )
                is FrequencyProperty.SpecificRepetitionTimes -> generateTransactionsBetween(
                    regularTransaction,
                    startDate,
                    targetDate,
                    bookletId,
                    currentMaxNumber = CurrentMaxNumber(
                        tracker?.numberOfGeneratedTransaction ?: 0,
                        frequency.number
                    )
                )
            }

            transactionsToCreate.forEach { transaction ->
                val saved = transactionRepository.save(transaction = transaction, accountId = bookletId)
                if (saved != null) {
                    createdTransactions.add(saved)
                }
            }

            if (transactionsToCreate.isNotEmpty()) {
                val newTracker = RegularTransactionTracker(
                    id = tracker?.id,
                    regularTransactionId = regularTxId,
                    bookletId = bookletId,
                    lastGeneratedDate = targetDate,
                    numberOfGeneratedTransaction = tracker?.numberOfGeneratedTransaction?.plus(transactionsToCreate.size) ?: transactionsToCreate.size
                )
                trackerRepository.upsertTracker(newTracker)
            }
        }

        return createdTransactions
    }

    /**
     * Generates a list of transactions for a given regular transaction within the specified date range,
     * while ensuring that no duplicate transactions are created for the same date.
     *
     * @param regularTransaction the regular transaction containing details for generating transactions
     * @param startDate the start date of the range within which transactions should be generated
     * @param endDate the end date of the range within which transactions should be generated
     * @param bookletId the ID of the booklet associated with the transactions
     * @return a list of generated transactions within the specified date range
     */
    private fun generateTransactionsBetween(
        regularTransaction: RegularTransaction,
        startDate: LocalDate,
        endDate: LocalDate,
        bookletId: UUID,
        untilDate: LocalDate? = null,
        currentMaxNumber: CurrentMaxNumber? = null
    ): List<Transaction> {
        val effectiveEndDate = calculateEffectiveEndDate(endDate, untilDate)

        val currentDate = alignInitialDateForYearlyRecurrence(startDate, regularTransaction)
        if (currentDate.isAfter(effectiveEndDate)) {
            return emptyList()
        }

        return generateTransactionsInLoop(
            regularTransaction,
            currentDate,
            effectiveEndDate,
            bookletId,
            currentMaxNumber
        )
    }

    private fun calculateEffectiveEndDate(endDate: LocalDate, untilDate: LocalDate?): LocalDate {
        return if (untilDate != null && untilDate.isBefore(endDate)) {
            untilDate
        } else {
            endDate
        }
    }

    private fun alignInitialDateForYearlyRecurrence(
        startDate: LocalDate,
        regularTransaction: RegularTransaction
    ): LocalDate {
        val rule = regularTransaction.recurrenceRule
        if (rule !is RecurrenceRule.Yearly) {
            return startDate
        }

        val month = rule.month
        val day = rule.dayOfMonth
        val lengthOfMonth = YearMonth.of(startDate.year, month).lengthOfMonth()
        val dayForMonth = if (day > lengthOfMonth) lengthOfMonth else day
        var first = LocalDate.of(startDate.year, month, dayForMonth)

        if (first.isBefore(startDate)) {
            first = first.plusYears(1)
        }

        return first
    }

    private fun generateTransactionsInLoop(
        regularTransaction: RegularTransaction,
        initialDate: LocalDate,
        effectiveEndDate: LocalDate,
        bookletId: UUID,
        currentMaxNumber: CurrentMaxNumber?
    ): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        var currentDate = initialDate
        var transactionCount = currentMaxNumber?.currentNumber ?: 0

        while (!currentDate.isAfter(effectiveEndDate)) {
            if (hasReachedMaxNumber(currentMaxNumber, transactionCount)) {
                break
            }

            if (shouldCreateTransaction(regularTransaction, currentDate, bookletId)) {
                val transaction = createPrevisionalTransaction(regularTransaction, currentDate)
                transactions.add(transaction)
                transactionCount++
            }

            currentDate = calculateNextOccurrence(currentDate, regularTransaction)
        }

        return transactions
    }

    private fun hasReachedMaxNumber(currentMaxNumber: CurrentMaxNumber?, transactionCount: Int): Boolean {
        return currentMaxNumber != null && transactionCount >= currentMaxNumber.maxNumber
    }

    private fun shouldCreateTransaction(
        regularTransaction: RegularTransaction,
        currentDate: LocalDate,
        bookletId: UUID
    ): Boolean {
        val transactionExists = checkIfTransactionExists(regularTransaction, currentDate, bookletId)
        if (transactionExists) {
            return false
        }

        return when (val rule = regularTransaction.recurrenceRule) {
            is RecurrenceRule.Weekly -> rule.daysOfWeek.contains(currentDate.dayOfWeek)
            else -> true
        }
    }

    data class CurrentMaxNumber(
        val currentNumber: Int,
        val maxNumber: Int
    )

    private fun checkIfTransactionExists(
        regularTransaction: RegularTransaction,
        date: LocalDate,
        bookletId: UUID
    ): Boolean {

        val yearMonth = YearMonth.from(date)
        val monthTransactions = transactionRepository.findTransactionsByBookletYearAndMonth(
            bookletId,
            yearMonth.year,
            yearMonth.month,
        )

        return monthTransactions?.any {
            it.label == regularTransaction.label &&
                    it.date.year == date.year && it.date.month == date.month && it.date.dayOfMonth == date.dayOfMonth
        } ?: false
    }

    private fun createPrevisionalTransaction(
        regularTransaction: RegularTransaction,
        date: LocalDate
    ): Transaction {
        val day = when (val rule = regularTransaction.recurrenceRule) {
            is RecurrenceRule.Monthly -> rule.dayOfMonth
            is RecurrenceRule.Yearly -> rule.dayOfMonth
            is RecurrenceRule.Weekly -> date.dayOfMonth
            is RecurrenceRule.Daily -> date.dayOfMonth
        }

        val month = when (val rule = regularTransaction.recurrenceRule) {
            is RecurrenceRule.Yearly -> rule.month
            else -> date.monthValue
        }

        val monthLength = YearMonth.of(date.year, month).lengthOfMonth()
        val adjustedDay = if (day > monthLength) monthLength else day

        return Transaction(
            id = null,
            label = regularTransaction.label,
            amount = regularTransaction.amount,
            date = LocalDate.of(date.year, month, adjustedDay),
            isPreview = true,
            isIncome = regularTransaction.isIncome,
            regularTransactionId = regularTransaction.id
        )
    }


    private fun calculateNextOccurrence(
        currentDate: LocalDate,
        regularTransaction: RegularTransaction
    ): LocalDate {
        return when (regularTransaction.recurrenceRule) {
            is RecurrenceRule.Monthly -> currentDate.plusMonths(1)
            is RecurrenceRule.Yearly -> currentDate.plusYears(1)
            is RecurrenceRule.Weekly -> currentDate.plusDays(1)
            is RecurrenceRule.Daily -> currentDate.plusDays(1)
        }
    }
}