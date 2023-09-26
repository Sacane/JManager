package fr.sacane.jmanager.infrastructure.rest.sheet

import fr.sacane.jmanager.domain.hexadoc.Adapter
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.port.api.SheetFeature
import fr.sacane.jmanager.domain.asTokenUUID
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.infrastructure.rest.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.logging.Logger


@RestController
@RequestMapping("/sheet")
@Adapter(Side.API)
class SheetController(private val transactionResolver: SheetFeature) {
    @PostMapping("/save")
    suspend fun createSheet(
        @RequestBody userAccountSheetDTO: UserAccountSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetSendDTO> {
        val queryResponse = transactionResolver.createSheetAndAssociateItWithAccount(
            userAccountSheetDTO.userId.id(),
            token.asTokenUUID(),
            userAccountSheetDTO.accountLabel,
            userAccountSheetDTO.sheetDTO.toModel()
        )
        if (queryResponse.status.isFailure()) return ResponseEntity.badRequest().build()
        return queryResponse.map {
            SheetSendDTO(it.label, it.date, it.expenses, it.income, it.sold)
        }.toResponseEntity()
    }

    @DeleteMapping("delete/{userId}")
    fun deleteByIds(
        @PathVariable("userId") userId: Long,
        @RequestBody sheetIds: AccountSheetIdsDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Nothing>
        = transactionResolver.deleteSheetsByIds(UserId(userId), sheetIds.accountId, sheetIds.sheetIds, token.asTokenUUID()).let {
            ResponseEntity.ok().build()
        }


    @PostMapping(path=["get"])
    fun getSheets(
        @RequestBody dto: UserSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetsAndAverageDTO> {
        val response = transactionResolver.retrieveSheetsByMonthAndYear(dto.userId.id(), token.asTokenUUID(), dto.month, dto.year, dto.accountLabel)
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(SheetsAndAverageDTO(response.mapTo { it -> it!!.map { it.toDTO() } }, 0.0))
    }

    @PostMapping("edit")
    fun editSheet(
        @RequestBody dto: UserIDSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetDTO>
        = transactionResolver.editSheet(dto.userId, dto.accountId, dto.sheet.toModel(), token.asTokenUUID())
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
        = transactionResolver.findById(userID, sheetID, token.asTokenUUID())
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