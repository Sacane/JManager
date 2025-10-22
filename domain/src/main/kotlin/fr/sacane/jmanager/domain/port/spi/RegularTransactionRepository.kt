package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.MonthlyTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import java.util.UUID

interface RegularTransactionRepository {

    fun getAllRegularTransactions(userId: UserId): List<RegularTransaction>
    fun getAllRegularUsedByAccount(userId: UserId, accountID: UUID): List<RegularTransaction>?

    fun saveMonthlyRegularTransaction(userId: UserId, monthlyTransaction: MonthlyTransaction, bookletIds: List<UUID>): RegularTransaction
    fun getRegularTransactionById(userId: UserId, transactionId: RegularTransactionId): RegularTransaction?
}