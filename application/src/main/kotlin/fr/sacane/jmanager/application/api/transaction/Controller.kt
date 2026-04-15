package fr.sacane.jmanager.application.api.transaction

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.RecurrenceRule
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransaction
import fr.sacane.jmanager.domain.models.transaction.regular.RegularTransactionId
import fr.sacane.jmanager.domain.port.api.BookletFeature
import fr.sacane.jmanager.domain.port.api.RegularTransactionFeature
import fr.sacane.jmanager.domain.models.SessionToken
import fr.sacane.jmanager.domain.port.api.TransactionDeletionResult
import fr.sacane.jmanager.domain.port.api.TransactionFeature
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.toUUID
import fr.sacane.jmanager.domain.toUUIDs
import fr.sacane.jmanager.application.api.*
import fr.sacane.jmanager.application.configuration.BigDecimalSerializer
import fr.sacane.jmanager.application.configuration.LocalDateSerializer
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import kotlinx.serialization.Serializable
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
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


    @PostMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createTransaction(
        @Valid @RequestBody userBookletResponse: UserBookletResponse
    ): ResponseEntity<TransactionResponse> {
        return transactionFeature.bookTransaction(
            SessionToken(currentUser.token),
            userBookletResponse.bookletLabel,
            userBookletResponse.transactionResult.toModel()
        ).map {
            it.toDTO()
        }.toHttpResponse()
    }

    @DeleteMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteByIds(
        @Valid @RequestBody transactionIds: BookletTransactionsIdRequest
    ): ResponseEntity<TransactionDeletionResponse> =
        transactionFeature
            .deleteTransactionsByIds(transactionIds.bookletId.toUUID(), transactionIds.transactionIds.toUUIDs(), SessionToken(currentUser.token))
            .map { it.toDTO() }
            .toHttpResponse()


    @GetMapping
    fun getTransactionsByMonthAndYearAndBookletId(
        @RequestParam("month", required = false) month: Month?,
        @RequestParam("year") year: Int,
        @RequestParam("bookletId") bookletId: String,
        @RequestParam("startDate", required = false) startDate: LocalDate?,
        @RequestParam("endDate", required = false) endDate: LocalDate?,
        ): ResponseEntity<TransactionListResponse> {
        validateDateRange(startDate, endDate)
        logger.info("Request transactions from booklet $bookletId for month $month and year $year")
        val response = bookletFeature.loadTransactionsForBookletForAMonth(
            token = SessionToken(currentUser.token),
            bookletId = java.util.UUID.fromString(bookletId),
            month = month ?: Month.JANUARY,
            year = year,
            startDate = startDate,
            endDate = endDate,
        )

        return response.map {
            TransactionListResponse(
                transactions = (it.currentTransactions + it.previsionalTransactions).map { transaction -> transaction.toDTO() },
                amount = it.realSold.value.toString() ,
                previewAmount = it.previsionalSold.value.toString()
            )
        }.toHttpResponse().also { logger.info("Transactions fetched successfully") }
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

    @PatchMapping(consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun patchTransaction(
        @Valid @RequestBody dto: UserBookletIdsTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        logger.info("Start editing transaction => ${dto.transaction}")
        return transactionFeature.editTransaction(java.util.UUID.fromString(dto.bookletId), dto.transaction.toModel(), SessionToken(currentUser.token))
            .map {
                it.toDTO()
            }.toHttpResponse()
            .also { logger.info("Transaction edited successfully : ${dto.transaction}") }
    }


    @GetMapping("{id}")
    fun findById(
        @PathVariable("id") transactionID: String
    ): ResponseEntity<TransactionResult>
        = transactionFeature.findById(java.util.UUID.fromString(transactionID), SessionToken(currentUser.token))
            .map {
                it.toDTO()
            }.toHttpResponse()

    @PatchMapping("/confirm", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun confirmPreviewTransaction(
        @Valid @RequestBody command: ConfirmPreviewRequest
    ): ResponseEntity<TransactionResponse> {
        logger.info("Confirming preview Transaction...")
        return transactionFeature.confirmPreviewTransaction(
            transactionId = java.util.UUID.fromString(command.transactionID),
            bookletID = java.util.UUID.fromString(command.bookletID),
            newAmount = command.newAmount?.toAmount(),
            newDate = command.newDate,
            token = SessionToken(currentUser.token)
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Preview Transaction confirmed successfully")
        }
    }


    @GetMapping("/regular")
    fun getAllRegularTransactions(): ResponseEntity<List<RegularTransactionDTO>> {
        return regularTransactionFeature.getAllRegularTransactions(SessionToken(currentUser.token))
            .map { it.map { transaction -> transaction.toDTO() } }
            .toHttpResponse()
    }

    @PostMapping("/monthly", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun createMonthlyTransaction(
        @Valid @RequestBody request: MonthlyRegularTransactionRequest
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Creating monthly transaction $request from userID ${currentUser.id}")
        if (request.bookletIds.isEmpty()) {
            throw InvalidRequestException(ResultState.BAD_REQUEST.code, "At least one booklet must be selected")
        }

        return regularTransactionFeature.bookRegularTransaction(
            SessionToken(currentUser.token),
            RegularTransaction(
                id = RegularTransactionId(""),
                label = request.label,
                amount = request.value.toAmount(),
                isIncome = request.isIncome,
                tag = request.tagDTO.toDomain(),
                frequencyProperty = request.frequencyProperty.frequencyToDomain(),
                startDate = request.startDate,
                recurrenceRule = RecurrenceRule.Monthly(request.repeatDay ?: request.startDate.dayOfMonth)
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
        return regularTransactionFeature.getRegularTransactionById(SessionToken(currentUser.token), id).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Regular transaction fetched successfully")
        }
    }

    @PatchMapping("/regular", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun updateRegularTransaction(
        @Valid @RequestBody request: UpdateRegularTransactionRequest
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Updating regular transaction ${request.id}")
        return regularTransactionFeature.updateRegularTransaction(
            SessionToken(currentUser.token),
            RegularTransaction(
                id = RegularTransactionId(request.id),
                label = request.label,
                amount = request.value.toAmount(),
                isIncome = request.isIncome,
                tag = request.tagDTO.toDomain(),
                frequencyProperty = request.frequencyProperty.frequencyToDomain(),
                startDate = request.startDate,
                recurrenceRule = request.recurrenceRule.toDomain(request.startDate.dayOfMonth)
            ),
            request.bookletIds.map { java.util.UUID.fromString(it) }
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Regular transaction updated successfully")
        }
    }

    @DeleteMapping("/regular/{id}")
    fun deleteRegularTransaction(@PathVariable id: String): ResponseEntity<Unit> {
        logger.info("Deleting regular transaction $id")
        return regularTransactionFeature.deleteRegularTransaction(SessionToken(currentUser.token), id)
            .map { }
            .toHttpResponse()
            .also {
                logger.info("Regular transaction deleted successfully")
            }
    }

    @DeleteMapping("/regular", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun deleteRegularTransactions(@Valid @RequestBody request: RegularTransactionsDeletionRequest): ResponseEntity<RegularTransactionsDeletionResponse> {
        logger.info("Bulk deleting ${request.transactionIds.size} regular transaction(s)")
        return regularTransactionFeature.deleteRegularTransactions(SessionToken(currentUser.token), request.transactionIds)
            .map { deletedIds ->
                RegularTransactionsDeletionResponse(deletedIds)
            }
            .toHttpResponse()
            .also {
                logger.info("Bulk regular transactions deletion finished")
            }
    }

    @PostMapping("/regular/{transactionId}/link/{bookletId}")
    fun linkRegularTransactionToBooklet(
        @PathVariable transactionId: String,
        @PathVariable bookletId: String
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Linking booklet $bookletId to regular transaction $transactionId")
        return regularTransactionFeature.linkRegularTransactionToBooklet(
            SessionToken(currentUser.token),
            transactionId,
            java.util.UUID.fromString(bookletId)
        ).map { it.toDTO() }
            .toHttpResponse()
            .also { logger.info("Booklet linked successfully") }
    }

    @DeleteMapping("/regular/{transactionId}/link/{bookletId}")
    fun unlinkRegularTransactionFromBooklet(
        @PathVariable transactionId: String,
        @PathVariable bookletId: String
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Unlinking booklet $bookletId from regular transaction $transactionId")
        return regularTransactionFeature.unlinkRegularTransactionFromBooklet(
            SessionToken(currentUser.token),
            transactionId,
            java.util.UUID.fromString(bookletId)
        ).map { it.toDTO() }
            .toHttpResponse()
            .also { logger.info("Booklet unlinked successfully") }
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
        this.bookletAmount.value.toString(),
        this.transaction.isPreview
    )
}

@Serializable
data class ConfirmPreviewRequest(
    @field:NotBlank
    val bookletID: String,
    @field:NotBlank
    val transactionID: String,
    @Serializable(with = BigDecimalSerializer::class)
    val newAmount: BigDecimal?,
    @Serializable(with = LocalDateSerializer::class)
    val newDate: LocalDate?
)

@Serializable
data class TransactionDeletionResponse(
    val deletedIds: List<String>,
    val amount: String,
)

private fun TransactionDeletionResult.toDTO(): TransactionDeletionResponse = TransactionDeletionResponse(
    deletedIds = deletedIds.map { it.toString() },
    amount = bookletAmount.value.toString(),
)
