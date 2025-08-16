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
    val booklets: MutableList<Booklet> = mutableListOf(),
    val tags: MutableSet<Tag> = mutableSetOf()
) {

    fun withToken(token: String): UserToken = UserToken(MinimalUserRepresentation(id, username, email), token)
    fun hasAccount(labelAccount: String): Boolean = booklets.any { labelAccount == it.label }
    override fun toString(): String = "username: $username"

    fun removeAccount(accountID: Long) {
        booklets.removeIf { it.id == accountID }
    }
    fun addAccount(booklet: Booklet) {
        booklets.add(booklet)
        booklet.owner = this
    }
}

data class UserWithPassword(
    val user: User,
    val password: String,
)