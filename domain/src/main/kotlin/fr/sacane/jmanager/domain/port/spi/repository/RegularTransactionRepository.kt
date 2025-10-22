package fr.sacane.jmanager.domain.port.spi.repository

import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import java.util.UUID

interface RegularTransactionRepository {

    fun getAllRegularTransactions(userId: UserId): List<RegularTransaction>
    fun getAllRegularUsedByAccount(userId: UserId, accountID: UUID): List<RegularTransaction>?

    fun saveRegularTransaction(userId: UserId, regularTransaction: RegularTransaction, bookletIds: List<UUID>): RegularTransaction
    fun getRegularTransactionById(userId: UserId, transactionId: RegularTransactionId): RegularTransaction?
    fun updateRegularTransaction(userId: UserId, regularTransaction: RegularTransaction): RegularTransaction?
    fun deleteRegularTransaction(userId: UserId, transactionId: RegularTransactionId): Boolean
}