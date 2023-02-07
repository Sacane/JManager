package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.domain.hexadoc.PortToLeft
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import java.time.Month

// TODO Instead of return Response timeout, this should refresh the token if they match
@PortToLeft
class TransactionReaderAdapter(private val port: TransactionRegister, private val userPort: UserTransaction) {
    fun saveAccount(userId: UserId, token: Token, account: Account) : Response<Account> {
        val tokenResponse = userPort.getUserToken(userId) ?: return Response.invalid()
        if(tokenResponse.id != token.id) return Response.timeout()
        val accountSaved = port.saveAccount(userId, account) ?: return Response.invalid()
        return Response.ok(accountSaved)
    }

    fun sheetByDateAndAccount(userId: UserId, token: Token, month: Month, year: Int, account: String): Response<List<Sheet>> {
        val userResponse = userPort.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.timeout()
        return Response.ok(port.getSheetsByDateAndAccount(userId, month, year, account))
    }

    fun findAccount(userId: UserId, userToken: Token, labelAccount: String): Response<Account> {
        val ticket = userPort.findById(userId) ?: return Response.invalid()
        val userTokenResponse = userPort.getUserToken(userId) ?: return Response.timeout()
        val user = ticket.user
        if(userTokenResponse.id == userToken.id) {
            return Response.ok(user?.accounts()?.find { it.label() == labelAccount }!!)
        }
        return Response.timeout()
    }


    fun saveSheet(userId: UserId, token: Token, accountLabel: String, sheet: Sheet): Response<Sheet> {
        val userResponse = userPort.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.invalid()
        val savedSheet = port.saveSheet(userId, accountLabel, sheet) ?: return Response.invalid()
        return Response.ok(savedSheet)
    }

    fun addCategory(userId: UserId, token: Token, category: Category): Response<Category> {
        val userResponse = userPort.findById(userId) ?: return Response.invalid()
        val userToken = userResponse.token ?: return Response.invalid()
        if(userToken.id != token.id) {
            return Response.timeout()
        }
        val categoryResponse = port.saveCategory(userId, category) ?: return Response.invalid()
        return Response.ok(categoryResponse)
    }

    fun getAccountByUser(userId: UserId, token: Token): Response<List<Account>?> {
        val userResponse = userPort.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.invalid()
        return Response.ok(port.getAccounts(userId))
    }

    fun retrieveAllCategoryOfUser(userId: UserId, token: Token): Response<List<Category>> {
        val ticket = userPort.findById(userId) ?: return Response.invalid()
        if(ticket.user == null || ticket.token == null) return Response.invalid()
        ticket.checkForIdentity(token) ?: return Response.invalid()
        return Response.ok(ticket.user.categories())
    }

    fun removeCategory(id: UserId, token: Token, label: String): Response<Category?> {
        val userTicket = userPort.findById(id) ?: return Response.invalid()
        userTicket.checkForIdentity(token) ?: return Response.timeout()
        val responseEntity = port.removeCategory(id, label)
        return Response.ok(responseEntity)
    }
}
