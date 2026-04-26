package fr.sacane.jmanager.application.api.booklet

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.BookletBalances
import fr.sacane.jmanager.domain.models.asCurrency
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.input.booklet.*
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.application.api.InvalidRequestException
import fr.sacane.jmanager.application.api.currentUser
import fr.sacane.jmanager.application.api.toDTO
import fr.sacane.jmanager.application.api.toHttpResponse
import fr.sacane.jmanager.application.api.transaction.TransactionResult
import kotlinx.serialization.Serializable
import jakarta.validation.Valid
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
    private val saveBookletUseCase: SaveBookletUseCase,
    private val findAllRegisteredBookletsUseCase: FindAllRegisteredBookletsUseCase,
    private val deleteBookletByIdUseCase: DeleteBookletByIdUseCase,
    private val findBookletByIdUseCase: FindBookletByIdUseCase,
    private val loadTransactionsForBookletForAMonthUseCase: LoadTransactionsForBookletForAMonthUseCase,
    private val loadBalancesForBookletForAMonthUseCase: LoadBalancesForBookletForAMonthUseCase,
    private val regenerateDeletedPrevisionalTransactionsUseCase: RegenerateDeletedPrevisionalTransactionsUseCase,
) {
    companion object {
        private val LOGGER: Logger = Logger.getLogger("BookletController")
    }

    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun saveBooklet(
        @Valid @RequestBody bookletRequest: BookletBookingRequest
    ): ResponseEntity<BookletInfoDTO> {
        LOGGER.info("Booking a new Booklet ${bookletRequest.label} starting at ${bookletRequest.amount}${bookletRequest.currency} for user ${currentUser.id}")
        currentUser
        return saveBookletUseCase.handle(
            SaveBookletCommand(
                token = SessionToken(currentUser.token),
                booklet = Booklet(amount = bookletRequest.amount.toAmount(bookletRequest.currency.asCurrency()), label = bookletRequest.label)
            )
        ).map { BookletInfoDTO(it.amount.toStringValue(), it.label, it.id.toString(), it.amount.currency.symbol) }.toHttpResponse()
    }

    @GetMapping
    fun getAllBooklets(): ResponseEntity<List<BookletDTO>> {
        LOGGER.info("Requesting all booklets...")

        val response = findAllRegisteredBookletsUseCase.handle(
            FindAllRegisteredBookletsQuery(SessionToken(currentUser.token))
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
    ): ResponseEntity<Nothing> = deleteBookletByIdUseCase.handle(DeleteBookletByIdCommand(bookletId.toUUID(), SessionToken(currentUser.token))).toHttpResponse()

    @GetMapping("{bookletID}")
    fun findBookletById(
        @PathVariable("bookletID") bookletID: String
    ): ResponseEntity<BookletDTO> {
        LOGGER.info("Requesting booklet with ID $bookletID")
        return findBookletByIdUseCase.handle(FindBookletByIdQuery(bookletID.toUUID(), SessionToken(currentUser.token)))
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
        val result = loadTransactionsForBookletForAMonthUseCase.handle(
            LoadTransactionsForBookletForAMonthQuery(
                token = SessionToken(currentUser.token),
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
                transactions = (res.currentTransactions + res.previsionalTransactions).map { it.toDTO() },
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
        return loadBalancesForBookletForAMonthUseCase
            .handle(
                LoadBalancesForBookletForAMonthQuery(
                    token = SessionToken(currentUser.token),
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
    ): ResponseEntity<BookletTransactionsResponse> {
        validateDateRange(startDate, endDate)
        LOGGER.info("Requesting transactions for booklet $bookletID")
        return loadTransactionsForBookletForAMonthUseCase
            .handle(
                LoadTransactionsForBookletForAMonthQuery(
                    token = SessionToken(currentUser.token),
                    bookletId = bookletID.toUUID(),
                    month = Month.of(month),
                    year = year,
                    startDate = startDate,
                    endDate = endDate,
                    pageNumber = page,
                    pageSize = size,
                )
            )
            .map { res ->
                BookletTransactionsResponse(
                    transactions = (res.currentTransactions + res.previsionalTransactions).map { it.toDTO() },
                    hasRegenerableTransactions = res.hasRegenerableTransactions,
                    pageNumber = res.pageNumber,
                    pageSize = res.pageSize,
                    totalElements = res.totalElements,
                    totalPages = res.totalPages,
                )
            }
            .toHttpResponse()
    }

    @PostMapping("{bookletID}/transactions/regenerate")
    fun regenerateDeletedPrevisionalTransactions(
        @PathVariable("bookletID") bookletID: String,
        @RequestParam("month") month: Int,
        @RequestParam("year") year: Int,
    ): ResponseEntity<RegenerateTransactionsResponse> {
        LOGGER.info("Regenerating deleted previsional transactions for booklet $bookletID, month=$month, year=$year")
        val targetYearMonth = YearMonth.of(year, Month.of(month))
        val currentYearMonth = YearMonth.now()
        val type = when {
            targetYearMonth.isBefore(currentYearMonth) -> RegenerationType.NONE
            targetYearMonth == currentYearMonth -> RegenerationType.PREVISIONAL
            else -> RegenerationType.VIRTUAL
        }
        return regenerateDeletedPrevisionalTransactionsUseCase
            .handle(
                RegenerateDeletedPrevisionalTransactionsCommand(
                    token = SessionToken(currentUser.token),
                    bookletId = bookletID.toUUID(),
                    month = Month.of(month),
                    year = year
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
