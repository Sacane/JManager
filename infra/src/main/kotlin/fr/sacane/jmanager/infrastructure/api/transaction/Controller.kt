package fr.sacane.jmanager.infrastructure.api.transaction

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.TransactionResumeResult
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.api.TransactionFeature
import fr.sacane.jmanager.infrastructure.api.id
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toModel
import fr.sacane.jmanager.infrastructure.api.sendAsHttpResponse
import org.springframework.http.ResponseEntity
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
    suspend fun createTransaction(
        @RequestBody userAccountSheetDTO: UserAccountSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<TransactionResultDTO> {
        return transactionFeature.bookTransaction(
            userAccountSheetDTO.userId.id(),
            token.asTokenUUID(),
            userAccountSheetDTO.accountLabel,
            userAccountSheetDTO.sheetDTO.toModel()
        ).map {
            it.toDTO()
        }.sendAsHttpResponse().apply {
            logger.info("Creating new transaction => ${userAccountSheetDTO.sheetDTO} TO => ${this.body?.tagDTO}")
        }
    }


    @DeleteMapping("{userId}")
    fun deleteByIds(
        @PathVariable("userId") userId: Long,
        @RequestBody sheetIds: AccountSheetIdsDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Nothing>
        = transactionFeature.deleteSheetsByIds(UserId(userId), sheetIds.accountId, sheetIds.sheetIds, token.asTokenUUID())
        .sendAsHttpResponse()


    @GetMapping
    fun getTransactionsByMonthAndYearAndAccountLabel(
        @RequestParam("userId") userId: Long,
        @RequestParam("month", required = false) month: Month?,
        @RequestParam("year") year: Int,
        @RequestParam("accountLabel") accountLabel: String,
        @RequestHeader("Authorization") token: String
        ): ResponseEntity<TransactionList> {
        LOGGER.info("Request transactions from booklet $accountLabel for month $month and year $year")
        val response = transactionFeature.retrieveTransactionsByMonthAndYear(userId.id(), token.asTokenUUID(), month ?: LocalDate.now().month, year, accountLabel)
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(TransactionList(response.mapTo { it!!.map { sheet -> sheet.toDTO() } }))
    }

    @PostMapping("edit")
    fun editTransaction(
        @RequestBody dto: UserIDSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<TransactionResultDTO> {
        logger.info("Start editing transaction => ${dto.sheet}")
        return transactionFeature.editTransaction(dto.userId, dto.accountId, dto.sheet.toModel(), token.asTokenUUID())
            .map {
                it.toDTO()
            }.sendAsHttpResponse()
            .also { LOGGER.info("Transaction edited successfully : ${dto.sheet}") }
    }


    @GetMapping("{id}")
    fun findById(
        @RequestParam("userID") userID: Long,
        @PathVariable("id") sheetID: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetDTO>
        = transactionFeature.findById(userID, sheetID, token.asTokenUUID())
            .map {
                it.toDTO()
            }.sendAsHttpResponse()

    @PostMapping("transaction/confirm")
    fun confirmPreviewTransaction(
        @RequestBody command: ConfirmPreviewCommand,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<TransactionResultDTO> {
        logger.info("Confirming preview Transaction...")
        return transactionFeature.confirmPreviewTransaction(
            userId = command.userID.id(),
            transactionId = command.transactionID,
            accountID = command.accountID,
            token = token.asTokenUUID()
        ).map {
            it.toDTO()
        }.sendAsHttpResponse()
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(TransactionController::javaClass.name)
    }
}

fun TransactionResumeResult.toDTO(): TransactionResultDTO {
    return TransactionResultDTO(
        this.transaction.id.toString(),
        this.transaction.label,
        this.transaction.date,
        this.transaction.amount.toString(),
        this.transaction.isIncome,
        this.transaction.tag.toDTO(),
        this.accountAmount.amount.toString(),
        this.accountPreviewAmount.amount.toString(),
        this.transaction.isPreview
    )
}

data class ConfirmPreviewCommand(
    val userID: Long,
    val accountID: Long,
    val transactionID: Long
)