package fr.sacane.jmanager.domain.fixture

import fr.sacane.jmanager.domain.models.User
import fr.sacane.jmanager.domain.models.UserId
import fr.sacane.jmanager.domain.models.UserWithPassword
import fr.sacane.jmanager.domain.models.Role
import java.util.UUID

object UserFixture {

    fun aUser(
        id: UserId = UserId(UUID.randomUUID()),
        username: String = "john",
        email: String? = ""
    ) = User(id, username, email)

    fun aUserWithPassword(
        user: User = aUser(),
        password: String = "test-password",
        roles: Set<Role> = setOf(Role.USER)
    ) = UserWithPassword(user, password, roles)
}
