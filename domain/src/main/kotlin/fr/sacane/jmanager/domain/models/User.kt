package fr.sacane.jmanager.domain.models

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class ConsentRecord(
    val tosAcceptedAt: LocalDateTime,
    val tosVersion: String?,
    val privacyAcceptedAt: LocalDateTime,
)


@JvmInline
value class UserId(val value: UUID?)

data class UserForAdmin(
    val user: User,
    val createdDate: LocalDateTime,
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
    val creationDate: LocalDateTime = LocalDateTime.now(),
    val isEnabled: Boolean = true,
    var projectionWindowDays: Int = 15,
    val subscriptionPlan: SubscriptionPlan = SubscriptionPlan.BETA_TESTER,
    var consent: ConsentRecord? = null,
    var emailVerified: Boolean = false,
    var mustChangePassword: Boolean = false,
    var credentialsChangedAt: LocalDateTime? = null,
) {

    init {
        require(projectionWindowDays in 7..60) {
            "projectionWindowDays must be between 7 and 60"
        }
    }

    fun withToken(token: String, refreshToken: UUID? = null): UserToken = UserToken(
        MinimalUserRepresentation(id, username, email),
        token,
        refreshToken,
    )
    /**
     * Whether an access token issued at [issuedAt] still stands after the last credential change.
     *
     * Token issue times have second precision, so the change time is truncated to the second: the
     * session re-issued by the change itself, in the same second, is accepted. A token with no issue
     * time predates this rule and is refused once credentials have changed.
     */
    fun acceptsTokenIssuedAt(issuedAt: LocalDateTime?): Boolean {
        val changedAt = credentialsChangedAt ?: return true
        return issuedAt != null && !issuedAt.isBefore(changedAt.truncatedTo(ChronoUnit.SECONDS))
    }

    fun hasBooklet(label: String): Boolean = booklets.any { label == it.label }
    override fun toString(): String = "username: $username"

    fun updateProjectionWindowDays(days: Int) {
        require(days in 7..60) {
            "projectionWindowDays must be between 7 and 60"
        }
        projectionWindowDays = days
    }

    fun addBooklet(booklet: Booklet) {
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