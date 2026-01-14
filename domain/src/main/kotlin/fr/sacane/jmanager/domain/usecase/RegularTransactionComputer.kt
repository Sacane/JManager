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

    /**
     * Calculates virtual transactions from regular transactions for a date range without persisting them.
     * This is used for calculating provisional balance without generating physical transactions.
     *
     * @param regularTransactions The list of regular transactions to compute.
     * @param startMonth The starting month of the calculation.
     * @param startYear The starting year of the calculation.
     * @param endMonth The ending month of the calculation.
     * @param endYear The ending year of the calculation.
     * @return A list of virtual transactions that would occur in the specified date range.
     */
    fun calculateVirtualTransactions(
        regularTransactions: List<RegularTransaction>,
        startMonth: Month,
        startYear: Int,
        endMonth: Month,
        endYear: Int
    ): List<Transaction>

    /**
     * Regenerates missing previsional transactions for a specific month by detecting gaps
     * and creating the missing transactions.
     *
     * @param bookletId The booklet ID for which to regenerate transactions.
     * @param regularTransactions The list of regular transactions to check.
     * @param targetMonth The month to regenerate transactions for.
     * @param targetYear The year to regenerate transactions for.
     * @return A list of regenerated previsional transactions.
     */
    fun regenerateMissingPrevisionalTransactions(
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

            val firstDayOfTargetMonth = LocalDate.of(targetYear, targetMonth, 1)
            val lastDayOfTargetMonth = YearMonth.of(targetYear, targetMonth).lengthOfMonth()
            val targetEndDate = LocalDate.of(targetYear, targetMonth, lastDayOfTargetMonth)

            if (targetEndDate.isBefore(regularTransaction.startDate)) {
                return@forEach
            }

            val transactionsToCreate = when(val frequency = regularTransaction.frequencyProperty) {
                is FrequencyProperty.Forever -> generateTransactionsBetween(
                    regularTransaction,
                    firstDayOfTargetMonth,
                    targetEndDate,
                    bookletId,
                )
                is FrequencyProperty.UntilDate -> generateTransactionsBetween(
                    regularTransaction,
                    firstDayOfTargetMonth,
                    targetEndDate,
                    bookletId,
                    untilDate = frequency.date
                )
                is FrequencyProperty.SpecificRepetitionTimes -> {
                    // For specific repetitions, we need to track the count globally
                    val currentCount = tracker?.numberOfGeneratedTransaction ?: 0
                    generateTransactionsBetween(
                        regularTransaction,
                        firstDayOfTargetMonth,
                        targetEndDate,
                        bookletId,
                        currentMaxNumber = CurrentMaxNumber(currentCount, frequency.number)
                    )
                }
            }

            // Save each transaction to the repository
            transactionsToCreate.forEach { transaction ->
                val saved = transactionRepository.save(bookletId, transaction)
                if (saved != null) {
                    createdTransactions.add(saved)
                }
            }

            // Update tracker if we have created transactions
            if (transactionsToCreate.isNotEmpty()) {
                val newTracker = RegularTransactionTracker(
                    id = tracker?.id,
                    regularTransactionId = regularTxId,
                    bookletId = bookletId,
                    lastGeneratedDate = targetEndDate,
                    numberOfGeneratedTransaction = tracker?.numberOfGeneratedTransaction?.plus(transactionsToCreate.size) ?: transactionsToCreate.size
                )
                trackerRepository.upsertTracker(newTracker)
            }
        }

        return createdTransactions
    }

    override fun calculateVirtualTransactions(
        regularTransactions: List<RegularTransaction>,
        startMonth: Month,
        startYear: Int,
        endMonth: Month,
        endYear: Int
    ): List<Transaction> {
        val virtualTransactions = mutableListOf<Transaction>()

        val startDate = LocalDate.of(startYear, startMonth, 1)
        val lastDayOfEndMonth = YearMonth.of(endYear, endMonth).lengthOfMonth()
        val endDate = LocalDate.of(endYear, endMonth, lastDayOfEndMonth)

        regularTransactions.forEach { regularTransaction ->
            // Skip if the regular transaction hasn't started yet
            if (regularTransaction.startDate.isAfter(endDate)) {
                return@forEach
            }

            val effectiveStartDate = if (regularTransaction.startDate.isAfter(startDate)) {
                regularTransaction.startDate
            } else {
                startDate
            }

            val transactions = when(val frequency = regularTransaction.frequencyProperty) {
                is FrequencyProperty.Forever -> generateVirtualTransactionsBetween(
                    regularTransaction,
                    effectiveStartDate,
                    endDate
                )
                is FrequencyProperty.UntilDate -> {
                    val effectiveEndDate = if (frequency.date.isBefore(endDate)) frequency.date else endDate
                    generateVirtualTransactionsBetween(
                        regularTransaction,
                        effectiveStartDate,
                        effectiveEndDate
                    )
                }
                is FrequencyProperty.SpecificRepetitionTimes -> generateVirtualTransactionsBetween(
                    regularTransaction,
                    effectiveStartDate,
                    endDate,
                    maxCount = frequency.number
                )
            }

            virtualTransactions.addAll(transactions)
        }

        return virtualTransactions
    }

    override fun regenerateMissingPrevisionalTransactions(
        bookletId: UUID,
        regularTransactions: List<RegularTransaction>,
        targetMonth: Month,
        targetYear: Int
    ): List<Transaction> {
        // This method simply delegates to generateMissingPrevisionalTransactions
        // which already handles checking for existing transactions and only creating missing ones
        return generateMissingPrevisionalTransactions(
            bookletId,
            regularTransactions,
            targetMonth,
            targetYear
        )
    }


    /**
     * Generate virtual transactions without checking if they exist in the database.
     * Used for provisional balance calculation only.
     */
    private fun generateVirtualTransactionsBetween(
        regularTransaction: RegularTransaction,
        startDate: LocalDate,
        endDate: LocalDate,
        maxCount: Int? = null
    ): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        var currentDate = alignInitialDateForYearlyRecurrence(startDate, regularTransaction)
        var count = 0

        if (currentDate.isAfter(endDate)) {
            return emptyList()
        }

        while (!currentDate.isAfter(endDate)) {
            if (maxCount != null && count >= maxCount) {
                break
            }

            val shouldCreate = when (val rule = regularTransaction.recurrenceRule) {
                is RecurrenceRule.Weekly -> rule.daysOfWeek.contains(currentDate.dayOfWeek)
                else -> true
            }

            if (shouldCreate) {
                val transaction = createPrevisionalTransaction(regularTransaction, currentDate)
                transactions.add(transaction)
                count++
            }

            currentDate = calculateNextOccurrence(currentDate, regularTransaction)
        }

        return transactions
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

        return monthTransactions?.any { transaction ->
            // Transaction must be previsional
            if (!transaction.isPreview) return@any false

            // Transaction must have the same date
            if (!transaction.date.isEqual(date)) return@any false

            // STRICT CHECK: If both have regularTransactionId, they must match
            // This is the primary and most reliable way to detect duplicates
            if (transaction.regularTransactionId != null && regularTransaction.id != null) {
                return@any transaction.regularTransactionId == regularTransaction.id
            }

            // If transaction has regularTransactionId but doesn't match, it's a different regular transaction
            if (transaction.regularTransactionId != null) {
                return@any false
            }

            // Legacy fallback: only for old transactions without regularTransactionId
            // Check by label and amount if regularTransactionId is not set
            return@any transaction.label == regularTransaction.label &&
                       transaction.amount == regularTransaction.amount
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