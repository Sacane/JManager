package fr.sacane.jmanager.domain.port.spi.repository
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.Transaction
import java.time.Month
import java.util.UUID

@Port(Side.INFRASTRUCTURE)
/**
 * SPI contract for transaction persistence and booklet-related retrievals.
 *
 * Implementations provide concrete storage operations for transactions and related
 * booklet reads. The domain depends on this abstraction to persist and
 * query transaction sheets without coupling to a specific datastore.
 */
interface TransactionRepository {
    /**
     * Persist a transaction for the given user and booklet label.
     *
     * @param userId Domain user identifier
     * @param bookletLabel Label of the booklet where the transaction is created
     * @param transaction Transaction entity to persist
     * @return Persisted Transaction with any assigned identifiers, or null on failure
     */
    fun persist(userId: UserId, bookletLabel: String, transaction: Transaction): Transaction?

    /**
     * Delete multiple transaction sheets by their identifiers.
     *
     * @param sheetIds List of UUIDs corresponding to transaction sheets to delete
     */
    fun deleteAllSheetsById(sheetIds: List<UUID>)

    /**
     * Find a transaction by its UUID.
     *
     * @param transactionId UUID of the transaction to retrieve
     * @return Transaction if found, or null otherwise
     */
    fun findTransactionById(transactionId: UUID): Transaction?

    /**
     * Save (update) a transaction for a given booklet id.
     *
     * @param bookletId UUID of the booklet owning the transaction
     * @param transaction Transaction to save
     * @return Saved Transaction or null on failure
     */
    fun save(bookletId: UUID, transaction: Transaction): Transaction?

    /**
     * Retrieve a Booklet aggregate with its transaction sheets by booklet label and user.
     *
     * @param label Booklet label
     * @param userId Domain user identifier
     * @return Booklet aggregate or null when not found
     */
    fun findBookletByLabelWithSheets(label: String, userId: UserId): Booklet?

    /**
     * Retrieve a Booklet aggregate with transactions by its id.
     *
     * @param id UUID of the booklet
     * @return Booklet aggregate or null when not found
     */
    fun findBookletByIdWithTransactions(id: UUID): Booklet?

    /**
     * Find all transactions for a given booklet id.
     *
     * @param bookletId UUID of the booklet
     * @return List of Transaction or null if none exist
     */
    fun findTransactionsByBookletId(bookletId: UUID): List<Transaction>?

    /**
     * Find transactions for a booklet filtered by year and month.
     *
     * @param bookletId UUID of the booklet
     * @param year Target year
     * @param month Target month
     * @return List of Transaction for the specified period, or null if none
     */
    fun findTransactionsByBookletYearAndMonth(bookletId: UUID, year: Int, month: Month): List<Transaction>?

    /**
     * Check whether any transaction references the given personal tag.
     *
     * @param tagId UUID of the personal tag to check
     * @return true if at least one transaction uses the tag
     */
    fun isPersonalTagUsed(tagId: UUID): Boolean

    /**
     * Replace every occurrence of the given personal tag in transactions with the supplied default tag.
     *
     * @param tagId UUID of the personal tag to replace
     * @param defaultTag Tag to assign as replacement
     */
    fun replacePersonalTagByDefault(tagId: UUID, defaultTag: Tag)
}
