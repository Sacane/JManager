package fr.sacane.jmanager.domain.port.spi

import fr.sacane.jmanager.domain.models.Account
import fr.sacane.jmanager.domain.models.UserId

interface AccountRepository {
    fun editFromAnother(account: Account): Account?
    fun getLastSheetPosition(accountId: Long): Int?
    fun save(ownerId: UserId, account: Account): Account?
    fun findAccountByIdWithTransactions(accountId: Long): Account?
    fun findAccountByLabelWithTransactions(userId: UserId, accountLabel: String): Account?
    fun deleteAccountById(accountId: Long)
    fun upsert(account: Account): Account
}