package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.models.Amount
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.BookletBalances
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.models.transaction.Transaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.utils.Result
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@Deprecated("Use individual use case interfaces from domain.port.input.booklet instead")
@Port(Side.APPLICATION)
/**
 * Application port: BookletFeature
 *
 * High-level API for managing booklets exposed to the application layer.
 * Implementations are responsible for authentication and returning domain Result<T>
 * signaling success or failure states.
 */
sealed interface BookletFeature {
    /**
     * Find a booklet by its unique identifier.
     *
     * @param bookletId The UUID of the booklet to find.
     * @param token Authentication token identifying the requester.
     * @return Result containing the found Booklet on success, or a failure state (e.g. BOOKLET_NOT_FOUND).
     */
    fun findBookletById(bookletId: UUID, token: SessionToken): Result<Booklet>

    /**
     * Edit an existing booklet.
     *
     * @param booklet The Booklet object containing updated values (must include an id).
     * @param token Authentication token identifying the requester.
     * @return Result containing the updated Booklet on success, or failure states when validation or persistence fails.
     */
    fun editBooklet(booklet: Booklet, token: SessionToken): Result<Booklet>

    /**
     * Delete a booklet by its identifier.
     *
     * @param bookletId The UUID of the booklet to delete.
     * @param token Authentication token identifying the requester.
     * @return Result with no value on success, or an error state if the booklet does not exist.
     */
    fun deleteBookletById(bookletId: UUID, token: SessionToken): Result<Nothing>

    /**
     * Find a booklet by its label for the authenticated user.
     *
     * @param token Authentication token identifying the requester.
     * @param label Label of the booklet to find.
     * @return Result containing the Booklet on success, or BOOKLET_LABEL_NOT_EXIST when not found.
     */
    fun findByLabelAndUserId(token: SessionToken, label: String): Result<Booklet>

    /**
     * Retrieve all booklets registered for the authenticated user.
     *
     * @param token Authentication token identifying the requester.
     * @return Result containing the list of Booklet on success.
     */
    fun findAllRegisteredBooklets(token: SessionToken): Result<List<Booklet>>

    /**
     * Save a new booklet for the authenticated user.
     *
     * @param token Authentication token identifying the requester.
     * @param booklet Booklet object to save.
     * @return Result containing the saved Booklet on success, or a failure when the label already exists or persistence fails.
     */
    fun save(token: SessionToken, booklet: Booklet): Result<Booklet>

    /**
     * Load transactions for a specific booklet for a given month and year.
     * This may generate provisional (preview) transactions for missing regular transactions
     * and compute provisional balances.
     *
     * @param token Authentication token identifying the requester.
     * @param bookletId UUID of the booklet to load transactions for.
     * @param month Target month to load transactions for.
     * @param year Target year to load transactions for.
     * @param startingMonth Starting month for calculation (defaults to current month if null).
     * @param startingYear Starting year for calculation (defaults to current year if null).
     * @return Result containing a BookletLoadingResult on success, or a failure state when the booklet is not found.
     */
    fun loadTransactionsForBookletForAMonth(
        token: SessionToken,
        bookletId: UUID,
        month: Month,
        year: Int,
        startingMonth: Month? = null,
        startingYear: Int? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
        pageNumber: Int = 0,
        pageSize: Int = 10,
    ): Result<BookletLoadingResult>

    /**
     * Load only the balances (soldes) for a specific booklet for a given month and year.
     * This uses the existing transaction loading logic but filters the result to return only
     * the balances information.
     *
     * @param token Authentication token identifying the requester.
     * @param bookletId UUID of the booklet to load balances for.
     * @param month Target month to load balances for.
     * @param year Target year to load balances for.
     * @param startingMonth Starting month for calculation (defaults to current month if null).
     * @param startingYear Starting year for calculation (defaults to current year if null).
     * @return Result containing the BookletBalances on success, or a failure state when the booklet is not found.
     */
    fun loadBalancesForBookletForAMonth(
        token: SessionToken,
        bookletId: UUID,
        month: Month,
        year: Int,
        startingMonth: Month? = null,
        startingYear: Int? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): Result<BookletBalances>

    /**
     * Re-generates previsional transactions for a given month that were previously deleted by the user.
     * Un-marks the month as excluded in the tracker so the generator can recreate them.
     * No duplicate is created if a confirmed or preview transaction already exists for that period.
     *
     * @param token Authentication token identifying the requester.
     * @param bookletId UUID of the booklet to target.
     * @param month Target month to regenerate.
     * @param year Target year to regenerate.
     * @return Result containing the list of newly created previsional transactions, or a failure state.
     */
    fun regenerateDeletedPrevisionalTransactions(
        token: SessionToken,
        bookletId: UUID,
        month: Month,
        year: Int
    ): Result<List<Transaction>>
}


// BookletFeatureImpl has been split into individual services in domain.port.input.booklet package

data class BookletLoadingResult(
    val label: String,
    val currentTransactions: List<Transaction>,
    val previsionalTransactions: List<Transaction>,
    val regularTransactions: List<RegularTransaction>,
    val realSold: Amount,
    val previsionalSold: Amount,
    val hasRegenerableTransactions: Boolean = false,
    val pageNumber: Int = 0,
    val pageSize: Int = 10,
    val totalElements: Long = 0L,
    val totalPages: Int = 1,
)
