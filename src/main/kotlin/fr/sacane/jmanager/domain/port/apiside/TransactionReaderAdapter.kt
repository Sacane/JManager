package fr.sacane.jmanager.domain.port.apiside

import fr.sacane.jmanager.common.hexadoc.PortToLeft
import fr.sacane.jmanager.domain.model.*
import fr.sacane.jmanager.domain.port.serverside.TransactionRegister
import fr.sacane.jmanager.domain.port.serverside.UserTransaction
import java.time.Month

// TODO Instead of return Response timeout, this should refresh the token if they match
@PortToLeft
class TransactionReaderAdapter(private val port: TransactionRegister, private val userPort: UserTransaction) {
    fun saveAccount(userId: UserId, token: Token, account: Account) : Response<Account>{
        val tokenResponse = userPort.getUserToken(userId) ?: return Response.invalid()
        if(tokenResponse.id != token.id) return Response.timeout()
        val accountSaved = port.saveAccount(userId, account) ?: return Response.invalid()
        return Response.ok(accountSaved)
    }

    fun sheetByDateAndAccount(userId: UserId, month: Month, year: Int, account: String): List<Sheet> {
        return port.getSheetsByDateAndAccount(userId, month, year, account)
    }

    fun findAccount(userId: UserId, userToken: Token, labelAccount: String): Response<Account> {
        val ticket = userPort.findById(userId)
        if(ticket.state.isFailure()){
            return Response.invalid()
        }
        val userTokenResponse = userPort.getUserToken(userId) ?: return Response.timeout()
        val user = ticket.user
        if(userTokenResponse.id == userToken.id) {
            return Response.ok(user?.accounts()?.find { it.label() == labelAccount }!!)
        }
        return Response.timeout()
    }


    fun saveSheet(userId: UserId, token: Token, accountLabel: String, sheet: Sheet): Response<Sheet> {
        val userResponse = userPort.findById(userId)
        if(userResponse.state.isFailure()) return Response.invalid()
        val identity = userResponse.checkForIdentity(token) ?: return Response.invalid()
        val savedSheet = port.saveSheet(userId, accountLabel, sheet) ?: return Response.invalid()
        return Response.ok(savedSheet)
    }

    fun addCategory(userId: UserId, token: Token, category: Category): Response<Category> {
        val userResponse = userPort.findById(userId)
        if(userResponse.state.isFailure()){
            return Response.invalid()
        }
        val userToken = userResponse.token ?: return Response.invalid()
        if(userToken.id != token.id) {
            return Response.timeout()
        }
        val categoryResponse = port.saveCategory(userId, category) ?: return Response.invalid()
        return Response.ok(categoryResponse)
    }

    fun getAccountByUser(userId: UserId, token: Token): List<Account>?{
        return port.getAccounts(userId)
    }

    fun retrieveAllCategoryOfUser(userId: Long, token: Token): Response<List<Category>> {
        val ticket = userPort.findById(UserId(userId))
        if(ticket.user == null || ticket.token == null) return Response.invalid()
        ticket.checkForIdentity(token) ?: return Response.invalid()
        if(ticket.state.isFailure()){
            return Response.timeout()
        }
        return Response.ok(ticket.user.categories())
    }

    fun removeCategory(id: UserId, token: Token, label: String): Response<Category?> {
        val userTicket = userPort.findById(id)
        if(userTicket.state.isFailure()) return Response.invalid()
        userTicket.checkForIdentity(token) ?: return Response.timeout()
        val responseEntity = port.removeCategory(id, label)
        return Response.ok(responseEntity)
    }
}
