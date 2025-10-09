package fr.sacane.jmanager.domain.port.spi
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.time.Month

@Port(Side.INFRASTRUCTURE)
interface TransactionRepositoryPort {
    fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction?
    fun deleteAllSheetsById(sheetIds: List<Long>)
    fun findTransactionById(transactionId: Long): Transaction?
    fun save(accountId: Long, transaction: Transaction): Transaction?
    fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Booklet?
    fun findAccountWithTransactionById(id: Long): Booklet?
    fun findTransactionsByBookletId(bookletId: Long): List<Transaction>?
    fun findTransactionsByBookletYearAndMonth(bookletId: Long, year: Int, month: Month): List<Transaction>?
}
