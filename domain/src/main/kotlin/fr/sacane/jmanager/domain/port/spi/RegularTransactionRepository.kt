package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId

interface RegularTransactionRepository {

    fun getAllRegularTransactions(userId: UserId): List<RegularTransaction>
    fun getAllRegularUsedByAccount(userId: UserId, accountID: Long): List<RegularTransaction>?

    fun saveMonthlyRegularTransaction(userId: UserId, monthlyTransaction: MonthlyTransaction, bookletIds: List<Long>): RegularTransaction
    fun getRegularTransactionById(userId: UserId, transactionId: RegularTransactionId): RegularTransaction?
}