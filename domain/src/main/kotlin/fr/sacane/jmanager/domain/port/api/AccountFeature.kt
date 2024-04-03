package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.Response
import fr.sacane.jmanager.domain.models.Response.Companion.invalid
import fr.sacane.jmanager.domain.models.Response.Companion.notFound
import fr.sacane.jmanager.domain.models.Response.Companion.ok
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.port.spi.TransactionRegister
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.*

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
    private val session: SessionManager
): AccountFeature {
    override fun findAccountById(
        userId: UserId,
        accountID: Long,
        token: UUID
    ): Response<Account> = session.authenticate(userId, token) {
        register.findAccountById(accountID)?.run {
            return@authenticate ok(this)
        }?: return@authenticate notFound("Le compte est introuvable")
    }


    override fun editAccount(
        userID: Long,
        account: Account,
        token: UUID
    ): Response<Account> = session.authenticate(UserId(userID), token) {
        val accountID = account.id ?: return@authenticate notFound("Le compte est introuvable en base")
        val oldAccount = register.findAccountById(accountID) ?: return@authenticate notFound()
        if(oldAccount.id != account.id && oldAccount.label == account.label){
            return@authenticate invalid("Le libellé du compte existe déjà")
        }
        oldAccount.updateFrom(account)
        val registered = register.persist(oldAccount) ?: return@authenticate invalid("Une erreur s'est produite lors de la mise à jour du compte")
        ok(registered)
    }


    override fun deleteAccountById(
        profileID: UserId,
        accountID: Long,
        token: UUID
    ): Response<Nothing> = session.authenticate(profileID, token) {
        val user = userRepository.findUserById(profileID) ?: return@authenticate notFound("L'utilisateur recherché n'exite pas")
        user.removeAccount(accountID)
        userRepository.upsert(user) ?: return@authenticate invalid("Une erreur s'est produite lors de l'insertion du compte")
        register.deleteAccountByID(accountID)
        ok()
    }


    override fun retrieveAccountByIdentityAndLabel(
        userId: UserId,
        token: UUID,
        label: String
    ): Response<Account> = session.authenticate(userId, token) {
        val user = userRepository.findUserById(userId) ?: return@authenticate notFound("L'utilisateur recherché n'existe pas")
        ok(
            user.accounts
            .find { acc -> acc.label == label }
            ?: return@authenticate notFound("Le compte $label n'est pas enregistré en base")
        )
    }


    override fun retrieveAllRegisteredAccounts(
        userId: UserId,
        token: UUID
    ): Response<List<Account>> = session.authenticate(userId, token) {
        val user = userRepository.findUserByIdWithAccounts(userId) ?: return@authenticate notFound("L'utilisateur n'existe pas en base")
        val entity = user.accounts
        user.accounts.forEach { println(it.label) }
        return@authenticate ok(entity)
    }

    override fun save(
        userId: UserId,
        token: UUID,
        account: Account
    ): Response<Account> = session.authenticate(userId, token) {
        val user = userRepository.findUserByIdWithAccounts(userId) ?: return@authenticate notFound("L'utilisateur n'existe pas en base")
        if(user.hasAccount(account.label)) {
            return@authenticate invalid("Le profil contient déjà un compte avec ce label")
        }
        val userSaved = register.persist(userId, account) ?: return@authenticate invalid("Impossible de créer un compte")
        ok(userSaved.accounts.find { it.label == account.label }
            ?: return@authenticate notFound("Une erreur s'est produite lors de la création du compte."))
    }
}