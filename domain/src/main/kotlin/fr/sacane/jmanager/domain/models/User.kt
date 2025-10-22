package fr.sacane.jmanager.domain.models

import java.util.UUID


@JvmInline
value class UserId(val value: UUID?)

data class MinimalUserRepresentation(
    val id: UserId = UserId(null),
    val username: String,
    val email: String? = null,
) {

    fun toUser(): User = User(id = id, username = username, email = email)

}

class User(
    val id: UserId = UserId(null),
    val username: String,
    val email: String?,
    val booklets: MutableList<Booklet> = mutableListOf(),
    val tags: MutableSet<Tag> = mutableSetOf(),
    val roles: Set<Role> = setOf(Role.USER)
) {

    fun withToken(token: String): UserToken = UserToken(MinimalUserRepresentation(id, username, email), token)
    fun hasAccount(labelAccount: String): Boolean = booklets.any { labelAccount == it.label }
    override fun toString(): String = "username: $username"
    fun addAccount(booklet: Booklet) {
        booklets.add(booklet)
        booklet.owner = this
    }
}

data class UserWithPassword(
    val user: User,
    val password: String,
    val roles: Set<Role> = setOf(Role.USER)
)

enum class Role(val weight: Int) {
    USER(1),
    ADMIN(2)
}

fun Collection<Role>.weight(): Int = this.sumOf { it.weight }