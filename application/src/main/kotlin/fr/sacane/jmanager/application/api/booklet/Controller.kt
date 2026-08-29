package fr.sacane.jmanager.application.api.booklet

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.BookletBalances
import fr.sacane.jmanager.domain.models.asCurrency
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.input.booklet.*
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.transaction.TransactionSortDirection
import fr.sacane.jmanager.domain.models.transaction.TransactionSortField
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.application.api.InvalidRequestException
import fr.sacane.jmanager.application.api.currentUser
import fr.sacane.jmanager.application.api.toDTO
import fr.sacane.jmanager.application.api.toHttpResponse
import fr.sacane.jmanager.application.api.transaction.TransactionResult
import fr.sacane.jmanager.application.api.tag.TagDTO
import fr.sacane.jmanager.application.bus.CommandBus
import fr.sacane.jmanager.application.bus.QueryBus
import fr.sacane.jmanager.application.configuration.BigDecimalSerializer
import fr.sacane.jmanager.application.configuration.LocalDateSerializer
import kotlinx.serialization.Serializable
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import java.math.BigDecimal
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.util.logging.Logger


@RestController
@RequestMapping("api/booklet")
@Adapter(Side.APPLICATION)
class BookletController (
    private val commandBus: CommandBus,
    private val queryBus: QueryBus,
) {
    companion object {
        private val LOGGER: Logger = Logger.getLogger("BookletController")
        private const val MAX_REGULAR_TRANSACTION_ID_LENGTH = 100
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun saveBooklet(
        @Valid @RequestBody bookletRequest: BookletBookingRequest
    ): ResponseEntity<BookletInfoDTO> {
        LOGGER.info("Booking a new Booklet ${bookletRequest.label} starting at ${bookletRequest.amount}${bookletRequest.currency} for user ${currentUser.id}")
        currentUser
        return commandBus.dispatch(
            SaveBookletCommand(
                userId = UserId(currentUser.id),
                booklet = Booklet(amount = bookletRequest.amount.toAmount(bookletRequest.currency.asCurrency()), label = bookletRequest.label)
            )
        ).map { BookletInfoDTO(it.amount.toStringValue(), it.label, it.id.toString(), it.amount.currency.symbol) }.toHttpResponse()
    }

    @GetMapping
    fun getAllBooklets(): ResponseEntity<List<BookletDTO>> {
        LOGGER.info("Requesting all booklets...")

        val response = queryBus.dispatch(
            FindAllRegisteredBookletsQuery(UserId(currentUser.id))
        )
        return response.map { booklets ->
            booklets.map {
                it.toDTO()
            }
        }.toHttpResponse()
    }

    @DeleteMapping(path = ["{bookletId}"])
    fun deleteBooklet(
        @PathVariable bookletId: String
    ): ResponseEntity<Nothing> = commandBus.dispatch(DeleteBookletByIdCommand(bookletId.toUUID(), UserId(currentUser.id))).toHttpResponse()

    @GetMapping("{bookletID}")
    fun findBookletById(
        @PathVariable("bookletID") bookletID: String
    ): ResponseEntity<BookletDTO> {
        LOGGER.info("Requesting booklet with ID $bookletID")
        return queryBus.dispatch(FindBookletByIdQuery(bookletID.toUUID(), UserId(currentUser.id)))
            .map { it.toDTO() }.toHttpResponse()
    }

    @GetMapping("report/{bookletID}")
    fun findBookletReportByIdMonthAndYear(
        @PathVariable("bookletID") bookletID: String,
        @RequestParam("month") month: Int,
        @RequestParam("year") year: Int,
        @RequestParam("startDate", required = false) startDate: LocalDate?,
        @RequestParam("endDate", required = false) endDate: LocalDate?,
    ): ResponseEntity<BookletReport> {
        validateDateRange(startDate, endDate)
        LOGGER.info("Requesting report for booklet $bookletID")
        val result = queryBus.dispatch(
            LoadTransactionsForBookletForAMonthQuery(
                userId = UserId(currentUser.id),
                bookletId = bookletID.toUUID(),
                month = Month.of(month),
                year = year,
                startDate = startDate,
                endDate = endDate,
                pageNumber = 0,
                pageSize = Int.MAX_VALUE,
            )
        )
        val report = result.map { res ->
            BookletReport(
                label = res.label,
                transactions = res.orderedTransactions.map { it.toDTO() },
                realSold = res.realSold.value.toString(),
                previewSold = res.previsionalSold.value.toString()
            )
        }
        return report.toHttpResponse()
    }

    @GetMapping("{bookletID}/balances")
    fun findBookletBalancesByMonthAndYear(
        @PathVariable("bookletID") bookletID: String,
        @RequestParam("month") month: Int,
        @RequestParam("year") year: Int,
        @RequestParam("startDate", required = false) startDate: LocalDate?,
        @RequestParam("endDate", required = false) endDate: LocalDate?,
    ): ResponseEntity<BookletBalancesResponse> {
        validateDateRange(startDate, endDate)
        LOGGER.info("Requesting balances for booklet $bookletID")
        return queryBus
            .dispatch(
                LoadBalancesForBookletForAMonthQuery(
                    userId = UserId(currentUser.id),
                    bookletId = bookletID.toUUID(),
                    month = Month.of(month),
                    year = year,
                    startDate = startDate,
                    endDate = endDate,
                )
            )
            .map { it.toDTO() }
            .toHttpResponse()
    }

    @GetMapping("{bookletID}/transactions")
    fun findBookletTransactionsByMonthAndYear(
        @PathVariable("bookletID") bookletID: String,
        @RequestParam("month") month: Int,
        @RequestParam("year") year: Int,
        @RequestParam("startDate", required = false) startDate: LocalDate?,
        @RequestParam("endDate", required = false) endDate: LocalDate?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "10") size: Int,
        @RequestParam(required = false) sortDirection: TransactionSortDirection?,
        @RequestParam(required = false) sortField: TransactionSortField?,
    ): ResponseEntity<BookletTransactionsResponse> {
        validateDateRange(startDate, endDate)
        LOGGER.info("Requesting transactions for booklet $bookletID")
        return queryBus
            .dispatch(
                LoadTransactionsForBookletForAMonthQuery(
                    userId = UserId(currentUser.id),
                    bookletId = bookletID.toUUID(),
                    month = Month.of(month),
                    year = year,
                    startDate = startDate,
                    endDate = endDate,
                    pageNumber = page,
                    pageSize = size,
                    sortDirection = sortDirection,
                    sortField = sortField,
                )
            )
            .map { res ->
                BookletTransactionsResponse(
                    transactions = res.orderedTransactions.map { it.toDTO() },
                    hasRegenerableTransactions = res.hasRegenerableTransactions,
                    pageNumber = res.pageNumber,
                    pageSize = res.pageSize,
                    totalElements = res.totalElements,
                    totalPages = res.totalPages,
                )
            }
            .toHttpResponse()
    }

    @GetMapping("{bookletID}/transactions/regenerable")
    fun findRegenerableTransactions(
        @PathVariable("bookletID") bookletID: String,
        @RequestParam("month") month: Int,
        @RequestParam("year") year: Int,
    ): ResponseEntity<List<RegenerableTransactionDTO>> {
        LOGGER.info("Requesting regenerable transactions for booklet $bookletID, month=$month, year=$year")
        return queryBus
            .dispatch(
                FindRegenerableTransactionsQuery(
                    userId = UserId(currentUser.id),
                    bookletId = bookletID.toUUID(),
                    month = Month.of(month),
                    year = year
                )
            )
            .map { candidates -> candidates.map { it.toDTO() } }
            .toHttpResponse()
    }

    @PostMapping("{bookletID}/transactions/regenerate")
    fun regenerateDeletedPrevisionalTransactions(
        @PathVariable("bookletID") bookletID: String,
        @RequestParam("month") month: Int,
        @RequestParam("year") year: Int,
        @Valid @RequestBody request: RegenerateTransactionsRequest,
    ): ResponseEntity<RegenerateTransactionsResponse> {
        LOGGER.info("Regenerating deleted previsional transactions for booklet $bookletID, month=$month, year=$year")
        validateSelectedIdentifiers(request.regularTransactionIds)
        val targetYearMonth = YearMonth.of(year, Month.of(month))
        val currentYearMonth = YearMonth.now()
        val type = when {
            targetYearMonth.isBefore(currentYearMonth) -> RegenerationType.NONE
            targetYearMonth == currentYearMonth -> RegenerationType.PREVISIONAL
            else -> RegenerationType.VIRTUAL
        }
        return commandBus
            .dispatch(
                RegenerateDeletedPrevisionalTransactionsCommand(
                    userId = UserId(currentUser.id),
                    bookletId = bookletID.toUUID(),
                    month = Month.of(month),
                    year = year,
                    regularTransactionIds = request.regularTransactionIds.map { RegularTransactionId(it) }
                )
            )
            .map { transactions ->
                RegenerateTransactionsResponse(
                    transactions = transactions.map { it.toDTO() },
                    type = type
                )
            }
            .toHttpResponse()
    }

    private fun validateSelectedIdentifiers(regularTransactionIds: List<String>) {
        if (regularTransactionIds.any { it.length > MAX_REGULAR_TRANSACTION_ID_LENGTH }) {
            throw InvalidRequestException(
                ResultState.BAD_REQUEST.code,
                "A regular transaction identifier must not exceed $MAX_REGULAR_TRANSACTION_ID_LENGTH characters"
            )
        }
    }

    private fun validateDateRange(startDate: LocalDate?, endDate: LocalDate?) {
        if ((startDate == null) != (endDate == null)) {
            throw InvalidRequestException(
                ResultState.BAD_REQUEST.code,
                "startDate and endDate must both be provided"
            )
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw InvalidRequestException(
                ResultState.BAD_REQUEST.code,
                "startDate cannot be after endDate"
            )
        }
    }
}

@Serializable
data class BookletReport(
    val label: String,
    val transactions: List<TransactionResult>,
    val realSold: String,
    val previewSold: String,
)

@Serializable
data class BookletBalancesResponse(
    val label: String,
    val realSold: String,
    val previewSold: String
)

@Serializable
data class BookletTransactionsResponse(
    val transactions: List<TransactionResult>,
    val hasRegenerableTransactions: Boolean,
    val pageNumber: Int = 0,
    val pageSize: Int = 10,
    val totalElements: Long = 0L,
    val totalPages: Int = 1,
)

@Serializable
enum class RegenerationType { PREVISIONAL, VIRTUAL, NONE }

/**
 * @property regularTransactionIds Identifiers of the regular transactions the user chose to restore.
 *           Each element is bounded to [MAX_REGULAR_TRANSACTION_ID_LENGTH] characters — the nearest
 *           reference constraint for an identifier, comfortably above the 36 characters of the UUID
 *           form actually issued. The bound is enforced in the controller rather than with a
 *           container-element `@Size`, which Hibernate Validator does not pick up here.
 */
@Serializable
data class RegenerateTransactionsRequest(
    @field:NotEmpty
    val regularTransactionIds: List<String>
)

@Serializable
data class RegenerableTransactionDTO(
    val regularTransactionId: String,
    val label: String,
    @Serializable(with = BigDecimalSerializer::class)
    val value: BigDecimal,
    val currency: String = "€",
    val isIncome: Boolean,
    @Serializable(with = LocalDateSerializer::class)
    val date: LocalDate,
    val tagDTO: TagDTO? = null,
)

@Serializable
data class RegenerateTransactionsResponse(
    val transactions: List<TransactionResult>,
    val type: RegenerationType
)

private fun BookletBalances.toDTO(): BookletBalancesResponse = BookletBalancesResponse(
    label = label,
    realSold = realSold.value.toString(),
    previewSold = previewSold.value.toString()
)
