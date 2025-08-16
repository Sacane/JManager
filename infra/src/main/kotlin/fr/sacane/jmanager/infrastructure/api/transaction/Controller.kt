package fr.sacane.jmanager.infrastructure.api.transaction

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Tag
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.domain.models.transaction.regular.Frequency
import fr.sacane.jmanager.domain.port.api.RegularTransactionFeature
import fr.sacane.jmanager.domain.port.api.TransactionFeature
import fr.sacane.jmanager.infrastructure.api.*
import fr.sacane.jmanager.infrastructure.api.tag.TagDTO
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
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
) {
    private val logger = Logger.getLogger(TransactionController::class.java.name)

    @PostMapping
    fun createTransaction(
        @RequestBody userBookletResponse: UserBookletResponse
    ): ResponseEntity<TransactionResponse> {
        logger.info("Current user : ${SecurityContextHolder.getContext().authentication}")
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
        = transactionFeature.deleteSheetsByIds(sheetIds.accountId, sheetIds.transactionIds, currentUser.token)
        .toHttpResponse()


    @GetMapping
    fun getTransactionsByMonthAndYearAndAccountLabel(
        @RequestParam("month", required = false) month: Month?,
        @RequestParam("year") year: Int,
        @RequestParam("accountLabel") accountLabel: String
        ): ResponseEntity<TransactionListResponse> {
        LOGGER.info("Request transactions from booklet $accountLabel for month $month and year $year")
        val response = transactionFeature.retrieveTransactionsByMonthAndYear(
            currentUser.token,
            month ?: LocalDate.now().month,
            year,
            accountLabel
        )
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(TransactionListResponse(response.mapTo { it!!.map { sheet -> sheet.toDTO() } }))
    }

    @PatchMapping
    fun patchTransaction(
        @RequestBody dto: UserAccountIdsTransactionRequest
    ): ResponseEntity<TransactionResponse> {
        logger.info("Start editing transaction => ${dto.transaction}")
        return transactionFeature.editTransaction(dto.accountId, dto.transaction.toModel(), currentUser.token)
            .map {
                it.toDTO()
            }.toHttpResponse()
            .also { LOGGER.info("Transaction edited successfully : ${dto.transaction}") }
    }


    @GetMapping("{id}")
    fun findById(
        @PathVariable("id") transactionID: Long
    ): ResponseEntity<TransactionResult>
        = transactionFeature.findById(transactionID, currentUser.token)
            .map {
                it.toDTO()
            }.toHttpResponse()

    @PatchMapping("/confirm")
    fun confirmPreviewTransaction(
        @RequestBody command: ConfirmPreviewCommand
    ): ResponseEntity<TransactionResponse> {
        logger.info("Confirming preview Transaction...")
        return transactionFeature.confirmPreviewTransaction(
            transactionId = command.transactionID,
            accountID = command.accountID,
            token = currentUser.token
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Preview Transaction confirmed successfully")
        }
    }

    @PostMapping("/regular")
    fun createRegularTransaction(
        @RequestBody regularTransactionCreationRequest: RegularTransactionCreationRequest
    ): ResponseEntity<RegularTransactionDTO> {
        logger.info("Current user : ${SecurityContextHolder.getContext().authentication}")
        return regularTransactionFeature.bookRegularTransaction(
            currentUser.token,
            LocalDate.parse(regularTransactionCreationRequest.startDate),
            regularTransactionCreationRequest.label,
            regularTransactionCreationRequest.value.toAmount(),
            regularTransactionCreationRequest.isIncome,
            tag = regularTransactionCreationRequest.tagDTO?.toDomain() ?: Tag("Aucune", isDefault = true),
            frequency = Frequency.valueOf(regularTransactionCreationRequest.regularity),
        ).map {
            it.toDTO()
        }.toHttpResponse()
    }

    @GetMapping("/regular")
    fun getAllRegularTransactions(): ResponseEntity<List<RegularTransactionDTO>> {
        return regularTransactionFeature.getAllRegularTransactions(currentUser.token)
            .map { it.map { transaction -> transaction.toDTO() } }
            .toHttpResponse()
    }

    @PostMapping("/monthly")
    fun createMonthlyTransaction(

    ): ResponseEntity<TransactionResponse> {
        logger.info("Creating monthly transaction...")
        return transactionFeature.bookTransaction(
            currentUser.token,
            MonthlyTransactionCreationRequest(
                label = "Monthly Salary",
                value = "3000",
                isIncome = true,
                month = LocalDate.now().month,
                year = LocalDate.now().year
            )
        ).map {
            it.toDTO()
        }.toHttpResponse().also {
            logger.info("Monthly transaction created successfully")
        }
    }


    companion object {
        private val LOGGER: Logger = Logger.getLogger(TransactionController::javaClass.name)
    }
}

fun TransactionResumeResult.toDTO(): TransactionResponse {
    return TransactionResponse(
        this.transaction.id.toString(),
        this.transaction.label,
        this.transaction.date,
        this.transaction.amount.amount.toString(),
        this.transaction.isIncome,
        this.transaction.tag.toDTO(),
        this.accountAmount.amount.toString(),
        this.accountPreviewAmount.amount.toString(),
        this.transaction.isPreview
    )
}


data class ConfirmPreviewCommand(
    val accountID: Long,
    val transactionID: Long
)

data class MonthlyTransactionCreationRequest(
    val label: String,
    val value: String,
    val isIncome: Boolean,
    val tagDTO: TagDTO? = null,
    val month: Month,
    val year: Int
)