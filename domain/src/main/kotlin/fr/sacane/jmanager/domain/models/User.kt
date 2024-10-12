package fr.sacane.jmanager.domain.models

import fr.sacane.jmanager.domain.Hash


@JvmInline
value class UserId(val id: Long?)

data class MinimalUserRepresentation(
    val id: UserId = UserId(null),
    val username: String,
    val email: String? = null,
)

class User(
    val id: UserId = UserId(null),
    val username: String,
    val email: String?,
    val accounts: MutableList<Account> = mutableListOf(),
    val tags: MutableSet<Tag> = mutableSetOf()
) {

    fun withToken(token: AccessToken): UserToken = UserToken(MinimalUserRepresentation(id, username, email), token)
    fun hasAccount(labelAccount: String): Boolean = accounts.any { labelAccount == it.label }
    override fun toString(): String = "username: $username"

    fun removeAccount(accountID: Long) {
        accounts.removeIf { it.id == accountID }
    }
    fun addAccount(account: Account) {
        accounts.add(account)
    }
}

data class UserWithPassword(
    val user: User,
    val password: String,
)