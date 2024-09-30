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
import fr.sacane.jmanager.domain.port.spi.AccountRepository
import fr.sacane.jmanager.domain.port.spi.UserRepository
import java.util.*

@Port(Side.APPLICATION)
sealed interface AccountFeature {
    fun findAccountById(userId: UserId, accountID: Long, token: UUID): Response<Account>
    fun editAccount(userID: Long, account: Account, token: UUID): Response<Account>
    fun deleteAccountById(profileID: UserId, accountID: Long, token: UUID): Response<Nothing>
    fun findByLabelAndUserId(userId: UserId, token: UUID, label: String): Response<Account>
    fun findAllRegisteredAccounts(userId: UserId, token: UUID): Response<List<Account>>
    fun save(userId: UserId, token: UUID, account: Account): Response<Account>
}

@DomainService
class AccountFeatureImpl(
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val accountRepository: AccountRepository
): AccountFeature {
    override fun findAccountById(
        userId: UserId,
        accountID: Long,
        token: UUID
    ): Response<Account> = session.authenticate(userId, token) {
        accountRepository.findAccountByIdWithTransactions(accountID)?.run {
            return@authenticate ok(this)
        }?: return@authenticate notFound("Le compte est introuvable")
    }


    override fun editAccount(
        userID: Long,
        account: Account,
        token: UUID
    ): Response<Account> = session.authenticate(UserId(userID), token) {
        val accountID = account.id ?: return@authenticate notFound("Le compte est introuvable en base")
        val oldAccount = accountRepository.findAccountByIdWithTransactions(accountID) ?: return@authenticate notFound()
        if(oldAccount.id != account.id && oldAccount.label == account.label){
            return@authenticate invalid("Le libellé du compte existe déjà")
        }
        oldAccount.updateFrom(account)
        println(oldAccount)
        val registered = accountRepository.upsert(oldAccount)
        ok(registered)
    }


    override fun deleteAccountById(
        profileID: UserId,
        accountID: Long,
        token: UUID
    ): Response<Nothing> = session.authenticate(profileID, token) {
        accountRepository.deleteAccountById(accountID)
        ok()
    }


    override fun findByLabelAndUserId(
        userId: UserId,
        token: UUID,
        label: String
    ): Response<Account> = session.authenticate(userId, token) {
        val user = userRepository.findUserByIdWithAccounts(userId) ?: return@authenticate notFound("L'utilisateur recherché n'existe pas")
        ok(
            user.accounts
            .find { acc -> acc.label == label }
            ?: return@authenticate notFound("Le compte $label n'est pas enregistré en base")
        )
    }


    override fun findAllRegisteredAccounts(
        userId: UserId,
        token: UUID
    ): Response<List<Account>> = session.authenticate(userId, token) {
        val user = userRepository.findUserByIdWithAccounts(userId) ?: return@authenticate notFound("L'utilisateur n'existe pas en base")
        return@authenticate ok(user.accounts)
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
        val accountSaved = accountRepository.save(userId, account) ?: return@authenticate notFound("Erreur lors de la sauvegarde du compte")
        ok(accountSaved)
    }
}