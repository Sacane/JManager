package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainImplementation
import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import java.time.Month

@Port(DomainSide.API)
interface TransactionResolver {
    fun openAccount(userId: UserId, token: Token, account: Account): Response<Account>
    fun createSheetAndAssociateItWithAccount(userId: UserId, token: Token, accountLabel: String, sheet: Sheet): Response<Sheet>
    fun retrieveSheetsByMonthAndYear(userId: UserId, token: Token, month: Month, year: Int, account: String): Response<List<Sheet>>
    fun retrieveAllRegisteredAccounts(userId: UserId, token: Token): Response<List<Account>>
    fun createCategory(userId: UserId, token: Token, category: Category): Response<Category>
    fun retrieveCategories(userId: UserId, token: Token): Response<List<Category>>
    fun retrieveAccountByIdentityAndLabel(userId: UserId, token: Token, label: String): Response<Account>
    fun deleteSheetsByIds(accountID: Long, sheetIds: List<Long>)
    fun deleteAccountById(profileID: UserId, accountID: Long): Response<Nothing>
    fun editAccount(userID: Long, account: Account, token: Token): Response<Account>
}

@DomainImplementation
class TransactionResolverImpl(private val register: TransactionRegister, private val userTransaction: UserTransaction): TransactionResolver {
    override fun openAccount(userId: UserId, token: Token, account: Account) : Response<Account> {
        val tokenResponse = userTransaction.getUserToken(userId) ?: return Response.invalid(TransactionRegister.missingUserMessage)
        if(tokenResponse.id != token.id) {
            return Response.timeout(TransactionRegister.timeoutMessage)
        }
        val ticket = userTransaction.findById(userId)
        if(ticket?.user?.accounts?.any { account.label == it.label } ?: return Response.notFound(TransactionRegister.missingUserMessage)) {
            return Response.invalid("Le profil contient déjà un compte avec ce label")
        }
        val userSaved = register.persist(userId, account) ?: return Response.invalid("Impossible de créer un compte")
        return Response.ok(userSaved.accounts().find { it.label == account.label } ?: return Response.notFound("Le compte créé n'a pas été sauvegardé correctement"))
    }

    override fun retrieveSheetsByMonthAndYear(userId: UserId, token: Token, month: Month, year: Int, account: String): Response<List<Sheet>> {
        val userResponse = userTransaction.findById(userId) ?: return Response.notFound(TransactionRegister.missingUserMessage)
        val user = userResponse.checkForIdentity(token) ?: return Response.timeout(TransactionRegister.timeoutMessage)
        val sheets = user.accounts()
            .find { it.label == account }
            ?.retrieveSheetSurroundByDate(month, year) ?: return Response.invalid("Aucun compte ne correspond au label indiqué")
        return Response.ok(sheets)
    }

    fun findAccount(userId: UserId, userToken: Token, labelAccount: String): Response<Account> {
        val ticket = userTransaction.findById(userId) ?: return Response.notFound(TransactionRegister.missingUserMessage)
        val userTokenResponse = userTransaction.getUserToken(userId) ?: return Response.timeout(TransactionRegister.timeoutMessage)
        val user = ticket.user
        if(userTokenResponse.id == userToken.id) {
            return Response.ok(user.accounts().find { it.label == labelAccount }!!)
        }
        return Response.timeout()
    }

    private fun updateSheetSold(account: Account){
        val sheets = account.sheets
        for(number in sheets!!.indices) {
            sheets[number].position = number
        }
        register.saveAllSheets(
            sheets.map{ sheet ->
                val lastRecord = account.sheets.find { it.position == sheet.position - 1 }
                sheet.also {
                    if(lastRecord == null) {
                        sheet.sold = account.sold
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

    override fun createSheetAndAssociateItWithAccount(userId: UserId, token: Token, accountLabel: String, sheet: Sheet): Response<Sheet> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.timeout()
        val account = register.findAccountByLabel(userId, accountLabel) ?: return Response.notFound()
        if(!account.sheets.isNullOrEmpty()) {
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

    override fun createCategory(userId: UserId, token: Token, category: Category): Response<Category> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.timeout()
        val categoryResponse = register.persist(userId, category) ?: return Response.invalid()
        return Response.ok(categoryResponse)
    }

    override fun retrieveAllRegisteredAccounts(userId: UserId, token: Token): Response<List<Account>> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid(TransactionRegister.missingUserMessage)
        userResponse.checkForIdentity(token) ?: return Response.invalid(TransactionRegister.timeoutMessage)
        return Response.ok(userResponse.user.accounts())
    }

    override fun retrieveCategories(userId: UserId, token: Token): Response<List<Category>> {
        val ticket = userTransaction.findById(userId) ?: return Response.invalid()
        ticket.checkForIdentity(token) ?: return Response.invalid()
        return Response.ok(ticket.user.categories())
    }

    override fun retrieveAccountByIdentityAndLabel(userId: UserId, token: Token, label: String): Response<Account> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.invalid()
        val targetOne = userResponse.user.accounts().find { it.label == label } ?: return Response.notFound()
        return Response.ok(targetOne)
    }

    override fun deleteSheetsByIds(accountID: Long, sheetIds: List<Long>) {
        val account: Account = register.findAccountById(accountID) ?: return
        if(account.sheets == null) return
        val isSheetOnList: (s: Sheet) -> Boolean = { sheetIds.contains(it.id) }
        account.cancelSheetsSupply(
            account.sheets.filter(isSheetOnList)
        )
        account.sheets.removeIf(isSheetOnList)
        updateSheetSold(account)
        register.persist(account)
        register.deleteAllSheetsById(sheetIds)
    }

    override fun deleteAccountById(profileID: UserId, accountID: Long) : Response<Nothing>{
        val profile = userTransaction.findUserById(profileID) ?: return Response.notFound()
        profile.accounts.removeIf { it.id == accountID }
        userTransaction.upsert(profile)!!
        register.deleteAccountByID(accountID)
        return Response.ok()
    }

    override fun editAccount(userID: Long, account: Account, token: Token): Response<Account> {
        val user = userTransaction.findById(UserId(userID)) ?: return Response.notFound()
        user.checkForIdentity(token) ?: return Response.invalid()
        val accountID = account.id ?: return Response.notFound()
        val oldAccount = register.findAccountById(accountID) ?: return Response.notFound()
        oldAccount.updateFrom(account)
        val registered = register.persist(oldAccount) ?: return Response.invalid()
        return Response.ok(registered)
    }
}
