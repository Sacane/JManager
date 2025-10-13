package fr.sacane.jmanager.domain.usecase

import fr.sacane.jmanager.domain.hexadoc.UseCase
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionTracker
import fr.sacane.jmanager.domain.port.spi.RegularTransactionTrackerRepository
import fr.sacane.jmanager.domain.port.spi.TransactionRepositoryPort
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

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
        bookletId: Long,
        regularTransactions: List<RegularTransaction>,
        targetMonth: Month,
        targetYear: Int
    ): List<Transaction>
}

@UseCase
class RegularTransactionGeneratorService(
    private val transactionRepository: TransactionRepositoryPort,
    private val trackerRepository: RegularTransactionTrackerRepository
): RegularTransactionGenerator {

    override fun generateMissingPrevisionalTransactions(
        bookletId: Long,
        regularTransactions: List<RegularTransaction>,
        targetMonth: Month,
        targetYear: Int
    ): List<Transaction> {
        val createdTransactions = mutableListOf<Transaction>()

        regularTransactions.forEach { regularTransaction ->
            val regularTxId = regularTransaction.id ?: return@forEach

            val tracker = trackerRepository.findTracker(regularTxId, bookletId)

            val startDate = if (tracker != null) {
                tracker.lastGeneratedDate.plusMonths(1)
            } else {
                regularTransaction.startDate
            }

            val targetDate = LocalDate.of(
                targetYear,
                targetMonth,
                targetMonth.length(YearMonth.of(targetYear, targetMonth).isLeapYear)
            )

            val transactionsToCreate = generateTransactionsBetween(
                regularTransaction,
                startDate,
                targetDate,
                bookletId
            )

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
                    lastGeneratedDate = targetDate
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
        bookletId: Long
    ): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        var currentDate = startDate

        if (currentDate.isAfter(endDate)) {
            return emptyList()
        }

        // Générer les transactions en fonction de la fréquence
        while (!currentDate.isAfter(endDate)) {
            val existingTransaction = checkIfTransactionExists(
                regularTransaction,
                currentDate,
                bookletId
            )

            if (!existingTransaction) {
                val transaction = createPrevisionalTransaction(
                    regularTransaction,
                    currentDate
                )
                transactions.add(transaction)
            }

            currentDate = calculateNextOccurrence(currentDate, regularTransaction)
        }

        return transactions
    }

    private fun checkIfTransactionExists(
        regularTransaction: RegularTransaction,
        date: LocalDate,
        bookletId: Long
    ): Boolean {

        val yearMonth = YearMonth.from(date)
        val monthTransactions = transactionRepository.findTransactionsByBookletYearAndMonth(
            bookletId,
            yearMonth.year,
            yearMonth.month,
        )

        return monthTransactions?.any {
            it.label == regularTransaction.label &&
                    it.date.year == date.year && it.date.month == date.month
        } ?: false
    }

    private fun createPrevisionalTransaction(
        regularTransaction: RegularTransaction,
        date: LocalDate
    ): Transaction {
        return Transaction(
            id = null,
            label = regularTransaction.label,
            amount = regularTransaction.amount,
            date = date,
            isPreview = true,
            isIncome = regularTransaction.isIncome,
            regularTransactionId = regularTransaction.id

        )
    }


    private fun calculateNextOccurrence(
        currentDate: LocalDate,
        regularTransaction: RegularTransaction
    ): LocalDate {
        return when (regularTransaction) {
            is MonthlyTransaction -> currentDate.plusMonths(1)
        }
    }
}