package fr.sacane.jmanager.domain.models


@JvmInline
value class UserId(val value: Long?)

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

    fun withToken(token: String): UserToken = UserToken(MinimalUserRepresentation(id, username, email), token)
    fun hasAccount(labelAccount: String): Boolean = accounts.any { labelAccount == it.label }
    override fun toString(): String = "username: $username"

    fun removeAccount(accountID: Long) {
        accounts.removeIf { it.id == accountID }
    }
    fun addAccount(account: Account) {
        accounts.add(account)
        account.owner = this
    }
}

data class UserWithPassword(
    val user: User,
    val password: String,
)