package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.LeftPort
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserTransaction
import java.time.Month

// TODO Instead of return Response timeout, this should refresh the token if they match
@LeftPort
class TransactionResolverImpl(private val register: TransactionRegister, private val userTransaction: UserTransaction): TransactionResolver {
    override fun openAccount(userId: UserId, token: Token, account: Account) : Response<Account> {
        val tokenResponse = userTransaction.getUserToken(userId) ?: return Response.invalid()
        if(tokenResponse.id != token.id) {
            return Response.timeout()
        }
        val userSaved = register.persist(userId, account) ?: return Response.invalid()
        return Response.ok(userSaved.accounts().find { it.label() == account.label() }!!)
    }

    override fun retrieveSheetsByMonthAndYear(userId: UserId, token: Token, month: Month, year: Int, account: String): Response<List<Sheet>> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid()
        val user = userResponse.checkForIdentity(token) ?: return Response.timeout()
        val sheets = user.accounts()
            .find { it.label() == account }
            ?.retrieveSheetSurroundByDate(month, year) ?: return Response.invalid()
        return Response.ok(sheets)
    }

    fun findAccount(userId: UserId, userToken: Token, labelAccount: String): Response<Account> {
        val ticket = userTransaction.findById(userId) ?: return Response.notFound()
        val userTokenResponse = userTransaction.getUserToken(userId) ?: return Response.timeout()
        val user = ticket.user
        if(userTokenResponse.id == userToken.id) {
            return Response.ok(user.accounts().find { it.label() == labelAccount }!!)
        }
        return Response.timeout()
    }


    override fun createSheetAndAssociateItWithAccount(userId: UserId, token: Token, accountLabel: String, sheet: Sheet): Response<Sheet> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.timeout()
        val account = register.findAccountByLabel(userId, accountLabel) ?: return Response.notFound()
        sheet.accountAmount = account.amount().plus(sheet.income).minus(sheet.expenses)
        register.persist(userId, accountLabel, sheet) ?: return Response.notFound()
        return Response.ok(sheet)
    }

    override fun createCategory(userId: UserId, token: Token, category: Category): Response<Category> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.timeout()
        val categoryResponse = register.persist(userId, category) ?: return Response.invalid()
        return Response.ok(categoryResponse)
    }

    override fun retrieveAllRegisteredAccounts(userId: UserId, token: Token): Response<List<Account>> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.invalid()
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
        val targetOne = userResponse.user.accounts().find { it.label() == label } ?: return Response.notFound()
        return Response.ok(targetOne)
    }

    fun removeCategory(id: UserId, token: Token, label: String): Response<Category?> {
        val userTicket = userTransaction.findById(id) ?: return Response.notFound()
        val user = userTicket.checkForIdentity(token) ?: return Response.timeout()
//        val responseEntity = port.removeCategory(id, label) ?: return Response.invalid()
        val targetCategory = user.categories().find { it.label == label } ?: return Response.notFound()
        register.remove(targetCategory)
        return Response.ok(targetCategory)
    }

    override fun deleteByIds(accountID: Long, sheetIds: List<Long>) {
        val account = register.findAccountById(accountID) ?: return
        account.sheets?.removeIf {sheetIds.contains(it.id) }
        //TODO update account's sold after delete
        register.persist(account)
    }

    override fun deleteAccountById(profileID: UserId, accountID: Long) : Response<Nothing>{
        val profile = userTransaction.findById(profileID) ?: return Response.notFound()
        profile.user.accounts().removeIf {it.id() == accountID}
        userTransaction.register(profile.user) ?: return Response.notFound()
        return Response.ok()
    }
}