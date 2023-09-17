package fr.sacane.jmanager.infrastructure.rest.sheet

import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Token
import fr.sacane.jmanager.domain.port.api.SheetFeature
import fr.sacane.jmanager.infrastructure.extractToken
import fr.sacane.jmanager.infrastructure.rest.*
import fr.sacane.jmanager.infrastructure.rest.id
import fr.sacane.jmanager.infrastructure.rest.toDTO
import fr.sacane.jmanager.infrastructure.rest.toModel
import fr.sacane.jmanager.infrastructure.rest.toResponseEntity
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import java.util.logging.Logger


@RestController
@RequestMapping("/sheet")
class SheetController(
    private val transactionResolver: SheetFeature
) {

    @PostMapping("/save")
    suspend fun createSheet(
        @RequestBody userAccountSheetDTO: UserAccountSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetSendDTO> {
        val queryResponse = transactionResolver.createSheetAndAssociateItWithAccount(
            userAccountSheetDTO.userId.id(),
            Token(UUID.fromString(extractToken(token))),
            userAccountSheetDTO.accountLabel,
            userAccountSheetDTO.sheetDTO.toModel()
        )
        if (queryResponse.status.isFailure()) return ResponseEntity.badRequest().build()
        return queryResponse.map { SheetSendDTO(it!!.label, it.date, it.expenses, it.income, it.sold) }
            .toResponseEntity()
    }

    @DeleteMapping("delete")
    fun deleteByIds(
        @RequestBody sheetIds: AccountSheetIdsDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<Nothing>
        = transactionResolver.deleteSheetsByIds(sheetIds.accountId, sheetIds.sheetIds).let {
            ResponseEntity.ok().build()
        }


    @PostMapping(path=["get"])
    fun getSheets(
        @RequestBody dto: UserSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetsAndAverageDTO> {
        val response = transactionResolver.retrieveSheetsByMonthAndYear(dto.userId.id(), Token(UUID.fromString(extractToken(token)), null, UUID.randomUUID()), dto.month, dto.year, dto.accountLabel)
        if(response.status.isFailure()) return ResponseEntity.badRequest().build()
        return ResponseEntity.ok(SheetsAndAverageDTO(response.mapTo { it -> it!!.map { it.toDTO() } }, 0.0))
    }

    @PostMapping("edit")
    fun editSheet(
        @RequestBody dto: UserIDSheetDTO,
        @RequestHeader("Authorization") token: String
    ): ResponseEntity<SheetDTO>
        = transactionResolver.editSheet(dto.userId, dto.accountId, dto.sheet.toModel(), Token(UUID.fromString(extractToken(token))))
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
        = transactionResolver.findById(userID, sheetID, Token(UUID.fromString(extractToken(token))))
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