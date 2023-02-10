package fr.sacane.jmanager.domain.port.api

import fr.sacane.jmanager.domain.hexadoc.LeftPort
import fr.sacane.jmanager.domain.models.*
import fr.sacane.jmanager.domain.port.spi.LoginInventory
import fr.sacane.jmanager.domain.port.spi.UserTransaction

@LeftPort
class LoginManager(private val loginPort: LoginInventory, private val userPort: UserTransaction): Administrator{


    override fun login(pseudonym: String, userPassword: Password): Response<Ticket> {
        val ticket = loginPort.login(pseudonym, userPassword) ?: return Response.invalid()
        return Response.ok(ticket)
    }

    override fun logout(userId: UserId, userToken: Token): Response<Nothing> {
        val ticket = loginPort.logout(userId, userToken) ?: return Response.invalid()
        return Response.ok()
    }

    override fun signIn(user: User): Response<User> {
        val userResponse = userPort.save(user) ?: return Response.invalid()
        return Response.ok(userResponse)
    }

}