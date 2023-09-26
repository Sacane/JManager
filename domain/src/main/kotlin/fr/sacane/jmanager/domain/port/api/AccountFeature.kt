package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Response.Companion.invalid
import fr.sacane.jmanager.domain.models.Response.Companion.notFound
import fr.sacane.jmanager.domain.models.Response.Companion.ok
import fr.sacane.jmanager.domain.models.AccessToken
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.Session
import fr.sacane.jmanager.domain.port.spi.SessionRepository
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.UUID

@Port(Side.API)
sealed interface AccountFeature {
    fun findAccountById(userId: UserId, accountID: Long, token: UUID): Response<Account>
    fun editAccount(userID: Long, account: Account, token: UUID): Response<Account>
    fun deleteAccountById(profileID: UserId, accountID: Long, token: UUID): Response<Nothing>
    fun retrieveAccountByIdentityAndLabel(userId: UserId, token: UUID, label: String): Response<Account>
    fun retrieveAllRegisteredAccounts(userId: UserId, token: UUID): Response<List<Account>>
    fun save(userId: UserId, token: UUID, account: Account): Response<Account>
}

@DomainService
class AccountFeatureImpl(
    private val register: TransactionRegister,
    private val userRepository: UserRepository,
    private val loginManager: SessionRepository
): AccountFeature {
    override fun findAccountById(
        userId: UserId,
        accountID: Long,
        token: UUID
    ): Response<Account> = Session.authenticate(userId, token) {
        register.findAccountById(accountID)?.run {
            return@authenticate ok(this)
        }?: return@authenticate notFound("Le compte est introuvable")
    }


    override fun editAccount(
        userID: Long,
        account: Account,
        token: UUID
    ): Response<Account> = Session.authenticate(UserId(userID), token) {
        val accountID = account.id ?: return@authenticate notFound("L'id du compte n'est pas valide")
        val oldAccount = register.findAccountById(accountID) ?: return@authenticate notFound()
        if(oldAccount.id != account.id && oldAccount.label == account.label){
            return@authenticate invalid("Le libellé du compte existe déjà")
        }
        oldAccount.updateFrom(account)
        val registered = register.persist(oldAccount) ?: return@authenticate invalid()
        ok(registered)
    }


    override fun deleteAccountById
                (profileID: UserId,
                 accountID: Long,
                 token: UUID
    ): Response<Nothing> = Session.authenticate(profileID, token) {
        this.accounts.removeIf { acc -> acc.id == accountID }
        userRepository.upsert(this) ?: return@authenticate invalid("Une erreur s'est produite lors de l'insertion du compte")
        register.deleteAccountByID(accountID)
        ok()
    }


    override fun retrieveAccountByIdentityAndLabel(
        userId: UserId,
        token: UUID,
        label: String
    ): Response<Account> = Session.authenticate(userId, token) {
        ok(
            this.accounts()
            .find { acc -> acc.label == label }
            ?: return@authenticate notFound("Le compte $label n'est pas enregistré en base")
        )
    }


    override fun retrieveAllRegisteredAccounts(
        userId: UserId,
        token: UUID
    ): Response<List<Account>> = Session.authenticate(userId, token) {
        println("HELLO")
        return@authenticate ok(this.accounts())
    }

    override fun save(
        userId: UserId,
        token: UUID,
        account: Account
    ): Response<Account> = Session.authenticate(userId, token) {
        if(this.accounts.any { account.label == it.label }) {
            return@authenticate invalid("Le profil contient déjà un compte avec ce label")
        }
        val userSaved = register.persist(userId, account) ?: return@authenticate invalid("Impossible de créer un compte")
        ok(userSaved.accounts().find { it.label == account.label }
            ?: return@authenticate notFound("Une erreur s'est produite lors de la création du compte."))
    }



}