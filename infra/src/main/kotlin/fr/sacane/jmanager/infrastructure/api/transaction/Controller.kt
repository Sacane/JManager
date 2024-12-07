package fr.sacane.jmanager.infrastructure.api.transaction

import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.utils.Response
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.api.TransactionFeature
import fr.sacane.jmanager.infrastructure.api.id
import fr.sacane.jmanager.infrastructure.api.toDTO
import fr.sacane.jmanager.infrastructure.api.toModel
import fr.sacane.jmanager.infrastructure.api.toResponseEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.Month
import java.util.logging.Logger

@RestController
@RequestMapping("api/sheet")
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
            it.transaction.exportAmountValues { expense, income ->
                TransactionResultDTO(
                    it.transaction.id.toString(),
                    it.transaction.label,
                    it.transaction.date,
                    expense.toString(),
                    income,
                    it.transaction.tag.toDTO(),
                    it.accountAmount.amount.toString(),
                    it.accountPreviewAmount.amount.toString(),
                    it.transaction.isPreview
                )
            }
        }.toResponseEntity().apply {
            logger.info("Creating new transaction => ${userAccountSheetDTO.sheetDTO} TO => ${this.body?.tagDTO}")
        }
    }


    @DeleteMapping("delete/{userId}")
    fun deleteByIds(
        @PathVariable("userId") userId: Long,
        @RequestBody sheetIds: AccountSheetIdsDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Nothing>
        = transactionFeature.deleteSheetsByIds(UserId(userId), sheetIds.accountId, sheetIds.sheetIds, token.asTokenUUID()).let {
            ResponseEntity.ok().build()
        }


    @GetMapping
    fun getTransactionsByMonthAndYearAndAccountLabel(
        @RequestParam("userId") userId: Long,
        @RequestParam("month", required = false) month: Month?,
        @RequestParam("year") year: Int,
        @RequestParam("accountLabel") accountLabel: String,
        @RequestHeader("Authorization") token: String
        ): ResponseEntity<TransactionList> {
        LOGGER.info("Start getting transactions for account $accountLabel")
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
            .map { it.transaction.exportAmountValues { expense, income ->
                    TransactionResultDTO(
                        it.transaction.id.toString(),
                        it.transaction.label,
                        it.transaction.date,
                        expense.toString(),
                        income,
                        it.transaction.tag.toDTO(),
                        it.accountAmount.amount.toString(),
                        it.accountPreviewAmount.amount.toString(),
                        it.transaction.isPreview
                    )
                }
            }.toResponseEntity()
            .also { LOGGER.info("Transaction edited successfully : ${dto.sheet}") }
    }


    @GetMapping("transaction/{id}")
    fun findById(
        @RequestParam("userID") userID: Long,
        @PathVariable("id") sheetID: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetDTO>
        = transactionFeature.findById(userID, sheetID, token.asTokenUUID())
            .mapTo {
                it ?: Response.invalid<SheetDTO>()
                Response.ok(it)
            }.map {
                it!!.toDTO()
            }.toResponseEntity()

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
        ).map { it.transaction.exportAmountValues { expense, income ->
                TransactionResultDTO(
                    it.transaction.id.toString(),
                    it.transaction.label,
                    it.transaction.date,
                    expense.toString(),
                    income,
                    it.transaction.tag.toDTO(),
                    it.accountAmount.amount.toString(),
                    it.accountPreviewAmount.amount.toString(),
                    it.transaction.isPreview
                )
            }
        }.toResponseEntity()
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(TransactionController::javaClass.name)
    }
}

data class ConfirmPreviewCommand(
    val userID: Long,
    val accountID: Long,
    val transactionID: Long
)