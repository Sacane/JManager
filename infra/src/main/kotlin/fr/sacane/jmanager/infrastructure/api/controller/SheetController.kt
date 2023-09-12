package fr.sacane.jmanager.infrastructure.api.controller

import fr.sacane.jmanager.infrastructure.api.*
import fr.sacane.jmanager.infrastructure.api.adapters.TransactionValidator
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.logging.Logger


@RestController
@RequestMapping("/sheet")
class SheetController(
    private val transactionValidator: TransactionValidator
) {
    private fun extractToken(authorization: String): String{
        return authorization.replace("Bearer ", "");
    }

    @PostMapping("/save")
    suspend fun createSheet(@RequestBody userAccountSheetDTO: UserAccountSheetDTO, @RequestHeader("Authorization") token: String): ResponseEntity<SheetSendDTO> {
        return transactionValidator.saveSheet(
            userAccountSheetDTO.userId,
            userAccountSheetDTO.accountLabel,
            userAccountSheetDTO.sheetDTO,
            extractToken(token)
        ).apply { LOGGER.info("Sheet has been created") }
    }

    @DeleteMapping("delete")
    fun deleteByIds(@RequestBody sheetIds: AccountSheetIdsDTO, @RequestHeader("Authorization") token: String): ResponseEntity<Nothing>{
        return transactionValidator.deleteSheetByIds(sheetIds)
    }

    @PostMapping(path=["get"])
    fun getSheets(@RequestBody dto: UserSheetDTO, @RequestHeader("Authorization") token: String): ResponseEntity<SheetsAndAverageDTO>{
        return transactionValidator.getSheetAccountByDate(dto, extractToken(token))
    }

    @PostMapping("edit")
    fun editSheet(@RequestBody dto: UserIDSheetDTO, @RequestHeader("Authorization") token: String): ResponseEntity<SheetDTO> {
        LOGGER.info("edit : ${dto.sheet}")
        return transactionValidator.editSheet(dto.userId, dto.accountId, dto.sheet, extractToken(token))
    }

    @GetMapping("/user/{userID}/find/{id}")
    fun findById(@PathVariable("userID") userID: Long, @PathVariable("id") sheetID: Long, @RequestHeader("Authorization") token: String): ResponseEntity<SheetDTO> {
        return transactionValidator.findSheetById(userID, sheetID, extractToken(token))
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(SheetController::javaClass.name)
    }
}