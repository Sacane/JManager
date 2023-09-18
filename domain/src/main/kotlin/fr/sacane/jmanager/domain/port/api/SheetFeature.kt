package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainImplementation
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import java.time.Month

@Port(Side.API)
sealed interface SheetFeature {
    fun createSheetAndAssociateItWithAccount(userId: UserId, token: Token, accountLabel: String, sheet: Sheet): Response<Sheet>
    fun retrieveSheetsByMonthAndYear(userId: UserId, token: Token, month: Month, year: Int, account: String): Response<List<Sheet>>
    fun editSheet(userID: Long, accountID: Long, sheet: Sheet, token: Token): Response<Sheet>
    fun findById(userID: Long, id: Long, token: Token): Response<Sheet>
    fun deleteSheetsByIds(accountID: Long, sheetIds: List<Long>)
}

@DomainImplementation
class SheetFeatureImplementation(
    private val register: TransactionRegister,
    private val userTransaction: UserTransaction,
    private val loginManager: LoginManager
): SheetFeature{

    private fun updateSheetSold(account: Account, update: Boolean = true){
        val sheets = account.sheets
        for(number in sheets.indices) {
            sheets[number].position = number
        }
        register.saveAllSheets(
            sheets.map{ sheet ->
                val lastRecord = account.sheets.find { it.position == sheet.position - 1 }
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

    override fun createSheetAndAssociateItWithAccount(
        userId: UserId,
        token: Token,
        accountLabel: String,
        sheet: Sheet
    ): Response<Sheet> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.timeout()
        val account = register.findAccountByLabel(userId, accountLabel) ?: return Response.notFound()
        if(account.sheets.isNotEmpty()) {
            val lastRecord = account.sheets.filter { it.date <= sheet.date }.maxByOrNull { it.position } ?: return Response.notFound()
            sheet.position = lastRecord.position + 1
            sheet.updateSoldStartingWith(lastRecord.sold)
            updateSheetPosition(account.id!!, sheet.date.year, sheet.date.month)
        } else {
            sheet.updateSoldStartingWith(account.sold)
        }
        register.persist(userId, accountLabel, sheet) ?: return Response.invalid()
        return Response.ok(sheet)
    }

    override fun retrieveSheetsByMonthAndYear(
        userId: UserId,
        token: Token,
        month: Month,
        year: Int,
        account: String
    ): Response<List<Sheet>> {
        val userResponse = userTransaction.findById(userId) ?: return Response.notFound(TransactionRegister.missingUserMessage)
        val user = userResponse.checkForIdentity(token) ?: return Response.timeout(TransactionRegister.timeoutMessage)
        val sheets = user.accounts()
            .find { it.label == account }
            ?.retrieveSheetSurroundByDate(month, year) ?: return Response.invalid("Aucun compte ne correspond au label indiqué")
        return Response.ok(sheets)
    }

    override fun editSheet(userID: Long, accountID: Long, sheet: Sheet, token: Token): Response<Sheet> {
        if(sheet.id == null) return Response.notFound("L'ID de la transaction n'existe pas")
        val user = userTransaction.findById(UserId(userID))?.checkForIdentity(token) ?: return Response.notFound("L'utlisateur est inconnue")
        val acc = user.accounts.find { it.id == accountID }
        val sheetFromResource = acc?.sheets?.find { it.id == sheet.id } ?: return Response.notFound("Aucune transaction n'existe avec l'ID suivant : ${sheet.id}")
        sheetFromResource.updateFromOther(sheet)
        if(sheetFromResource.position == 0){
            acc.setSoldFromSheet(sheetFromResource)
        }
        println(acc.sheets)
        updateSheetSold(acc, false)
        acc.updateSoldByLastSheet()
        println(acc.sold)
        return register.save(acc).run {
            this ?: return Response.invalid("Une erreur est survenu lors de la sauvegarde de la transaction")
            println(this.sold)
            Response.ok(sheetFromResource)
        }
    }

    override fun findById(userID: Long, id: Long, token: Token): Response<Sheet> {
        userTransaction.findById(UserId(userID))?.checkForIdentity(token) ?: return Response.notFound("L'utlisateur est inconnue")
        val sheet = register.findSheetByID(id) ?: return Response.notFound("La transaction n'existe pas")
        return Response.ok(sheet)
    }

    override fun deleteSheetsByIds(accountID: Long, sheetIds: List<Long>) {
        val account: Account = register.findAccountById(accountID) ?: return
        val isSheetOnList: (s: Sheet) -> Boolean = { sheetIds.contains(it.id) }
        account.cancelSheetsSupply(
            account.sheets.filter(isSheetOnList)
        )
        account.sheets.removeIf(isSheetOnList)
        updateSheetSold(account)
        register.persist(account)
        register.deleteAllSheetsById(sheetIds)
    }

}