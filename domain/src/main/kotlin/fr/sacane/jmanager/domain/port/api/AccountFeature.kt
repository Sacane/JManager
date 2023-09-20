package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainImplementation
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Response.Companion.invalid
import fr.sacane.jmanager.domain.models.Response.Companion.notFound
import fr.sacane.jmanager.domain.models.Response.Companion.ok
import fr.sacane.jmanager.domain.models.Token
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.LoginRegisterManager
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserTransaction

@Port(Side.API)
sealed interface AccountFeature {
    fun findAccountById(userId: UserId, accountID: Long, token: Token): Response<Account>
    fun editAccount(userID: Long, account: Account, token: Token): Response<Account>
    fun deleteAccountById(profileID: UserId, accountID: Long, token: Token): Response<Nothing>
    fun retrieveAccountByIdentityAndLabel(userId: UserId, token: Token, label: String): Response<Account>
    fun retrieveAllRegisteredAccounts(userId: UserId, token: Token): Response<List<Account>>
    fun save(userId: UserId, token: Token, account: Account): Response<Account>
}

@DomainImplementation
class AccountFeatureImpl(
    private val register: TransactionRegister,
    private val userTransaction: UserTransaction,
    private val loginManager: LoginRegisterManager
): AccountFeature {
    override fun findAccountById(
        userId: UserId,
        accountID: Long,
        token: Token
    ): Response<Account> = loginManager.authenticate(userId, token) {
        register.findAccountById(accountID)?.run {
            return@authenticate ok(this)
        }?: return@authenticate notFound("Le compte est introuvable")
    }


    override fun editAccount(
        userID: Long,
        account: Account,
        token: Token
    ): Response<Account> = loginManager.authenticate(UserId(userID), token) {
        val accountID = account.id ?: return@authenticate notFound("L'id du compte n'est pas valide")
        val oldAccount = register.findAccountById(accountID) ?: return@authenticate notFound()
        if(oldAccount.label == account.label){
            return@authenticate invalid("Le libellé du compte existe déjà")
        }
        oldAccount.updateFrom(account)
        val registered = register.persist(oldAccount) ?: return@authenticate invalid()
        Response.ok(registered)
    }


    override fun deleteAccountById(profileID: UserId, accountID: Long, token: Token): Response<Nothing> {
        return loginManager.authenticate(profileID, token) {
            this.accounts.removeIf { acc -> acc.id == accountID }
            userTransaction.upsert(this) ?: return@authenticate invalid("Une erreur s'est produite lors de l'insertion du compte")
            register.deleteAccountByID(accountID)
            Response.ok()
        }
    }

    override fun retrieveAccountByIdentityAndLabel(
        userId: UserId,
        token: Token,
        label: String
    ): Response<Account> = loginManager.authenticate(userId, token) {
        Response.ok(
            this.accounts()
            .find { acc -> acc.label == label }
            ?: return@authenticate notFound("Le compte $label n'est pas enregistré en base")
        )
    }


    override fun retrieveAllRegisteredAccounts(
        userId: UserId,
        token: Token
    ): Response<List<Account>> = loginManager.authenticate(userId, token) {
        return@authenticate Response.ok(this.accounts())
    }

    override fun save(
        userId: UserId,
        token: Token,
        account: Account
    ): Response<Account> = loginManager.authenticate(userId, token) {
        val ticket = userTransaction.findById(userId)
        if(ticket?.user?.accounts?.any { account.label == it.label } ?: return@authenticate notFound(TransactionRegister.missingUserMessage)) {
            return@authenticate invalid("Le profil contient déjà un compte avec ce label")
        }
        val userSaved = register.persist(userId, account) ?: return@authenticate invalid("Impossible de créer un compte")
        Response.ok(userSaved.accounts().find { it.label == account.label }
            ?: return@authenticate notFound("Le compte créé n'a pas été sauvegardé correctement"))
    }



}