package fr.sacane.jmanager.domain.models

import java.time.LocalDateTime
import java.util.UUID


@JvmInline
value class UserId(val value: UUID?)

data class UserForAdmin(
    val user: User,
    val roles: Set<Role>,
    val createdDate: LocalDateTime,
    val email: String? = null
)

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
    val roles: Set<Role> = setOf(Role.USER),
    val creationDate: LocalDateTime = LocalDateTime.now()
) {

    fun withToken(token: String): UserToken = UserToken(MinimalUserRepresentation(id, username, email), token)
    fun hasAccount(labelAccount: String): Boolean = booklets.any { labelAccount == it.label }
    override fun toString(): String = "username: $username"
    fun addAccount(booklet: Booklet) {
        booklets.add(booklet)
        booklet.owner = this
    }

    fun toModelForAdmin(): UserForAdmin = UserForAdmin(this, roles, creationDate, email)
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