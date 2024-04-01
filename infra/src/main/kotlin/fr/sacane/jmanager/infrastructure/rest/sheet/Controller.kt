package fr.sacane.jmanager.infrastructure.rest.sheet

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.port.api.SheetFeature
import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.toAmount
import fr.sacane.jmanager.infrastructure.rest.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.logging.Logger


@RestController
@RequestMapping("api/sheet")
@Adapter(Side.API)
class SheetController(private val sheetFeature: SheetFeature) {
    @PostMapping("/save")
    suspend fun createSheet(
        @RequestBody userAccountSheetDTO: UserAccountSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetSendDTO> {
        LOGGER.info("Trying to add the following sheet")
        LOGGER.info(userAccountSheetDTO.sheetDTO.toString())
        return sheetFeature.saveAndLink(
            userAccountSheetDTO.userId.id(),
            token.asTokenUUID(),
            userAccountSheetDTO.accountLabel,
            userAccountSheetDTO.sheetDTO.toModel()
        ).map {
            it.exportAmountValues { expense, income, sold ->
                SheetSendDTO(
                    it.label,
                    it.date,
                    expense.toAmount().toString(),
                    income.toAmount().toString(),
                    sold.toAmount().toString()
                )
            }
        }.toResponseEntity()
    }


    @DeleteMapping("delete/{userId}")
    fun deleteByIds(
        @PathVariable("userId") userId: Long,
        @RequestBody sheetIds: AccountSheetIdsDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Nothing>
        = sheetFeature.deleteSheetsByIds(UserId(userId), sheetIds.accountId, sheetIds.sheetIds, token.asTokenUUID()).let {
            ResponseEntity.ok().build()
        }


    @GetMapping
    fun getSheets(
        @RequestParam("userId") userId: Long,
        @RequestParam("month", required = false) month: Month?,
        @RequestParam("year") year: Int,
        @RequestParam("accountLabel") accountLabel: String,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetsAndAverageDTO> {
        LOGGER.info("Start getting sheets for account $accountLabel")
        val response = sheetFeature.retrieveSheetsByMonthAndYear(userId.id(), token.asTokenUUID(), month ?: LocalDate.now().month, year, accountLabel)
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(SheetsAndAverageDTO(response.mapTo { it!!.map { sheet -> sheet.toDTO() } }, 0.0))
    }

    @PostMapping("edit")
    fun editSheet(
        @RequestBody dto: UserIDSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetDTO>
        = sheetFeature.editSheet(dto.userId, dto.accountId, dto.sheet.toModel(), token.asTokenUUID())
            .mapBoth(
                {s -> ResponseEntity.ok(s!!.toDTO()) },
                {ResponseEntity.badRequest().build()}
            ) ?: ResponseEntity.badRequest().build<SheetDTO?>()
            .also { LOGGER.info("edit : ${dto.sheet}") }


    @GetMapping("/user/{userID}/find/{id}")
    fun findById(
        @PathVariable("userID") userID: Long,
        @PathVariable("id") sheetID: Long,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetDTO>
        = sheetFeature.findById(userID, sheetID, token.asTokenUUID())
            .mapTo {
                it ?: Response.invalid<SheetDTO>()
                Response.ok(it)
            }.map {
                it!!.toDTO()
            }.toResponseEntity()

    companion object {
        private val LOGGER: Logger = Logger.getLogger(SheetController::javaClass.name)
    }
}