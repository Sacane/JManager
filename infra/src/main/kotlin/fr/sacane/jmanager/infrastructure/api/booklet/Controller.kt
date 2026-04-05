package fr.sacane.jmanager.infrastructure.api.booklet

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.BookletBalances
import fr.sacane.jmanager.domain.models.asCurrency
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.api.BookletFeature
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.infrastructure.api.InvalidRequestException
import fr.sacane.jmanager.infrastructure.api.currentUser
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import fr.sacane.jmanager.infrastructure.api.transaction.TransactionResult
import kotlinx.serialization.Serializable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.Month
import java.util.logging.Logger


@RestController
@RequestMapping("api/booklet")
@Adapter(Side.APPLICATION)
class BookletController (
    private val feature: BookletFeature
) {
    companion object {
        private val LOGGER: Logger = Logger.getLogger("BookletController")
    }

    @PostMapping
    fun saveBooklet(
        @RequestBody bookletRequest: BookletBookingRequest
    ): ResponseEntity<BookletInfoDTO> {
        LOGGER.info("Booking a new Booklet ${bookletRequest.label} starting at ${bookletRequest.amount}${bookletRequest.currency} for user ${currentUser.id}")
        return feature.save(
            currentUser.token,
            Booklet(amount = bookletRequest.amount.toAmount(bookletRequest.currency.asCurrency()), label = bookletRequest.label)
        ).map { BookletInfoDTO(it.amount.toStringValue(), it.label, it.id.toString(), it.amount.currency.symbol) }.toHttpResponse()
    }

    @GetMapping
    fun getAllBooklets(): ResponseEntity<List<BookletDTO>> {
        LOGGER.info("Requesting all booklets...")

        val response = feature.findAllRegisteredBooklets(
            currentUser.token
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
    ): ResponseEntity<Nothing> = feature.deleteBookletById(bookletId.toUUID(), currentUser.token).toHttpResponse()

    @GetMapping("{bookletID}")
    fun findBookletById(
        @PathVariable("bookletID") bookletID: String
    ): ResponseEntity<BookletDTO> {
        LOGGER.info("Requesting booklet with ID $bookletID")
        return feature.findBookletById(bookletID.toUUID(), currentUser.token)
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
        val result = feature.loadTransactionsForBookletForAMonth(
            token = currentUser.token,
            bookletId = bookletID.toUUID(),
            month = Month.of(month),
            year = year,
            startDate = startDate,
            endDate = endDate,
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
        return feature
            .loadBalancesForBookletForAMonth(
                token = currentUser.token,
                bookletId = bookletID.toUUID(),
                month = Month.of(month),
                year = year,
                startDate = startDate,
                endDate = endDate,
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
    ): ResponseEntity<BookletTransactionsResponse> {
        validateDateRange(startDate, endDate)
        LOGGER.info("Requesting transactions for booklet $bookletID")
        return feature
            .loadTransactionsForBookletForAMonth(
                token = currentUser.token,
                bookletId = bookletID.toUUID(),
                month = Month.of(month),
                year = year,
                startDate = startDate,
                endDate = endDate,
            )
            .map { res ->
                BookletTransactionsResponse(
                    transactions = (res.currentTransactions + res.previsionalTransactions).map { it.toDTO() }
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
    val transactions: List<TransactionResult>
)

private fun BookletBalances.toDTO(): BookletBalancesResponse = BookletBalancesResponse(
    label = label,
    realSold = realSold.value.toString(),
    previewSold = previewSold.value.toString()
)
