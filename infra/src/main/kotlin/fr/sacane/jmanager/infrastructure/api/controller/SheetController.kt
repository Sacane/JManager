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
            SheetDTO(
                userAccountSheetDTO.sheetDTO.id,
                userAccountSheetDTO.sheetDTO.label,
                userAccountSheetDTO.sheetDTO.expenses,
                userAccountSheetDTO.sheetDTO.income,
                userAccountSheetDTO.sheetDTO.date.plusDays(1),
                userAccountSheetDTO.sheetDTO.accountAmount
            ),
            extractToken(token)
        ).apply { LOGGER.info("Sheet has been created") }
    }

    @DeleteMapping("delete")
    fun deleteByIds(@RequestBody sheetIds: AccountSheetIdsDTO, @RequestHeader("Authorization") token: String): ResponseEntity<Nothing>{
        return transactionValidator.deleteSheetByIds(sheetIds)
    }

    @PostMapping(path=["get"])
    suspend fun getSheets(@RequestBody dto: UserSheetDTO, @RequestHeader("Authorization") token: String): ResponseEntity<SheetsAndAverageDTO>{
        LOGGER.info("CHECK FOR SHEETS : $dto")
        return transactionValidator.getSheetAccountByDate(dto, extractToken(token))
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger(SheetController::javaClass.name)
    }
}