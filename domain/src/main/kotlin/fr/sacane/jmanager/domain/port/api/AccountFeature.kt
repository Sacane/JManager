package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.DomainService
import fr.sacane.jmanager.domain.hexadoc.Port
import fr.sacane.jmanager.domain.hexadoc.Side
import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.port.spi.AccountRepositoryPort
import fr.sacane.jmanager.domain.port.spi.SessionManager
import fr.sacane.jmanager.domain.port.spi.UserRepository
import fr.sacane.jmanager.domain.utils.Result
import fr.sacane.jmanager.domain.utils.ResultState
import fr.sacane.jmanager.domain.utils.failure
import fr.sacane.jmanager.domain.utils.success

@Port(Side.APPLICATION)
sealed interface AccountFeature {
    fun findAccountById(accountID: Long, token: String): Result<Account>
    fun editAccount(account: Account, token: String): Result<Account>
    fun deleteAccountById(accountID: Long, token: String): Result<Nothing>
    fun findByLabelAndUserId(token: String, label: String): Result<Account>
    fun findAllRegisteredAccounts(token: String): Result<List<Account>>
    fun save(token: String, account: Account): Result<Account>
}

@DomainService
class AccountFeatureImpl(
    private val userRepository: UserRepository,
    private val session: SessionManager,
    private val accountRepository: AccountRepositoryPort
): AccountFeature {
    override fun findAccountById(
        accountID: Long,
        token: String
    ): Result<Account> = session.authenticate(token) {
        accountRepository.findAccountByIdWithTransactions(accountID)?.run {
            return@authenticate success(this)
        }?: return@authenticate failure(ResultState.BOOKLET_NOT_FOUND, "Le compte est introuvable")
    }

    override fun editAccount(
        account: Account,
        token: String
    ): Result<Account> = session.authenticate(token) {
        val accountID = account.id ?: return@authenticate failure(ResultState.BOOKLET_NOT_FOUND, "Le livret ${account.label} est introuvable en base")
        val oldAccount = accountRepository.findAccountByIdWithTransactions(accountID) ?: return@authenticate failure(ResultState.BOOKLET_NOT_FOUND, "Le livret ${account.id} est introuvable")
        if(oldAccount.id != account.id && oldAccount.label == account.label){
            return@authenticate failure(ResultState.BOOKLET_LABEL_EXIST, "Le libellé du livret existe déjà")
        }
        oldAccount.updateFrom(account)
        val registered = accountRepository.upsert(oldAccount)
        success(registered)
    }

    override fun deleteAccountById(
        accountID: Long,
        token: String
    ): Result<Nothing> = session.authenticate(token) {
        if(accountRepository.findAccountByIdWithTransactions(accountID) == null){
            return@authenticate failure(ResultState.NOT_FOUND, "Le livret $accountID n'existe pas")
        }
        accountRepository.deleteAccountById(accountID)
        success()
    }

    override fun findByLabelAndUserId(
        token: String,
        label: String
    ): Result<Account> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithAccounts(it)
            ?: return@authenticate failure(ResultState.USER_NOT_FOUND, "L'utilisateur recherché n'existe pas")
        success(
            user.accounts
            .find { acc -> acc.label == label }
            ?: return@authenticate failure(ResultState.BOOKLET_LABEL_NOT_EXIST, "Le compte $label n'est pas enregistré en base")
        )
    }

    override fun findAllRegisteredAccounts(
        token: String
    ): Result<List<Account>> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithAccounts(it)
            ?: return@authenticate failure(ResultState.BOOKLET_NOT_FOUND, "L'utilisateur n'existe pas en base")
        return@authenticate success(user.accounts)
    }

    override fun save(
        token: String,
        account: Account
    ): Result<Account> = session.authenticate(token) {
        val user = userRepository.findUserByIdWithAccounts(it)
            ?: return@authenticate failure(ResultState.USER_NOT_FOUND, "L'utilisateur n'existe pas en base")
        if(user.hasAccount(account.label)) {
            return@authenticate failure(ResultState.BOOKLET_LABEL_EXIST, "Le profil contient déjà un compte avec le label ${account.label}")
        }
        val accountSaved = accountRepository.save(it, account)
            ?: return@authenticate failure(ResultState.INFRASTRUCTURE_ERROR,"Erreur lors de la sauvegarde du compte")
        success(accountSaved)
    }
}