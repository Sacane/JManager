package fr.sacane.jmanager.domain.fixture

import fr.sacane.jmanager.domain.models.ConsentRecord
import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.Role
import java.time.LocalDateTime
import java.util.UUID

object UserFixture {

    fun aUser(
        id: UserId = UserId(UUID.randomUUID()),
        username: String = "john",
        email: String? = "",
        emailVerified: Boolean = false,
    ) = User(id = id, username = username, email = email, emailVerified = emailVerified)

    fun aUserWithPassword(
        user: User = aUser(),
        password: String = "test-password",
        roles: Set<Role> = setOf(Role.USER)
    ) = UserWithPassword(user, password, roles)

    fun aUserWithConsent(
        id: UserId = UserId(UUID.randomUUID()),
        username: String = "john",
        email: String? = "",
        consentTimestamp: LocalDateTime = LocalDateTime.of(2026, 1, 1, 10, 0),
        tosVersion: String = "1.0",
    ) = User(
        id = id,
        username = username,
        email = email,
        consent = ConsentRecord(
            tosAcceptedAt = consentTimestamp,
            tosVersion = tosVersion,
            privacyAcceptedAt = consentTimestamp,
        )
    )
}
