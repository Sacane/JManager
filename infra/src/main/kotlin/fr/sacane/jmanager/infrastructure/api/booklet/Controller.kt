package fr.sacane.jmanager.infrastructure.api.booklet

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Booklet
import fr.sacane.jmanager.domain.models.asCurrency
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.port.api.BookletFeature
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.infrastructure.api.currentUser
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import fr.sacane.jmanager.infrastructure.api.transaction.TransactionResult
import kotlinx.serialization.Serializable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Month
import java.util.logging.Logger


@RestController
@RequestMapping("api/account")
@Adapter(Side.APPLICATION)
class BookletController (
    private val feature: BookletFeature
) {
    companion object {
        private val LOGGER: Logger = Logger.getLogger("BookletController")
    }

    @PostMapping
    fun saveBooklet(
        @RequestBody userAccount: BookletBookingRequest
    ): ResponseEntity<AccountInfoDTO> {
        LOGGER.info("Booking a new Booklet ${userAccount.labelAccount} starting at ${userAccount.amount}${userAccount.currency} for user ${currentUser.id}")
        return feature.save(
            currentUser.token,
            Booklet(amount = userAccount.amount.toAmount(userAccount.currency.asCurrency()), labelAccount = userAccount.labelAccount)
        ).map { AccountInfoDTO(it.amount.toStringValue(), it.label, it.id.toString(), it.amount.currency.symbol) }.toHttpResponse()
    }

    @GetMapping
    fun getAllBooklets(): ResponseEntity<List<AccountDTO>> {
        LOGGER.info("Requesting all accounts...")

        val response = feature.findAllRegisteredAccounts(
            currentUser.token
        )
        return response.map { accounts ->
            accounts.map {
                it.toDTO()
            }
        }.toHttpResponse()
    }

    @DeleteMapping(path = ["{accountId}"])
    fun deleteAccount(
        @PathVariable accountId: String
    ): ResponseEntity<Nothing> = feature.deleteAccountById(accountId.toUUID(), currentUser.token).toHttpResponse()

    @GetMapping("{accountID}")
    fun findBookletById(
        @PathVariable("accountID") accountID: String
    ): ResponseEntity<AccountDTO> {
        LOGGER.info("Requesting account with ID $accountID")
        return feature.findAccountById(accountID.toUUID(), currentUser.token)
            .map { it.toDTO() }.toHttpResponse()
    }

    @GetMapping("report/{accountID}")
    fun findBookletReportByIdMonthAndYear(
        @PathVariable("accountID") accountID: String,
        @RequestParam("month") month: Int,
        @RequestParam("year") year: Int
    ): ResponseEntity<BookletReport> {
        LOGGER.info("Requesting account report for booklet $accountID")
        val result = feature.loadTransactionsForBookletForAMonth(
            token = currentUser.token, accountID.toUUID(), Month.of(month), year
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
}

@Serializable
data class BookletReport(
    val label: String,
    val transactions: List<TransactionResult>,
    val realSold: String,
    val previewSold: String,
)
