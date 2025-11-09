package fr.sacane.jmanager.infrastructure.api.transaction

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.api.BookletFeature
import fr.sacane.jmanager.domain.port.api.RegularTransactionFeature
import fr.sacane.jmanager.domain.port.api.TransactionFeature
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.domain.toUUIDs
import fr.sacane.jmanager.infrastructure.api.*
import kotlinx.serialization.Serializable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.Month
import java.util.logging.Logger

@RestController
@RequestMapping("api/transaction")
@Adapter(Side.APPLICATION)
class TransactionController(
    private val transactionFeature: TransactionFeature,
    private val regularTransactionFeature: RegularTransactionFeature,
    private val bookletFeature: BookletFeature
) {

    companion object {
        private val logger = Logger.getLogger(TransactionController::class.java.name)
    }


    @PostMapping
    fun createTransaction(
        @RequestBody userBookletResponse: UserBookletResponse
    ): ResponseEntity<TransactionResponse> {
        return transactionFeature.bookTransaction(
            currentUser.token,
            userBookletResponse.accountLabel,
            userBookletResponse.transactionResult.toModel()
        ).map {
            it.toDTO()
        }.toHttpResponse()
    }

    @DeleteMapping
    fun deleteByIds(
        @RequestBody sheetIds: AccountTransactionsIdRequest
    ): ResponseEntity<Nothing>
        = transactionFeature.deleteSheetsByIds(sheetIds.accountId.toUUID(), sheetIds.transactionIds.toUUIDs(), currentUser.token)
        .toHttpResponse()


    @GetMapping
    fun getTransactionsByMonthAndYearAndAccountId(
        @RequestParam("month", required = false) month: Month?,
        @RequestParam("year") year: Int,
        @RequestParam("bookletId") bookletId: String
        ): ResponseEntity<TransactionListResponse> {
        logger.info("Request transactions from booklet $bookletId for month $month and year $year")
        val response = bookletFeature.loadTransactionsForBookletForAMonth(currentUser.token, java.util.UUID.fromString(bookletId), month ?: Month.JANUARY, year)

        return response.map {
            TransactionListResponse(
                transactions = (it.currentTransactions + it.previsionalTransactions).map { sheet -> sheet.toDTO() },
                amount = it.realSold.value.toString() ,
                previewAmount = it.previsionalSold.value.toString()
            )
        }.toHttpResponse().also { logger.info("Transactions fetched successfully") }
    }

    @PatchMapping
    fun patchTransaction(
        @RequestBody dto: UserAccountIdsTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        logger.info("Start editing transaction => ${dto.transaction}")
        return transactionFeature.editTransaction(java.util.UUID.fromString(dto.accountId), dto.transaction.toModel(), currentUser.token)
            .map {
                it.toDTO()
            }.toHttpResponse()
            .also { logger.info("Transaction edited successfully : ${dto.transaction}") }
    }


    @GetMapping("{id}")
    fun findById(
        @PathVariable("id") transactionID: String
    ): ResponseEntity<TransactionResult>
        = transactionFeature.findById(java.util.UUID.fromString(transactionID), currentUser.token)
            .map {
                it.toDTO()
            }.toHttpResponse()

    @PatchMapping("/confirm")
    fun confirmPreviewTransaction(
        @RequestBody command: ConfirmPreviewRequest
    ): ResponseEntity<TransactionResponse> {
        logger.info("Confirming preview Transaction...")
        return transactionFeature.confirmPreviewTransaction(
            transactionId = java.util.UUID.fromString(command.transactionID),
            accountID = java.util.UUID.fromString(command.accountID),
            token = currentUser.token
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Preview Transaction confirmed successfully")
        }
    }


    @GetMapping("/regular")
    fun getAllRegularTransactions(): ResponseEntity<List<RegularTransactionDTO>> {
        return regularTransactionFeature.getAllRegularTransactions(currentUser.token)
            .map { it.map { transaction -> transaction.toDTO() } }
            .toHttpResponse()
    }

    @PostMapping("/monthly")
    fun createMonthlyTransaction(
        @RequestBody request: MonthlyRegularTransactionRequest
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Creating monthly transaction $request from userID ${currentUser.id}")
        return regularTransactionFeature.bookRegularTransaction(
            currentUser.token,
            RegularTransaction(
                id = RegularTransactionId(""),
                label = request.label,
                amount = request.value.toAmount(),
                isIncome = request.isIncome,
                tag = request.tagDTO.toDomain(),
                frequencyProperty = request.frequencyProperty.frequencyToDomain(),
                startDate = LocalDate.now(),
                recurrenceRule = RecurrenceRule.Monthly(request.repeatDay ?: 1)
            ),
            request.bookletIds.map { java.util.UUID.fromString(it) }
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Monthly transaction created successfully")
        }
    }

    @GetMapping("/regular/{id}")
    fun getRegularTransactionById(@PathVariable id: String): ResponseEntity<RegularTransactionDTO> {
        logger.info("Fetching regular transaction with ID $id")
        return regularTransactionFeature.getRegularTransactionById(currentUser.token, id).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Regular transaction fetched successfully")
        }
    }

    @PatchMapping("/regular")
    fun updateRegularTransaction(
        @RequestBody request: UpdateRegularTransactionRequest
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Updating regular transaction ${request.id}")
        return regularTransactionFeature.updateRegularTransaction(
            currentUser.token,
            RegularTransaction(
                id = RegularTransactionId(request.id),
                label = request.label,
                amount = request.value.toAmount(),
                isIncome = request.isIncome,
                tag = request.tagDTO.toDomain(),
                frequencyProperty = request.frequencyProperty.frequencyToDomain(),
                startDate = LocalDate.now(),
                recurrenceRule = request.recurrenceRule.toDomain()
            )
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Regular transaction updated successfully")
        }
    }

    @DeleteMapping("/regular/{id}")
    fun deleteRegularTransaction(@PathVariable id: String): ResponseEntity<Unit> {
        logger.info("Deleting regular transaction $id")
        return regularTransactionFeature.deleteRegularTransaction(currentUser.token, id)
            .map { }
            .toHttpResponse()
            .also {
                logger.info("Regular transaction deleted successfully")
            }
    }
}

fun TransactionResumeResult.toDTO(): TransactionResponse {
    return TransactionResponse(
        this.transaction.id.toString(),
        this.transaction.label,
        this.transaction.date,
        this.transaction.amount.value.toString(),
        this.transaction.isIncome,
        this.transaction.tag!!.toDTO(),
        this.accountAmount.value.toString(),
        this.accountPreviewAmount.value.toString(),
        this.transaction.isPreview
    )
}

@Serializable
data class ConfirmPreviewRequest(
    val accountID: String,
    val transactionID: String
)
