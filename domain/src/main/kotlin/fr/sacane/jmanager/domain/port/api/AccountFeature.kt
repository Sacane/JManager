package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainImplementation
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Token
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserTransaction

@Port(Side.API)
sealed interface AccountFeature {
    fun findAccountById(userId: UserId, accountID: Long, token: Token): Response<Account>
    fun editAccount(userID: Long, account: Account, token: Token): Response<Account>
    fun deleteAccountById(profileID: UserId, accountID: Long): Response<Nothing>
    fun retrieveAccountByIdentityAndLabel(userId: UserId, token: Token, label: String): Response<Account>
    fun retrieveAllRegisteredAccounts(userId: UserId, token: Token): Response<List<Account>>
    fun save(userId: UserId, token: Token, account: Account): Response<Account>
}

@DomainImplementation
class AccountFeatureImpl(
    private val register: TransactionRegister,
    private val userTransaction: UserTransaction
): AccountFeature {
    override fun findAccountById(userId: UserId, accountID: Long, token: Token): Response<Account> {
        userTransaction.findById(userId)?.checkForIdentity(token) ?: return Response.notFound("L'utlisateur est inconnue")
        return register.findAccountById(accountID).run {
            this ?: return Response.notFound("Le compte n'existe pas")
            Response.ok(this)
        }
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

    override fun deleteAccountById(profileID: UserId, accountID: Long): Response<Nothing> {
        val profile = userTransaction.findUserById(profileID) ?: return Response.notFound()
        profile.accounts.removeIf { it.id == accountID }
        userTransaction.upsert(profile)!!
        register.deleteAccountByID(accountID)
        return Response.ok()
    }

    override fun retrieveAccountByIdentityAndLabel(userId: UserId, token: Token, label: String): Response<Account> {
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid()
        userResponse.checkForIdentity(token) ?: return Response.invalid()
        val targetOne = userResponse.user.accounts().find { it.label == label } ?: return Response.notFound()
        return Response.ok(targetOne)
    }

    override fun retrieveAllRegisteredAccounts(userId: UserId, token: Token): Response<List<Account>> {
        println("User id : $userId")
        val userResponse = userTransaction.findById(userId) ?: return Response.invalid(TransactionRegister.missingUserMessage)
        userResponse.checkForIdentity(token) ?: return Response.invalid(TransactionRegister.timeoutMessage)
        return Response.ok(userResponse.user.accounts())
    }

    override fun save(userId: UserId, token: Token, account: Account): Response<Account> {
        val tokenResponse = userTransaction.getUserToken(userId) ?: return Response.invalid(TransactionRegister.missingUserMessage)
        if(tokenResponse.id != token.id) {
            return Response.timeout(TransactionRegister.timeoutMessage)
        }
        val ticket = userTransaction.findById(userId)
        if(ticket?.user?.accounts?.any { account.label == it.label } ?: return Response.notFound(TransactionRegister.missingUserMessage)) {
            return Response.invalid("Le profil contient déjà un compte avec ce label")
        }
        val userSaved = register.persist(userId, account) ?: return Response.invalid("Impossible de créer un compte")
        return Response.ok(userSaved.accounts().find { it.label == account.label }
            ?: return Response.notFound("Le compte créé n'a pas été sauvegardé correctement"))
    }

}