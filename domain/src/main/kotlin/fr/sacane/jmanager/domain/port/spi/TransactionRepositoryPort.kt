package fr.sacane.jmanager.domain.port.spi
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.time.Month
import java.util.UUID

@Port(Side.INFRASTRUCTURE)
interface TransactionRepositoryPort {
    fun persist(userId: UserId, accountLabel: String, transaction: Transaction): Transaction?
    fun deleteAllSheetsById(sheetIds: List<UUID>)
    fun findTransactionById(transactionId: UUID): Transaction?
    fun save(accountId: UUID, transaction: Transaction): Transaction?
    fun findAccountWithSheetByLabelAndUser(label: String, userId: UserId): Booklet?
    fun findAccountWithTransactionById(id: UUID): Booklet?
    fun findTransactionsByBookletId(bookletId: UUID): List<Transaction>?
    fun findTransactionsByBookletYearAndMonth(bookletId: UUID, year: Int, month: Month): List<Transaction>?
}
