package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.FrequencyProperty
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.output.repository.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.output.repository.TransactionRepository
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
     * @param startDateBound Optional lower bound within the target month. Defaults to the first day of
     *        the month when null. Allows restricting generation to a custom cycle sub-range (e.g. a
     *        user-configured period that starts on the 28th of the previous calendar month).
     * @param endDateBound Optional upper bound within the target month. Defaults to the last day of
     *        the month when null. Allows restricting generation to a custom cycle sub-range (e.g. a
     *        user-configured period that ends on the 27th of the calendar month).
     * @return A list of generated previsional transactions.
     */
    fun generateMissingPrevisionalTransactions(
        bookletId: UUID,
        regularTransactions: List<RegularTransaction>,
        targetMonth: Month,
        targetYear: Int,
        startDateBound: LocalDate? = null,
        endDateBound: LocalDate? = null
    ): List<Transaction>

    /**
     * Calculates virtual transactions from regular transactions for a date range without persisting them.
     * This is used for calculating provisional balance without generating physical transactions.
     *
     * @param bookletId The booklet ID to check for excluded months.
     * @param regularTransactions The list of regular transactions to compute.
     * @param startMonth The starting month of the calculation.
     * @param startYear The starting year of the calculation.
     * @param endMonth The ending month of the calculation.
     * @param endYear The ending year of the calculation.
     * @param existingPhysicalTransactions Physical transactions already present in the booklet for the
     *        date range. Virtual occurrences whose (regularTransactionId, date) key matches an existing
     *        physical transaction (whether preview or confirmed) are excluded to avoid double-counting.
     * @return A list of virtual transactions that would occur in the specified date range,
     *         deduplicated against existingPhysicalTransactions.
     */
    fun calculateVirtualTransactions(
        bookletId: UUID,
        regularTransactions: List<RegularTransaction>,
        startMonth: Month,
        startYear: Int,
        endMonth: Month,
        endYear: Int,
        existingPhysicalTransactions: List<Transaction> = emptyList()
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
        targetYear: Int,
        startDateBound: LocalDate?,
        endDateBound: LocalDate?
    ): List<Transaction> {
        val createdTransactions = mutableListOf<Transaction>()

        regularTransactions.forEach { regularTransaction ->
            val regularTxId = regularTransaction.id

            val tracker = trackerRepository.findTracker(regularTxId, bookletId)

            // Check if this month is excluded
            val targetYearMonth = YearMonth.of(targetYear, targetMonth)
            if (tracker?.excludedMonths?.contains(targetYearMonth) == true) {
                return@forEach
            }

            val calendarMonthStart = LocalDate.of(targetYear, targetMonth, 1)
            val calendarMonthEnd = LocalDate.of(targetYear, targetMonth, YearMonth.of(targetYear, targetMonth).lengthOfMonth())

            if (calendarMonthEnd.isBefore(regularTransaction.startDate)) {
                return@forEach
            }

            val rawTransactions = when(val frequency = regularTransaction.frequencyProperty) {
                is FrequencyProperty.Forever -> generateTransactionsBetween(
                    regularTransaction,
                    calendarMonthStart,
                    calendarMonthEnd,
                    bookletId,
                )
                is FrequencyProperty.UntilDate -> generateTransactionsBetween(
                    regularTransaction,
                    calendarMonthStart,
                    calendarMonthEnd,
                    bookletId,
                    untilDate = frequency.date
                )
                is FrequencyProperty.SpecificRepetitionTimes -> {
                    // For specific repetitions, we need to track the count globally
                    val currentCount = tracker?.numberOfGeneratedTransaction ?: 0
                    generateTransactionsBetween(
                        regularTransaction,
                        calendarMonthStart,
                        calendarMonthEnd,
                        bookletId,
                        currentMaxNumber = CurrentMaxNumber(currentCount, frequency.number)
                    )
                }
            }

            // Apply cycle boundary filter when explicit bounds are provided.
            // The generation loop always covers the full calendar month so that Monthly/Yearly
            // recurrence rules (which compute dates from rule.dayOfMonth and not from the
            // loop position) produce the right occurrences. The filter then restricts the
            // resulting transaction dates to the caller-supplied cycle sub-range.
            val transactionsToCreate = rawTransactions.filter { tx ->
                (startDateBound == null || !tx.date.isBefore(startDateBound)) &&
                (endDateBound == null || !tx.date.isAfter(endDateBound))
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
                    lastGeneratedDate = calendarMonthEnd,
                    numberOfGeneratedTransaction = tracker?.numberOfGeneratedTransaction?.plus(transactionsToCreate.size) ?: transactionsToCreate.size,
                    excludedMonths = tracker?.excludedMonths ?: emptySet()
                )
                trackerRepository.upsertTracker(newTracker)
            }
        }

        return createdTransactions
    }

    override fun calculateVirtualTransactions(
        bookletId: UUID,
        regularTransactions: List<RegularTransaction>,
        startMonth: Month,
        startYear: Int,
        endMonth: Month,
        endYear: Int,
        existingPhysicalTransactions: List<Transaction>
    ): List<Transaction> {
        val virtualTransactions = mutableListOf<Transaction>()

        val startDate = LocalDate.of(startYear, startMonth, 1)
        val lastDayOfEndMonth = YearMonth.of(endYear, endMonth).lengthOfMonth()
        val endDate = LocalDate.of(endYear, endMonth, lastDayOfEndMonth)

        // All physical transactions (preview or confirmed) are matched by (regularTransactionId, YearMonth)
        // regardless of exact date: when a user edits a preview date or confirms with a changed date,
        // the transaction's date differs from the natural recurrence date, but it still covers that
        // month's occurrence. Matching by YearMonth prevents generating a spurious virtual duplicate.
        val physicalByYearMonthKeys = existingPhysicalTransactions
            .filter { it.regularTransactionId != null }
            .map { "${it.regularTransactionId}-${YearMonth.from(it.date)}" }
            .toSet()

        regularTransactions.forEach { regularTransaction ->
            // Skip if the regular transaction hasn't started yet
            if (regularTransaction.startDate.isAfter(endDate)) {
                return@forEach
            }

            // Check excluded months for this regular transaction
            val tracker = trackerRepository.findTracker(regularTransaction.id, bookletId)
            val excludedMonths = tracker?.excludedMonths ?: emptySet()

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

            // Filter out excluded months and already materialized physical occurrences.
            val filteredTransactions = transactions.filter { transaction ->
                val transactionYearMonth = YearMonth.from(transaction.date)
                if (excludedMonths.contains(transactionYearMonth)) return@filter false
                val physicalKey = "${transaction.regularTransactionId}-$transactionYearMonth"
                physicalKey !in physicalByYearMonthKeys
            }

            virtualTransactions.addAll(filteredTransactions)
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
        // Check if a transaction already exists for this regular transaction at this date
        // This includes both preview and real transactions to avoid duplicates
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
        // Calculate the ACTUAL date that would be used for the transaction
        // This is important for Monthly/Yearly transactions where the day might be different
        val actualTransactionDate = calculateActualTransactionDate(regularTransaction, date)

        val yearMonth = YearMonth.from(actualTransactionDate)
        val monthTransactions = transactionRepository.findTransactionsByBookletYearAndMonth(
            bookletId,
            yearMonth.year,
            yearMonth.month,
        )

        return monthTransactions?.firstOrNull { transaction ->
            isDuplicateTransaction(transaction, regularTransaction, actualTransactionDate)
        } != null
    }

    private fun isDuplicateTransaction(
        existingTransaction: Transaction,
        regularTransaction: RegularTransaction,
        actualTransactionDate: LocalDate
    ): Boolean {
        if (existingTransaction.regularTransactionId != null &&
            existingTransaction.regularTransactionId == regularTransaction.id
        ) {
            return true
        }

        if (!existingTransaction.isPreview || !existingTransaction.date.isEqual(actualTransactionDate)) {
            return false
        }

        return existingTransaction.label == regularTransaction.label &&
                existingTransaction.amount == regularTransaction.amount
    }

    /**
     * Calculates the actual date that will be used for a transaction.
     * For Monthly/Yearly transactions, this adjusts the day according to the recurrence rule.
     * This must match the logic in createPrevisionalTransaction().
     */
    private fun calculateActualTransactionDate(
        regularTransaction: RegularTransaction,
        date: LocalDate
    ): LocalDate {
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

        return LocalDate.of(date.year, month, adjustedDay)
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
            tag = regularTransaction.tag ?: Tag.noneTag(),
            regularTransactionId = regularTransaction.id
        )
    }


    private fun calculateNextOccurrence(
        currentDate: LocalDate,
        regularTransaction: RegularTransaction
    ): LocalDate {
        return when (val rule = regularTransaction.recurrenceRule) {
            is RecurrenceRule.Monthly -> {
                val nextMonth = currentDate.plusMonths(1)
                val targetDay = rule.dayOfMonth
                val monthLength = YearMonth.of(nextMonth.year, nextMonth.month).lengthOfMonth()
                LocalDate.of(nextMonth.year, nextMonth.month, minOf(targetDay, monthLength))
            }
            is RecurrenceRule.Yearly -> currentDate.plusYears(1)
            is RecurrenceRule.Weekly -> currentDate.plusDays(1)
            is RecurrenceRule.Daily -> currentDate.plusDays(1)
        }
    }
}