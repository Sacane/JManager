package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.time.LocalDate
import java.time.Month
import java.util.UUID

@Port(Side.API)
sealed interface SheetFeature {
    fun saveAndLink(userId: UserId, token: UUID, accountLabel: String, sheet: Sheet): Response<Sheet>
    fun retrieveSheetsByMonthAndYear(userId: UserId, token: UUID, month: Month, year: Int, account: String): Response<List<Sheet>>
    fun editSheet(userID: Long, accountID: Long, sheet: Sheet, token: UUID): Response<Sheet>
    fun findById(userID: Long, id: Long, token: UUID): Response<Sheet>
    fun deleteSheetsByIds(userId: UserId, accountID: Long, sheetIds: List<Long>, token: UUID)
}

@DomainService
class SheetFeatureImplementation(
    private val register: TransactionRegister,
    private val userRepository: UserRepository,
    private val session: SessionManager
): SheetFeature{

    private fun updateSheetSoldFrom(account: Account, month: Month, update: Boolean = true){
        val sheets = account.sheets.filter { it.date.month == month }
        for(number in sheets.indices) {
            if(number == 0) continue
            sheets[number].position = sheets[number - 1].position
        }
        register.saveAllSheets(
            sheets.map{ sheet ->
                val lastRecord = sheets.find { it.position == sheet.position - 1 }
                sheet.also {
                    if(lastRecord == null) {
                        if(update) sheet.sold = account.sold
                    } else {
                        sheet.updateSoldStartingWith(lastRecord.sold)
                    }
                }
            }.toList()
        )
    }
    private fun updateSheetPosition(accountID: Long, year: Int, month: Month) {
        val account = register.findAccountById(accountID)
        val sheets = account?.sheets
            ?.filter { it.date.year == year && it.date.month == month }
            ?.sortedBy { it.position }
            ?: return

        for(number in sheets.indices) {
            sheets[number].position = number
        }
        register.saveAllSheets(sheets)
    }

    override fun saveAndLink(
        userId: UserId,
        token: UUID,
        accountLabel: String,
        sheet: Sheet
    ): Response<Sheet> = session.authenticate(userId, token) {
        val account = register.findAccountByLabel(userId, accountLabel) ?: return@authenticate Response.notFound()
        if(account.sheets.isNotEmpty()) {
            val lastRecord = account.sheets
                .filter { it.date <= sheet.date }
                .maxByOrNull { it.position }
            if(lastRecord == null) {
                sheet.position = 0
                sheet.updateSoldStartingWith(account.sold)
            } else {
                sheet.position = lastRecord.position + 1
                sheet.updateSoldStartingWith(lastRecord.sold)
            }
            updateSheetPosition(account.id!!, sheet.date.year, sheet.date.month)
        } else {
            sheet.updateSoldStartingWith(account.sold)
        }
        register.persist(userId, accountLabel, sheet) ?: return@authenticate Response.invalid()
        Response.ok(sheet)
    }



    override fun retrieveSheetsByMonthAndYear(
        userId: UserId,
        token: UUID,
        month: Month,
        year: Int,
        account: String
    ): Response<List<Sheet>> = session.authenticate(userId, token) {
        val user = userRepository.findUserById(userId) ?: return@authenticate Response.notFound("L'utilisateur n'existe pas en base")
        Response.ok(user.accounts
            .find { it.label == account }
            ?.retrieveSheetSurroundByDate(month, year)
            ?: return@authenticate Response.notFound("Aucun compte ne correspond au label indiqué")
        )
    }

    override fun editSheet(
        userID: Long,
        accountID: Long,
        sheet: Sheet,
        token: UUID
    ): Response<Sheet> = session.authenticate(UserId(userID), token, roleUser) {
        val user = userRepository.findUserById(UserId(userID)) ?: return@authenticate Response.notFound("L'utilisateur n'existe pas en base")
        if(sheet.id == null) return@authenticate Response.notFound("L'ID de la transaction n'existe pas")
        val acc = user.accounts.find { it.id == accountID }
        val sheetFromResource = acc?.sheets?.find { it.id == sheet.id } ?: return@authenticate Response.notFound("Aucune transaction n'existe avec l'ID suivant : ${sheet.id}")
        sheetFromResource.updateFromOther(sheet)
        if(sheetFromResource.position == 0){
            acc.setSoldFromSheet(sheetFromResource)
        }
        updateSheetSoldFrom(acc, sheet.date.month, false)
        acc.updateSoldByLastSheet()
        return@authenticate register.save(acc).run {
            this ?: return@authenticate Response.invalid("Une erreur est survenu lors de la sauvegarde de la transaction")
            Response.ok(sheetFromResource)
        }
    }

    override fun findById(
        userID: Long,
        id: Long,
        token: UUID
    ): Response<Sheet> = session.authenticate(UserId(userID), token, roleUser) {
        val sheet = register.findSheetByID(id) ?: return@authenticate Response.notFound("La transaction n'existe pas")
        Response.ok(sheet)
    }

    override fun deleteSheetsByIds(userId: UserId, accountID: Long, sheetIds: List<Long>, token: UUID) {
        val account: Account = register.findAccountById(accountID) ?: return
        val isSheetOnList: (s: Sheet) -> Boolean = { sheetIds.contains(it.id) }
        val month = account.sheets.filter(isSheetOnList)[0].date.month
        account.cancelSheetsAmount(account.sheets.filter(isSheetOnList))
        account.sheets.removeIf(isSheetOnList)
        updateSheetSoldFrom(account, month)
        register.persist(account)
        register.deleteAllSheetsById(sheetIds)
    }

}