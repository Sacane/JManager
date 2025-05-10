package fr.sacane.jmanager.infrastructure.api.transaction

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.port.api.TransactionFeature
import fr.sacane.jmanager.infrastructure.api.currentUser
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toHttpResponse
import fr.sacane.jmanager.infrastructure.api.toModel
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.Month
import java.util.logging.Logger

@RestController
@RequestMapping("api/transaction")
@Adapter(Side.APPLICATION)
class TransactionController(private val transactionFeature: TransactionFeature) {
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
        = transactionFeature.deleteSheetsByIds(sheetIds.accountId, sheetIds.sheetIds, currentUser.token)
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
        logger.info("Start editing transaction => ${dto.sheet}")
        return transactionFeature.editTransaction(dto.accountId, dto.sheet.toModel(), currentUser.token)
            .map {
                it.toDTO()
            }.toHttpResponse()
            .also { LOGGER.info("Transaction edited successfully : ${dto.sheet}") }
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