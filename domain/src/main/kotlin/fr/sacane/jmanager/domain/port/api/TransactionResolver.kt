package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainImplementation
import fr.sacane.jmanager.domain.hexadoc.DomainSide
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.models.Response.Companion.invalid
import fr.sacane.jmanager.domain.models.Response.Companion.notFound
import fr.sacane.jmanager.domain.models.Response.Companion.ok
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
    fun editSheet(userID: Long, accountID: Long, sheet: Sheet, token: Token): Response<Sheet>
    fun findById(userID: Long, id: Long, token: Token): Response<Sheet>
    fun findAccountById(userId: UserId, accountID: Long, token: Token): Response<Account>
}

@DomainImplementation
class TransactionResolverImpl(private val register: TransactionRegister, private val userTransaction: UserTransaction): TransactionResolver {
    override fun openAccount(userId: UserId, token: Token, account: Account) : Response<Account> {
        val tokenResponse = userTransaction.getUserToken(userId) ?: return invalid(TransactionRegister.missingUserMessage)
        if(tokenResponse.id != token.id) {
            return Response.timeout(TransactionRegister.timeoutMessage)
        }
        val ticket = userTransaction.findById(userId)
        if(ticket?.user?.accounts?.any { account.label == it.label } ?: return notFound(TransactionRegister.missingUserMessage)) {
            return invalid("Le profil contient déjà un compte avec ce label")
        }
        val userSaved = register.persist(userId, account) ?: return invalid("Impossible de créer un compte")
        return ok(userSaved.accounts().find { it.label == account.label } ?: return notFound("Le compte créé n'a pas été sauvegardé correctement"))
    }

    override fun retrieveSheetsByMonthAndYear(userId: UserId, token: Token, month: Month, year: Int, account: String): Response<List<Sheet>> {
        val userResponse = userTransaction.findById(userId) ?: return notFound(TransactionRegister.missingUserMessage)
        val user = userResponse.checkForIdentity(token) ?: return Response.timeout(TransactionRegister.timeoutMessage)
        val sheets = user.accounts()
            .find { it.label == account }
            ?.retrieveSheetSurroundByDate(month, year) ?: return invalid("Aucun compte ne correspond au label indiqué")
        return ok(sheets)
    }

    fun findAccount(userId: UserId, userToken: Token, labelAccount: String): Response<Account> {
        val ticket = userTransaction.findById(userId) ?: return notFound(TransactionRegister.missingUserMessage)
        val userTokenResponse = userTransaction.getUserToken(userId) ?: return Response.timeout(TransactionRegister.timeoutMessage)
        val user = ticket.user
        if(userTokenResponse.id == userToken.id) {
            return ok(user.accounts().find { it.label == labelAccount }!!)
        }
        return Response.timeout()
    }

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

    override fun createSheetAndAssociateItWithAccount(userId: UserId, token: Token, accountLabel: String, sheet: Sheet): Response<Sheet> {
        val userResponse = userTransaction.findById(userId) ?: return invalid()
        userResponse.checkForIdentity(token) ?: return Response.timeout()
        val account = register.findAccountByLabel(userId, accountLabel) ?: return notFound()
        if(account.sheets.isNotEmpty()) {
            val lastRecord = account.sheets.filter { it.date <= sheet.date }.maxByOrNull { it.position } ?: return notFound()
            sheet.position = lastRecord.position + 1
            sheet.updateSoldStartingWith(lastRecord.sold)
            updateSheetPosition(account.id!!, sheet.date.year, sheet.date.month)
        } else {
            sheet.updateSoldStartingWith(account.sold)
        }
        register.persist(userId, accountLabel, sheet) ?: return invalid()
        return ok(sheet)
    }

    override fun createCategory(userId: UserId, token: Token, category: Category): Response<Category> {
        val userResponse = userTransaction.findById(userId) ?: return invalid()
        userResponse.checkForIdentity(token) ?: return Response.timeout()
        val categoryResponse = register.persist(userId, category) ?: return invalid()
        return ok(categoryResponse)
    }

    override fun retrieveAllRegisteredAccounts(userId: UserId, token: Token): Response<List<Account>> {
        val userResponse = userTransaction.findById(userId) ?: return invalid(TransactionRegister.missingUserMessage)
        userResponse.checkForIdentity(token) ?: return invalid(TransactionRegister.timeoutMessage)
        return ok(userResponse.user.accounts())
    }

    override fun retrieveCategories(userId: UserId, token: Token): Response<List<Category>> {
        val ticket = userTransaction.findById(userId) ?: return invalid()
        ticket.checkForIdentity(token) ?: return invalid()
        return ok(ticket.user.categories())
    }

    override fun retrieveAccountByIdentityAndLabel(userId: UserId, token: Token, label: String): Response<Account> {
        val userResponse = userTransaction.findById(userId) ?: return invalid()
        userResponse.checkForIdentity(token) ?: return invalid()
        val targetOne = userResponse.user.accounts().find { it.label == label } ?: return notFound()
        return ok(targetOne)
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

    override fun deleteAccountById(profileID: UserId, accountID: Long) : Response<Nothing>{
        val profile = userTransaction.findUserById(profileID) ?: return notFound()
        profile.accounts.removeIf { it.id == accountID }
        userTransaction.upsert(profile)!!
        register.deleteAccountByID(accountID)
        return ok()
    }

    override fun editAccount(userID: Long, account: Account, token: Token): Response<Account> {
        val user = userTransaction.findById(UserId(userID)) ?: return notFound()
        user.checkForIdentity(token) ?: return invalid()
        val accountID = account.id ?: return notFound()
        val oldAccount = register.findAccountById(accountID) ?: return notFound()
        oldAccount.updateFrom(account)
        val registered = register.persist(oldAccount) ?: return invalid()
        return ok(registered)
    }

    override fun editSheet(userID: Long, accountID: Long, sheet: Sheet, token: Token): Response<Sheet> {
        if(sheet.id == null) return notFound("L'ID de la transaction n'existe pas")
        val user = userTransaction.findById(UserId(userID))?.checkForIdentity(token) ?: return notFound("L'utlisateur est inconnue")
        val acc = user.accounts.find { it.id == accountID }
        val sheetFromResource = acc?.sheets?.find { it.id == sheet.id } ?: return notFound("Aucune transaction n'existe avec l'ID suivant : ${sheet.id}")
        sheetFromResource.updateFromOther(sheet)
        if(sheetFromResource.position == 0){
            acc.setSoldFromSheet(sheetFromResource)
        }
        println(acc.sheets)
        updateSheetSold(acc, false)
        acc.updateSoldByLastSheet()
        println(acc.sold)
        return register.save(acc).run {
            this ?: return invalid("Une erreur est survenu lors de la sauvegarde de la transaction")
            println(this.sold)
            ok(sheetFromResource)
        }
    }

    override fun findById(userID: Long, id: Long, token: Token): Response<Sheet> {
        userTransaction.findById(UserId(userID))?.checkForIdentity(token) ?: return notFound("L'utlisateur est inconnue")
        val sheet = register.findSheetByID(id) ?: return notFound("La transaction n'existe pas")
        return ok(sheet)
    }

    override fun findAccountById(userId: UserId, accountID: Long, token: Token): Response<Account> {
        userTransaction.findById(userId)?.checkForIdentity(token) ?: return notFound("L'utlisateur est inconnue")
        return register.findAccountById(accountID).run {
            this ?: return notFound("Le compte n'existe pas")
            ok(this)
        }
    }
}
