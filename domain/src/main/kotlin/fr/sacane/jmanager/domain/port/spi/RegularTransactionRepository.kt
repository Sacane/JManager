package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.Frequency
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import java.time.LocalDate

interface RegularTransactionRepository {
    fun saveRegularTransaction(
        userId: UserId,
        startDate: LocalDate,
        label: String,
        amount: Amount,
        isIncome: Boolean,
        tag: Tag,
        frequency: Frequency
    ): RegularTransaction

    fun getAllRegularTransactions(userId: UserId): List<RegularTransaction>

    fun getAllRegularUsedByAccount(userId: UserId, accountID: Long): List<RegularTransaction>?

    fun linkedRegularTransactionsWithBooklet(
        userId: UserId,
        regularTransactionId: RegularTransactionId,
        bookletId: Long
    )

    fun saveRegularTransaction(userId: UserId, transaction: RegularTransaction): RegularTransaction
    fun saveMonthlyRegularTransaction(userId: UserId, monthlyTransaction: MonthlyTransaction): RegularTransaction
    fun getRegularTransactionById(userId: UserId, transactionId: RegularTransactionId): RegularTransaction
}